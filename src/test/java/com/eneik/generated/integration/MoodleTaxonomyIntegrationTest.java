package com.eneik.generated.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class MoodleTaxonomyIntegrationTest {

    @Autowired
    private MoodleTaxonomyService taxonomyService;

    @Autowired
    private MoodleFinancialCategoryRepository categoryRepository;

    @Autowired
    private MoodleFinancialTagRepository tagRepository;

    @Autowired
    private MoodleGlossaryTermRepository glossaryTermRepository;

    @Test
    public void testTaxonomyAndGlossaryImport() throws Exception {
        // Given the taxonomy definition JSON
        String taxonomyJson = "{\n" +
                "  \"categories\": [\n" +
                "    {\n" +
                "      \"name\": \"Financial Administration\",\n" +
                "      \"description\": \"Departmental financial plans\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"Residency Budgets\",\n" +
                "      \"description\": \"Residency department budget\",\n" +
                "      \"parentName\": \"Financial Administration\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"tags\": [\n" +
                "    {\n" +
                "      \"name\": \"budget\",\n" +
                "      \"description\": \"Relates to fiscal budgets\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"workload\",\n" +
                "      \"description\": \"Relates to instructor workloads\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // When taxonomy definition is applied
        taxonomyService.importTaxonomy(taxonomyJson);

        // Then correct financial categories and tags are created
        Optional<MoodleFinancialCategory> parentOpt = categoryRepository.findByName("Financial Administration");
        assertThat(parentOpt).isPresent();
        assertThat(parentOpt.get().getDescription()).isEqualTo("Departmental financial plans");

        Optional<MoodleFinancialCategory> childOpt = categoryRepository.findByName("Residency Budgets");
        assertThat(childOpt).isPresent();
        assertThat(childOpt.get().getDescription()).isEqualTo("Residency department budget");
        assertThat(childOpt.get().getParentId()).isEqualTo(parentOpt.get().getId());

        Optional<MoodleFinancialTag> budgetTagOpt = tagRepository.findByName("budget");
        assertThat(budgetTagOpt).isPresent();
        assertThat(budgetTagOpt.get().getDescription()).isEqualTo("Relates to fiscal budgets");

        Optional<MoodleFinancialTag> workloadTagOpt = tagRepository.findByName("workload");
        assertThat(workloadTagOpt).isPresent();
        assertThat(workloadTagOpt.get().getDescription()).isEqualTo("Relates to instructor workloads");


        // Given a glossary definition JSON
        String glossaryJson = "{\n" +
                "  \"terms\": [\n" +
                "    {\n" +
                "      \"term\": \"budget\",\n" +
                "      \"definition\": \"A financial plan or projection of income and expenditure for a set period.\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"term\": \"workload\",\n" +
                "      \"definition\": \"The amount of work, teaching hours, or clinical assignments allocated to an instructor.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // When applied
        taxonomyService.importGlossary(glossaryJson);

        // Then terms like 'budget' and 'workload' are available system-wide
        Optional<MoodleGlossaryTerm> budgetTermOpt = glossaryTermRepository.findByTerm("budget");
        assertThat(budgetTermOpt).isPresent();
        assertThat(budgetTermOpt.get().getDefinition()).isEqualTo("A financial plan or projection of income and expenditure for a set period.");

        Optional<MoodleGlossaryTerm> workloadTermOpt = glossaryTermRepository.findByTerm("workload");
        assertThat(workloadTermOpt).isPresent();
        assertThat(workloadTermOpt.get().getDefinition()).isEqualTo("The amount of work, teaching hours, or clinical assignments allocated to an instructor.");
    }
}
