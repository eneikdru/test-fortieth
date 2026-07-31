package com.eneik.generated.knowledgebase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DocumentSearchAndContentTest {

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

    private String adminToken;

    private String getJwtToken(String username, String role) throws Exception {
        String loginJson = "{\"username\":\"" + username + "\", \"password\":\"password\", \"role\":\"" + role + "\"}";
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + loginResponse.split("\"token\":\"")[1].split("\"")[0];
    }

    @BeforeEach
    public void setup() throws Exception {
        // Clear database in correct order of constraints
        auditLogRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // Get admin token
        adminToken = getJwtToken("admin_user", "ADMINISTRATOR");
    }

    @Test
    public void testDocumentWorkflowAndSearchWithSynonyms() throws Exception {
        // 1. Upload a document with "ФГОС" in the title and "образовательный стандарт" synonyms
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "fgos_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This file contains the key educational regulations for Epidemiology specialization.".getBytes(StandardCharsets.UTF_8)
        );

        String docIdStr = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file1)
                        .param("title", "Регламент ФГОС ЦНИИ")
                        .param("category", "Нормативные акты")
                        .param("tags", "ординатура", "ФГОС")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title").value("Регламент ФГОС ЦНИИ"))
                .andExpect(jsonPath("$.category").value("Нормативные акты"))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("ординатура", "ФГОС")))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.fileType").value("txt"))
                .andReturn().getResponse().getContentAsString();

        // Extract ID from JSON
        Long docId = Long.parseLong(docIdStr.split("\"id\":")[1].split(",")[0].trim());

        // 2. Upload another document with "ЦНИИ Эпидемиологии"
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "epidemiology_instructions.pdf",
                "application/pdf",
                "Detailed instructions for clinical residency research guidelines.".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file2)
                        .param("title", "Инструкция ЦНИИ Эпидемиологии")
                        .param("category", "Методические материалы")
                        .param("tags", "исследования", "ординатура")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Инструкция ЦНИИ Эпидемиологии"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.fileType").value("pdf"));

        // 3. Search with synonyms (Query: "федеральный государственный образовательный стандарт" should match "ФГОС" doc)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "федеральный государственный образовательный стандарт")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Регламент ФГОС ЦНИИ"));

        // 4. Search with query "ФБУН" (synonym of "ЦНИИ Эпидемиологии" and "Федеральное бюджетное учреждение науки")
        // It should match BOTH documents because:
        // - doc1 has "ЦНИИ" in title
        // - doc2 has "ЦНИИ Эпидемиологии" in title
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "ФБУН")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 5. Check specialty filter ("ординатура" matches both)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "ординатура")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 6. Check specialty filter for "исследования" (matches only second doc)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "исследования")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Инструкция ЦНИИ Эпидемиологии"));

        // 7. Check documentType filter (pdf)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("documentType", "pdf")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Инструкция ЦНИИ Эпидемиологии"));

        // 8. Check updatedAfter filter (future date should yield 0 results)
        String futureIso = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_DATE_TIME);
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("updatedAfter", futureIso)
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 9. Get document details by ID and check VIEW audit log triggers
        mockMvc.perform(get("/api/v1/integration/documents/{id}", docId)
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Регламент ФГОС ЦНИИ"));

        // 10. Update document metadata and append new file version (PUT request with multipart)
        MockMultipartFile updatedFile = new MockMultipartFile(
                "file",
                "fgos_v2.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Updated standard regulations version two.".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents/{id}", docId)
                        .file(updatedFile)
                        .param("title", "Регламент ФГОС ЦНИИ Обновленный")
                        .param("category", "Нормативные акты")
                        .header("Authorization", adminToken)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Регламент ФГОС ЦНИИ Обновленный"))
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.fileType").value("docx"));

        // 11. Verify file download
        mockMvc.perform(get("/api/v1/integration/documents/download/{id}/version/2", docId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Updated standard regulations version two."));

        // 12. Check analytics statistics
        mockMvc.perform(get("/api/v1/integration/analytics/statistics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popularSearches", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.popularSearches[0].query", anyOf(is("федеральный государственный образовательный стандарт"), is("ФБУН"))))
                .andExpect(jsonPath("$.topViewedDocuments", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.topViewedDocuments[0].title", containsString("Регламент ФГОС")))
                .andExpect(jsonPath("$.topDownloadedDocuments", hasSize(greaterThanOrEqualTo(1))));

        // 13. Delete document
        mockMvc.perform(delete("/api/v1/integration/documents/{id}", docId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // Verify delete is 404 now
        mockMvc.perform(get("/api/v1/integration/documents/{id}", docId)
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
