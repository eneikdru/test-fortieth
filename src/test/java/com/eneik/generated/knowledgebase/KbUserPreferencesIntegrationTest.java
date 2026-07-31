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

    private String getJwtToken(String username, String role) throws Exception {
        String responseStr = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return responseStr.split("\"token\":\"")[1].split("\"")[0];
    }

    @BeforeEach
    public void setup() throws Exception {
        userFavoriteRepository.deleteAll();
        savedQueryRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        String token = getJwtToken("admin_uploader", "ADMINISTRATOR");

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
                        .header("Authorization", "Bearer " + token)
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
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        documentId2 = Long.parseLong(response2.split("\"id\":")[1].split(",")[0].trim());
    }

    @Test
    public void testFavoritesWorkflows() throws Exception {
        String alphaToken = getJwtToken("user_alpha", "STUDENT");
        String betaToken = getJwtToken("user_beta", "STUDENT");

        // 1. Add favorite for user_alpha
        mockMvc.perform(post("/api/v1/integration/preferences/favorites")
                        .param("documentId", documentId1.toString())
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId", is(documentId1.intValue())))
                .andExpect(jsonPath("$.title", is("Pediatrics Guidelines")))
                .andExpect(jsonPath("$.category", is("Pediatrics")));

        // 2. Add same favorite for user_alpha (Idempotency)
        mockMvc.perform(post("/api/v1/integration/preferences/favorites")
                        .param("documentId", documentId1.toString())
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId", is(documentId1.intValue())));

        // 3. Get favorites for user_alpha (should have 1 item)
        mockMvc.perform(get("/api/v1/integration/preferences/favorites")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentId", is(documentId1.intValue())));

        // 4. Get favorites for user_beta (should be empty - Isolation)
        mockMvc.perform(get("/api/v1/integration/preferences/favorites")
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 5. Add favorite for a non-existent document
        mockMvc.perform(post("/api/v1/integration/preferences/favorites")
                        .param("documentId", "999999")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isNotFound());

        // 6. Delete favorite for user_alpha
        mockMvc.perform(delete("/api/v1/integration/preferences/favorites/{documentId}", documentId1)
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isNoContent());

        // 7. Verify favorites list for user_alpha is empty
        mockMvc.perform(get("/api/v1/integration/preferences/favorites")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 8. Delete non-existent document favorite
        mockMvc.perform(delete("/api/v1/integration/preferences/favorites/{documentId}", 999999L)
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSavedQueriesWorkflows() throws Exception {
        String alphaToken = getJwtToken("user_alpha", "ORDINATOR");
        String betaToken = getJwtToken("user_beta", "ORDINATOR");

        // 1. Save query for user_alpha
        String qStr = mockMvc.perform(post("/api/v1/integration/preferences/saved-queries")
                        .param("query", "pediatric residency protocols")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.queryText", is("pediatric residency protocols")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long queryId = Long.parseLong(qStr.split("\"id\":")[1].split(",")[0].trim());

        // 2. Save same query for user_alpha (Idempotency)
        mockMvc.perform(post("/api/v1/integration/preferences/saved-queries")
                        .param("query", "pediatric residency protocols")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(queryId.intValue())));

        // 3. Save empty query (Bad Request)
        mockMvc.perform(post("/api/v1/integration/preferences/saved-queries")
                        .param("query", "   ")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isBadRequest());

        // 4. Get saved queries for user_alpha (should have 1 item)
        mockMvc.perform(get("/api/v1/integration/preferences/saved-queries")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].queryText", is("pediatric residency protocols")));

        // 5. Get saved queries for user_beta (should be empty - Isolation)
        mockMvc.perform(get("/api/v1/integration/preferences/saved-queries")
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 6. Delete user_alpha's query using user_beta's context (Forbidden)
        mockMvc.perform(delete("/api/v1/integration/preferences/saved-queries/{id}", queryId)
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isForbidden());

        // 7. Delete non-existent query (NotFound)
        mockMvc.perform(delete("/api/v1/integration/preferences/saved-queries/{id}", 999999L)
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isNotFound());

        // 8. Delete user_alpha's query successfully
        mockMvc.perform(delete("/api/v1/integration/preferences/saved-queries/{id}", queryId)
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isNoContent());

        // 9. Verify saved queries list for user_alpha is empty
        mockMvc.perform(get("/api/v1/integration/preferences/saved-queries")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testUserProfileEndpointWithHeadersAndIsolation() throws Exception {
        String profile1Token = getJwtToken("user_profile_1", "STUDENT");
        String profile2Token = getJwtToken("user_profile_2", "STUDENT");

        // 1. Create a favorite document for user_profile_1
        mockMvc.perform(post("/api/v1/integration/preferences/favorites")
                        .param("documentId", documentId1.toString())
                        .header("Authorization", "Bearer " + profile1Token))
                .andExpect(status().isCreated());

        // 2. Save a query for user_profile_1
        mockMvc.perform(post("/api/v1/integration/preferences/saved-queries")
                        .param("query", "pediatric immunology")
                        .header("Authorization", "Bearer " + profile1Token))
                .andExpect(status().isCreated());

        // 3. Fetch user_profile_1's profile using JWT and verify details
        mockMvc.perform(get("/api/v1/integration/preferences/profile")
                        .header("Authorization", "Bearer " + profile1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("user_profile_1")))
                .andExpect(jsonPath("$.role", is("STUDENT")))
                .andExpect(jsonPath("$.favorites", hasSize(1)))
                .andExpect(jsonPath("$.favorites[0].documentId", is(documentId1.intValue())))
                .andExpect(jsonPath("$.favorites[0].title", is("Pediatrics Guidelines")))
                .andExpect(jsonPath("$.savedQueries", hasSize(1)))
                .andExpect(jsonPath("$.savedQueries[0].queryText", is("pediatric immunology")));

        // 4. Fetch user_profile_2's profile and check isolation (should be empty/no favorites or queries)
        mockMvc.perform(get("/api/v1/integration/preferences/profile")
                        .header("Authorization", "Bearer " + profile2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("user_profile_2")))
                .andExpect(jsonPath("$.role", is("STUDENT")))
                .andExpect(jsonPath("$.favorites", hasSize(0)))
                .andExpect(jsonPath("$.savedQueries", hasSize(0)));
    }

    @Test
    public void testUserProfileEndpointWithJwtAuthentication() throws Exception {
        // 1. Log in to get standard Student JWT token
        String responseStr = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"jwt_user\",\"password\":\"password\",\"role\":\"ORDINATOR\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = responseStr.split("\"token\":\"")[1].split("\"")[0];

        // 2. Add a favorite using JWT
        mockMvc.perform(post("/api/v1/integration/preferences/favorites")
                        .param("documentId", documentId2.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId", is(documentId2.intValue())));

        // 3. Save a query using JWT
        mockMvc.perform(post("/api/v1/integration/preferences/saved-queries")
                        .param("query", "cardiology valve repair")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // 4. Fetch profile using JWT and verify it returned correctly
        mockMvc.perform(get("/api/v1/integration/preferences/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("jwt_user")))
                .andExpect(jsonPath("$.role", is("ORDINATOR")))
                .andExpect(jsonPath("$.favorites", hasSize(1)))
                .andExpect(jsonPath("$.favorites[0].documentId", is(documentId2.intValue())))
                .andExpect(jsonPath("$.favorites[0].title", is("Cardiology Guidelines")))
                .andExpect(jsonPath("$.savedQueries", hasSize(1)))
                .andExpect(jsonPath("$.savedQueries[0].queryText", is("cardiology valve repair")));
    }

    @Test
    public void testUserProfileEndpointWithInvalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/integration/preferences/profile")
                        .header("Authorization", "Bearer invalid-jwt-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSearchAndDocumentLogicForFavoritesAndSavedQueries() throws Exception {
        String token = getJwtToken("search_user", "STUDENT");

        // 1. Initial State: search query list is empty
        mockMvc.perform(get("/api/v1/integration/preferences/saved-queries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 2. Perform a search with query "Guidelines". This should auto-save the query.
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "Guidelines")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // both documents have "Guidelines" in their titles
                .andExpect(jsonPath("$[0].isFavorite", is(false)))
                .andExpect(jsonPath("$[1].isFavorite", is(false)));

        // 3. Verify the query was auto-saved
        String savedQueriesStr = mockMvc.perform(get("/api/v1/integration/preferences/saved-queries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].queryText", is("Guidelines")))
                .andReturn().getResponse().getContentAsString();

        Long savedQueryId = Long.parseLong(savedQueriesStr.split("\"id\":")[1].split(",")[0].trim());

        // 4. Mark document 1 as favorite
        mockMvc.perform(post("/api/v1/integration/preferences/favorites")
                        .param("documentId", documentId1.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // 5. Search again: document 1 should have isFavorite = true, document 2 should have isFavorite = false
        mockMvc.perform(get("/api/v1/integration/documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(documentId1.intValue())))
                .andExpect(jsonPath("$[0].isFavorite", is(true)))
                .andExpect(jsonPath("$[1].id", is(documentId2.intValue())))
                .andExpect(jsonPath("$[1].isFavorite", is(false)));

        // 6. Search with favoritesOnly = true: should only return document 1
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("favoritesOnly", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(documentId1.intValue())))
                .andExpect(jsonPath("$[0].isFavorite", is(true)));

        // 7. Search using savedQueryId: should execute search using the auto-saved "Guidelines" query
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("savedQueryId", savedQueryId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
