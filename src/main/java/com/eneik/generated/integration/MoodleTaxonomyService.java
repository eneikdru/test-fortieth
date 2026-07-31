package com.eneik.generated.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MoodleTaxonomyService {

    private final MoodleFinancialCategoryRepository categoryRepository;
    private final MoodleFinancialTagRepository tagRepository;
    private final MoodleGlossaryTermRepository glossaryTermRepository;
    private final ObjectMapper objectMapper;

    public MoodleTaxonomyService(MoodleFinancialCategoryRepository categoryRepository,
                                 MoodleFinancialTagRepository tagRepository,
                                 MoodleGlossaryTermRepository glossaryTermRepository,
                                 ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.glossaryTermRepository = glossaryTermRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Parses and applies a taxonomy definition JSON.
     * Expected structure:
     * {
     *   "categories": [
     *     { "name": "Residency Budgets", "description": "...", "parentName": "Optional parent" }
     *   ],
     *   "tags": [
     *     { "name": "budget", "description": "..." }
     *   ]
     * }
     */
    @Transactional
    public void importTaxonomy(String jsonInput) throws Exception {
        Map<String, Object> data = objectMapper.readValue(jsonInput, new TypeReference<Map<String, Object>>() {});

        if (data.containsKey("categories")) {
            List<Map<String, String>> categories = objectMapper.convertValue(data.get("categories"), new TypeReference<List<Map<String, String>>>() {});
            for (Map<String, String> catMap : categories) {
                String name = catMap.get("name");
                String description = catMap.get("description");
                String parentName = catMap.get("parentName");

                Long parentId = null;
                if (parentName != null && !parentName.trim().isEmpty()) {
                    MoodleFinancialCategory parent = categoryRepository.findByName(parentName)
                            .orElseGet(() -> {
                                MoodleFinancialCategory p = new MoodleFinancialCategory();
                                p.setName(parentName);
                                return categoryRepository.save(p);
                            });
                    parentId = parent.getId();
                }

                MoodleFinancialCategory category = categoryRepository.findByName(name)
                        .orElse(new MoodleFinancialCategory());

                category.setName(name);
                category.setDescription(description);
                category.setParentId(parentId);
                categoryRepository.save(category);
            }
        }

        if (data.containsKey("tags")) {
            List<Map<String, String>> tags = objectMapper.convertValue(data.get("tags"), new TypeReference<List<Map<String, String>>>() {});
            for (Map<String, String> tagMap : tags) {
                String name = tagMap.get("name");
                String description = tagMap.get("description");

                MoodleFinancialTag tag = tagRepository.findByName(name)
                        .orElse(new MoodleFinancialTag());

                tag.setName(name);
                tag.setDescription(description);
                tagRepository.save(tag);
            }
        }
    }

    /**
     * Parses and applies a glossary definition JSON.
     * Expected structure:
     * {
     *   "terms": [
     *     { "term": "budget", "definition": "..." }
     *   ]
     * }
     */
    @Transactional
    public void importGlossary(String jsonInput) throws Exception {
        Map<String, Object> data = objectMapper.readValue(jsonInput, new TypeReference<Map<String, Object>>() {});

        if (data.containsKey("terms")) {
            List<Map<String, String>> terms = objectMapper.convertValue(data.get("terms"), new TypeReference<List<Map<String, String>>>() {});
            for (Map<String, String> termMap : terms) {
                String termName = termMap.get("term");
                String definition = termMap.get("definition");

                MoodleGlossaryTerm glossaryTerm = glossaryTermRepository.findByTerm(termName)
                        .orElse(new MoodleGlossaryTerm());

                glossaryTerm.setTerm(termName);
                glossaryTerm.setDefinition(definition);
                glossaryTermRepository.save(glossaryTerm);
            }
        }
    }
}
