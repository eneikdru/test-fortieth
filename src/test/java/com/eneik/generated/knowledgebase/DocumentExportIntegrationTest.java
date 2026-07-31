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

import java.nio.charset.StandardCharsets;

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
    public void testDynamicPdfAndDocxExport() throws Exception {
        // 1. Upload a mock document
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is the core content of the document to be exported dynamically.".getBytes(StandardCharsets.UTF_8)
        );

        String responseStr = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Export Test Document")
                        .param("category", "Test Category")
                        .param("tags", "test", "export")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title").value("Export Test Document"))
                .andReturn().getResponse().getContentAsString();

        Long docId = Long.parseLong(responseStr.split("\"id\":")[1].split(",")[0].trim());

        // 2. Test PDF Export on /export endpoint
        MvcResult pdfResult = mockMvc.perform(get("/api/v1/integration/documents/{id}/export", docId)
                        .param("format", "pdf")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(header().string("Content-Disposition", containsString("export_" + docId + ".pdf")))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 0);
        String pdfStr = new String(pdfBytes, StandardCharsets.UTF_8);
        assertTrue(pdfStr.contains("%PDF-1.4"));
        assertTrue(pdfStr.contains("Document Title: Export Test Document"));
        assertTrue(pdfStr.contains("Category: Test Category"));
        assertTrue(pdfStr.contains("Content:"));

        // 3. Test DOCX Export on /export endpoint
        MvcResult docxResult = mockMvc.perform(get("/api/v1/integration/documents/{id}/export", docId)
                        .param("format", "docx")
                        .accept(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document").toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .andExpect(header().string("Content-Disposition", containsString("export_" + docId + ".docx")))
                .andReturn();

        byte[] docxBytes = docxResult.getResponse().getContentAsByteArray();
        assertTrue(docxBytes.length > 0);
        // Verify zip magic signature (PK..)
        assertEquals((byte) 0x50, docxBytes[0]);
        assertEquals((byte) 0x4B, docxBytes[1]);

        // 4. Test PDF Export on /download/{id}/version/{versionNumber} endpoint
        mockMvc.perform(get("/api/v1/integration/documents/download/{id}/version/1", docId)
                        .param("format", "pdf")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(header().string("Content-Disposition", containsString("export_" + docId + ".pdf")));

        // 5. Test DOCX Export on /download/{id}/version/{versionNumber} endpoint
        mockMvc.perform(get("/api/v1/integration/documents/download/{id}/version/1", docId)
                        .param("format", "docx")
                        .accept(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document").toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .andExpect(header().string("Content-Disposition", containsString("export_" + docId + ".docx")));
    }

    @Test
    public void testDynamicPdfAndDocxExportWithNullMetadata() throws Exception {
        // 1. Upload a mock document with missing optional parameters (no category, no tags)
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sparse_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Sparse document content.".getBytes(StandardCharsets.UTF_8)
        );

        String responseStr = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Sparse Export Doc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title").value("Sparse Export Doc"))
                .andExpect(jsonPath("$.category").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        Long docId = Long.parseLong(responseStr.split("\"id\":")[1].split(",")[0].trim());

        // 2. Export to PDF and verify the null category/tags are rendered as "-" gracefully
        MvcResult pdfResult = mockMvc.perform(get("/api/v1/integration/documents/{id}/export", docId)
                        .param("format", "pdf")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is(MediaType.APPLICATION_PDF_VALUE)))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 0);
        String pdfStr = new String(pdfBytes, StandardCharsets.UTF_8);
        assertTrue(pdfStr.contains("Document Title: Sparse Export Doc"));
        assertTrue(pdfStr.contains("Category: -"));
        assertTrue(pdfStr.contains("Tags: -"));
    }
}
