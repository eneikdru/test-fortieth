package com.eneik.generated.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FinancialAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BudgetDocumentRepository budgetDocumentRepository;

    @Autowired
    private SyncedRoleRepository syncedRoleRepository;

    @BeforeEach
    public void setup() {
        budgetDocumentRepository.deleteAll();
        syncedRoleRepository.deleteAll();
    }

    @Test
    public void testStudentAccessDeniedToBudgetDocuments() throws Exception {
        // Given a student user in Moodle (identified via X-Moodle-Role: student)
        // When they attempt to access budget documents (GET or POST)
        // Then access is explicitly denied (403 Forbidden)
        mockMvc.perform(get("/api/financial/documents")
                        .header("X-Moodle-Role", "student")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/financial/documents")
                        .header("X-Moodle-Role", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Q4 Budget\",\"content\":\"Sensitive financial data\",\"categoryId\":1}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testEconomistAccessGrantedToBudgetDocuments() throws Exception {
        // Given an economist role (identified via X-Moodle-Role: economist)
        // When they access budget documents or create one
        // Then it grants read and write access to all financial block categories/documents
        mockMvc.perform(post("/api/financial/documents")
                        .header("X-Moodle-Role", "economist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"2027 Budget Proposal\",\"content\":\"Comprehensive financial planning\",\"categoryId\":1}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("2027 Budget Proposal"))
                .andExpect(jsonPath("$.content").value("Comprehensive financial planning"));

        mockMvc.perform(get("/api/financial/documents")
                        .header("X-Moodle-Role", "economist")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("2027 Budget Proposal"));
    }

    @Test
    public void testConfiguredSyncedEconomistRoleAccessGranted() throws Exception {
        // Given a synced role mapped to internal economist identifier in DB
        SyncedRole economistRole = new SyncedRole();
        economistRole.setExternalRoleName("custom_economist_role");
        economistRole.setInternalEiosIdentifier("EIOS_ECONOMIST");
        economistRole.setDescription("Custom mapped economist role");
        syncedRoleRepository.save(economistRole);

        // When configured, it grants read and write access to financial documents
        mockMvc.perform(post("/api/financial/documents")
                        .header("X-Moodle-Role", "custom_economist_role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Database Synced Economist Budget\",\"content\":\"Mapped role planning\",\"categoryId\":2}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/financial/documents")
                        .header("X-Moodle-Role", "custom_economist_role")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Database Synced Economist Budget"));
    }
}
