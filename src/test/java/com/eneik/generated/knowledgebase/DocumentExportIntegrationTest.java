package com.eneik.generated.knowledgebase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DocumentExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbDocumentVersionRepository versionRepository;

    @Autowired
    private KbUserRepository userRepository;

    @Autowired
    private KbAuditLogRepository auditLogRepository;

    @BeforeEach
    public void setup() {
        auditLogRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testDocumentExportToPdfAndDocx() throws Exception {
        // 1. Upload a document with title, category, tags, and some cyrillic text content
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pediatrics_guide.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Важные клинические рекомендации по педиатрии.".getBytes(StandardCharsets.UTF_8)
        );

        String responseStr = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Методическое пособие по Педиатрии")
                        .param("category", "Педиатрия")
                        .param("tags", "клиника", "ординатура")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        // Extract ID
        Long docId = Long.parseLong(responseStr.split("\"id\":")[1].split(",")[0].trim());

        // 2. Export to PDF
        MvcResult pdfResult = mockMvc.perform(get("/api/v1/integration/documents/{id}/export", docId)
                        .param("format", "pdf")
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("export_" + docId + ".pdf")))
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 0);
        // A valid PDF always starts with %PDF-
        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 10), StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"));

        // 3. Export to DOCX
        MvcResult docxResult = mockMvc.perform(get("/api/v1/integration/documents/{id}/export", docId)
                        .param("format", "docx")
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("export_" + docId + ".docx")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andReturn();

        byte[] docxBytes = docxResult.getResponse().getContentAsByteArray();
        assertTrue(docxBytes.length > 0);

        // Verify it is a valid zip and contains the expected entries and content
        boolean foundDocumentXml = false;
        String documentXmlContent = "";
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    foundDocumentXml = true;
                    byte[] entryBytes = zis.readAllBytes();
                    documentXmlContent = new String(entryBytes, StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        }

        assertTrue(foundDocumentXml, "Should find word/document.xml in the DOCX archive");
        assertTrue(documentXmlContent.contains("Методическое пособие по Педиатрии"), "Should contain the title");
        assertTrue(documentXmlContent.contains("Category: Педиатрия"), "Should contain the category");
        assertTrue(documentXmlContent.contains("Tags: "), "Should contain the Tags label");
        assertTrue(documentXmlContent.contains("клиника"), "Should contain tag 'клиника'");
        assertTrue(documentXmlContent.contains("ординатура"), "Should contain tag 'ординатура'");
        assertTrue(documentXmlContent.contains("Важные клинические рекомендации по педиатрии."), "Should contain the document content");

        // 4. Verify audit log was written for the EXPORT action
        mockMvc.perform(get("/api/v1/integration/analytics/statistics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        boolean hasExportLog = auditLogRepository.findAll().stream()
                .anyMatch(log -> "EXPORT".equalsIgnoreCase(log.getAction()) && docId.equals(log.getTargetId()));
        assertTrue(hasExportLog, "Should have created audit log entries for export actions");
    }

    @Test
    public void testExportNotFoundAndUnsupportedFormat() throws Exception {
        // Export non-existent document
        mockMvc.perform(get("/api/v1/integration/documents/99999/export")
                        .param("format", "pdf"))
                .andExpect(status().isNotFound());

        // Upload a document first
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Test content".getBytes(StandardCharsets.UTF_8)
        );

        String responseStr = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Test Title")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long docId = Long.parseLong(responseStr.split("\"id\":")[1].split(",")[0].trim());

        // Try exporting with unsupported format
        mockMvc.perform(get("/api/v1/integration/documents/{id}/export", docId)
                        .param("format", "txt"))
                .andExpect(status().isBadRequest());
    }
}
