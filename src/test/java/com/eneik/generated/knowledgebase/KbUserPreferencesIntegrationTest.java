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
public class KbUserPreferencesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbDocumentVersionRepository versionRepository;

    @Autowired
    private KbUserRepository userRepository;

    @Autowired
    private KbUserFavoriteRepository userFavoriteRepository;

    @Autowired
    private KbSavedQueryRepository savedQueryRepository;

    private Long documentId1;
    private Long documentId2;

    @BeforeEach
    public void setup() throws Exception {
        userFavoriteRepository.deleteAll();
        savedQueryRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // Create an admin user to upload documents
        KbUser admin = new KbUser();
        admin.setUsername("admin_uploader");
        admin.setRole("ADMINISTRATOR");
        admin = userRepository.save(admin);

        // Upload some documents to use in favorites tests
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "pediatrics.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Pediatrics content".getBytes(StandardCharsets.UTF_8)
        );

        String response1 = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file1)
                        .param("title", "Pediatrics Guidelines")
                        .param("category", "Pediatrics")
                        .header("X-User-Name", "admin_uploader")
                        .header("X-User-Role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        documentId1 = Long.parseLong(response1.split("\"id\":")[1].split(",")[0].trim());

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "cardiology.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Cardiology content".getBytes(StandardCharsets.UTF_8)
        );

        String response2 = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file2)
                        .param("title", "Cardiology Guidelines")
                        .param("category", "Cardiology")
                        .header("X-User-Name", "admin_uploader")
                        .header("X-User-Role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        documentId2 = Long.parseLong(response2.split("\"id\":")[1].split(",")[0].trim());
    }

    @Test
    public void testFavoritesWorkflows() throws Exception {
        // 1. Add favorite for user_alpha
        mockMvc.perform(post("/api/v1/integration/preferences/favorites")
                        .param("documentId", documentId1.toString())
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId", is(documentId1.intValue())))
                .andExpect(jsonPath("$.title", is("Pediatrics Guidelines")))
                .andExpect(jsonPath("$.category", is("Pediatrics")));

        // 2. Add same favorite for user_alpha (Idempotency)
        mockMvc.perform(post("/api/v1/integration/preferences/favorites")
                        .param("documentId", documentId1.toString())
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId", is(documentId1.intValue())));

        // 3. Get favorites for user_alpha (should have 1 item)
        mockMvc.perform(get("/api/v1/integration/preferences/favorites")
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentId", is(documentId1.intValue())));

        // 4. Get favorites for user_beta (should be empty - Isolation)
        mockMvc.perform(get("/api/v1/integration/preferences/favorites")
                        .header("X-User-Name", "user_beta")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 5. Add favorite for a non-existent document
        mockMvc.perform(post("/api/v1/integration/preferences/favorites")
                        .param("documentId", "999999")
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNotFound());

        // 6. Delete favorite for user_alpha
        mockMvc.perform(delete("/api/v1/integration/preferences/favorites/{documentId}", documentId1)
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNoContent());

        // 7. Verify favorites list for user_alpha is empty
        mockMvc.perform(get("/api/v1/integration/preferences/favorites")
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 8. Delete non-existent document favorite
        mockMvc.perform(delete("/api/v1/integration/preferences/favorites/{documentId}", 999999L)
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSavedQueriesWorkflows() throws Exception {
        // 1. Save query for user_alpha
        String qStr = mockMvc.perform(post("/api/v1/integration/preferences/saved-queries")
                        .param("query", "pediatric residency protocols")
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "ORDINATOR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.queryText", is("pediatric residency protocols")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long queryId = Long.parseLong(qStr.split("\"id\":")[1].split(",")[0].trim());

        // 2. Save same query for user_alpha (Idempotency)
        mockMvc.perform(post("/api/v1/integration/preferences/saved-queries")
                        .param("query", "pediatric residency protocols")
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "ORDINATOR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(queryId.intValue())));

        // 3. Save empty query (Bad Request)
        mockMvc.perform(post("/api/v1/integration/preferences/saved-queries")
                        .param("query", "   ")
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "ORDINATOR"))
                .andExpect(status().isBadRequest());

        // 4. Get saved queries for user_alpha (should have 1 item)
        mockMvc.perform(get("/api/v1/integration/preferences/saved-queries")
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "ORDINATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].queryText", is("pediatric residency protocols")));

        // 5. Get saved queries for user_beta (should be empty - Isolation)
        mockMvc.perform(get("/api/v1/integration/preferences/saved-queries")
                        .header("X-User-Name", "user_beta")
                        .header("X-User-Role", "ORDINATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 6. Delete user_alpha's query using user_beta's context (Forbidden)
        mockMvc.perform(delete("/api/v1/integration/preferences/saved-queries/{id}", queryId)
                        .header("X-User-Name", "user_beta")
                        .header("X-User-Role", "ORDINATOR"))
                .andExpect(status().isForbidden());

        // 7. Delete non-existent query (NotFound)
        mockMvc.perform(delete("/api/v1/integration/preferences/saved-queries/{id}", 999999L)
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "ORDINATOR"))
                .andExpect(status().isNotFound());

        // 8. Delete user_alpha's query successfully
        mockMvc.perform(delete("/api/v1/integration/preferences/saved-queries/{id}", queryId)
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "ORDINATOR"))
                .andExpect(status().isNoContent());

        // 9. Verify saved queries list for user_alpha is empty
        mockMvc.perform(get("/api/v1/integration/preferences/saved-queries")
                        .header("X-User-Name", "user_alpha")
                        .header("X-User-Role", "ORDINATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
