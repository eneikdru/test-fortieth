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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserPreferencesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbDocumentVersionRepository versionRepository;

    @Autowired
    private KbUserRepository userRepository;

    @Autowired
    private KbFavoriteRepository favoriteRepository;

    @Autowired
    private KbSavedQueryRepository savedQueryRepository;

    @Autowired
    private KbAuditLogRepository auditLogRepository;

    private Long documentId1;
    private Long documentId2;

    @BeforeEach
    public void setup() throws Exception {
        // Clean database in order to prevent foreign key violations
        auditLogRepository.deleteAll();
        favoriteRepository.deleteAll();
        savedQueryRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // Upload some documents using Admin user context
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "pediatrics_guide.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Pediatrics guide content".getBytes(StandardCharsets.UTF_8)
        );

        String docResp1 = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file1)
                        .param("title", "Clinical Residency in Pediatrics")
                        .param("category", "Pediatrics")
                        .header("X-User-Name", "admin_user")
                        .header("X-User-Role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        documentId1 = Long.parseLong(docResp1.split("\"id\":")[1].split(",")[0].trim());

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "anatomy_guide.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Anatomy content".getBytes(StandardCharsets.UTF_8)
        );

        String docResp2 = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file2)
                        .param("title", "Human Anatomy Guide")
                        .param("category", "Anatomy")
                        .header("X-User-Name", "admin_user")
                        .header("X-User-Role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        documentId2 = Long.parseLong(docResp2.split("\"id\":")[1].split(",")[0].trim());
    }

    @Test
    public void testAddAndRetrieveFavoritesFlow() throws Exception {
        // Add document 1 to favorites of Alice
        mockMvc.perform(post("/api/v1/integration/documents/favorites")
                        .param("documentId", String.valueOf(documentId1))
                        .header("X-User-Name", "alice")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isCreated());

        // Get favorites for Alice in a "new session" (by supplying the same auth context)
        mockMvc.perform(get("/api/v1/integration/documents/favorites")
                        .header("X-User-Name", "alice")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(documentId1.intValue())))
                .andExpect(jsonPath("$[0].title", is("Clinical Residency in Pediatrics")));

        // Verify that Bob's favorites are completely empty (strict user isolation / indexical context lock)
        mockMvc.perform(get("/api/v1/integration/documents/favorites")
                        .header("X-User-Name", "bob")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Remove document from favorites for Alice
        mockMvc.perform(delete("/api/v1/integration/documents/favorites/{documentId}", documentId1)
                        .header("X-User-Name", "alice")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNoContent());

        // Alice's favorites should now be empty
        mockMvc.perform(get("/api/v1/integration/documents/favorites")
                        .header("X-User-Name", "alice")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testSaveAndRetrieveSearchQueriesFlow() throws Exception {
        // Save search query for Alice
        mockMvc.perform(post("/api/v1/integration/documents/saved-queries")
                        .param("query", "Pediatrics")
                        .header("X-User-Name", "alice")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queryText", is("Pediatrics")));

        // Get saved queries for Alice in a new session (by supplying the same auth context)
        String queriesResp = mockMvc.perform(get("/api/v1/integration/documents/saved-queries")
                        .header("X-User-Name", "alice")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].queryText", is("Pediatrics")))
                .andReturn().getResponse().getContentAsString();

        long savedQueryId = Long.parseLong(queriesResp.split("\"id\":")[1].split(",")[0].trim());

        // Verify Bob's saved queries are empty (strict user isolation / indexical context lock)
        mockMvc.perform(get("/api/v1/integration/documents/saved-queries")
                        .header("X-User-Name", "bob")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Verify Bob cannot delete Alice's saved query (403 Forbidden)
        mockMvc.perform(delete("/api/v1/integration/documents/saved-queries/{id}", savedQueryId)
                        .header("X-User-Name", "bob")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());

        // Delete saved query as Alice
        mockMvc.perform(delete("/api/v1/integration/documents/saved-queries/{id}", savedQueryId)
                        .header("X-User-Name", "alice")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNoContent());

        // Alice's saved queries should now be empty
        mockMvc.perform(get("/api/v1/integration/documents/saved-queries")
                        .header("X-User-Name", "alice")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testAddFavoriteReturns404IfDocumentNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/integration/documents/favorites")
                        .param("documentId", "9999")
                        .header("X-User-Name", "alice")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNotFound());
    }
}
