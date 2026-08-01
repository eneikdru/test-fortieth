package com.eneik.generated.knowledgebase;

import com.eneik.generated.integration.LmsMetadata;
import com.eneik.generated.integration.LmsMetadataRepository;
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

    @Autowired
    private LmsMetadataRepository lmsMetadataRepository;

    @BeforeEach
    public void setup() {
        // Clear database in correct order of constraints
        auditLogRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
        lmsMetadataRepository.deleteAll();
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
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Инструкция ЦНИИ Эпидемиологии"))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.fileType").value("pdf"));

        // 3. Search with synonyms (Query: "федеральный государственный образовательный стандарт" should match "ФГОС" doc)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "федеральный государственный образовательный стандарт")
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
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 5. Check specialty filter ("ординатура" matches both)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "ординатура")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 6. Check specialty filter for "исследования" (matches only second doc)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "исследования")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Инструкция ЦНИИ Эпидемиологии"));

        // 7. Check documentType filter (pdf)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("documentType", "pdf")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Инструкция ЦНИИ Эпидемиологии"));

        // 8. Check updatedAfter filter (future date should yield 0 results)
        String futureIso = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_DATE_TIME);
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("updatedAfter", futureIso)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 9. Get document details by ID and check VIEW audit log triggers
        mockMvc.perform(get("/api/v1/integration/documents/{id}", docId)
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
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Регламент ФГОС ЦНИИ Обновленный"))
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.fileType").value("docx"));

        // 11. Verify file download
        mockMvc.perform(get("/api/v1/integration/documents/download/{id}/version/2", docId))
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
        mockMvc.perform(delete("/api/v1/integration/documents/{id}", docId))
                .andExpect(status().isNoContent());

        // Verify delete is 404 now
        mockMvc.perform(get("/api/v1/integration/documents/{id}", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSearchWithTypoTolerance() throws Exception {
        // Upload a document with title containing "Эпидемиология"
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "epidem.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Regulations on scientific research in epidemiology.".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file1)
                        .param("title", "Введение в Эпидемиологию")
                        .param("category", "Материалы")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Upload another document with title "Регламент аспирантуры"
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "reg.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Postgraduate student guidelines.".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file2)
                        .param("title", "Регламент аспирантуры")
                        .param("category", "Инструкции")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Search with typo in "Эпидемиологию" -> "Эпидемология"
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "Эпидемология")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Введение в Эпидемиологию"));

        // Search with typo in "Регламент" -> "Регламет"
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "Регламет")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Регламент аспирантуры"));
    }

    @Test
    public void testSearchPagination() throws Exception {
        // Upload 12 documents to check default pagination (default limit is 10)
        for (int i = 1; i <= 12; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "doc_" + i + ".txt",
                    MediaType.TEXT_PLAIN_VALUE,
                    ("Content for document number " + i).getBytes(StandardCharsets.UTF_8)
            );

            mockMvc.perform(multipart("/api/v1/integration/documents")
                            .file(file)
                            .param("title", "Standard Doc " + i)
                            .param("category", "PaginationTestCategory")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated());
        }

        // 1. Default pagination without page/size parameters - should apply default limit of 10
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "PaginationTestCategory") // matches category via special filter
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].title").value("Standard Doc 1"))
                .andExpect(jsonPath("$[9].title").value("Standard Doc 10"));

        // 2. Specified page and size (page=0, size=5)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "PaginationTestCategory")
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].title").value("Standard Doc 1"))
                .andExpect(jsonPath("$[4].title").value("Standard Doc 5"));

        // 3. Specified page and size (page=1, size=5) - second page
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "PaginationTestCategory")
                        .param("page", "1")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].title").value("Standard Doc 6"))
                .andExpect(jsonPath("$[4].title").value("Standard Doc 10"));

        // 4. Specified page and size (page=2, size=5) - third page with remaining 2 documents
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "PaginationTestCategory")
                        .param("page", "2")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Standard Doc 11"))
                .andExpect(jsonPath("$[1].title").value("Standard Doc 12"));

        // 5. Using pageNumber and pageSize (pageNumber=1, pageSize=4)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "PaginationTestCategory")
                        .param("pageNumber", "1")
                        .param("pageSize", "4")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].title").value("Standard Doc 5"))
                .andExpect(jsonPath("$[3].title").value("Standard Doc 8"));

        // 6. Using offset and limit (offset=2, limit=3)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "PaginationTestCategory")
                        .param("offset", "2")
                        .param("limit", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Standard Doc 3"))
                .andExpect(jsonPath("$[2].title").value("Standard Doc 5"));

        // 7. Requesting explicitly paginated response with paginated=true (Acceptance Criteria verification)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "PaginationTestCategory")
                        .param("paginated", "true")
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.items[0].title").value("Standard Doc 1"));
    }

    @Test
    public void testSearchWithTypoAndSynonymExpansion() throws Exception {
        // Upload a document with "ФГОС" in the title
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fgos_test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Regulations for ФГОС.".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Регламент ФГОС")
                        .param("category", "Акты")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Search with typo: "образавательный стандарт" ('образавательный' has a typo, should be 'образовательный')
        // "образовательный стандарт" is in synonym group with "фгос"
        // Corrected intent should be "образовательный стандарт" which expands to "ФГОС", returning the "Регламент ФГОС" doc.
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "образавательный стандарт")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Регламент ФГОС"));
    }

    @Test
    public void testSearchExactPriorityOverFuzzy() throws Exception {
        // 1. Upload Doc with correct name "Эпидемиология"
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "correct.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Correct epidemiology document.".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file1)
                        .param("title", "Основы Эпидемиологии")
                        .param("category", "Учебники")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // 2. Upload Doc with misspelled name "Эпидемология" literally
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "typo.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Misspelled document for testing.".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file2)
                        .param("title", "Основы Эпидемологии")
                        .param("category", "Опечатки")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // When we search for "Эпидемиологии" (correct spelling), exact match ("Основы Эпидемиологии") must be ranked first.
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "Эпидемиологии")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Основы Эпидемиологии"))
                .andExpect(jsonPath("$[1].title").value("Основы Эпидемологии"));

        // When we search for "Эпидемологии" (spelled with typo), literal match "Основы Эпидемологии" must be ranked first.
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "Эпидемологии")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Основы Эпидемологии"))
                .andExpect(jsonPath("$[1].title").value("Основы Эпидемиологии"));
    }

    @Test
    public void testBinaryExtractionWithTika() throws Exception {
        // Upload a dummy binary file (we use a simple text string here, but Tika parses it as well, proving the flow)
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dummy_binary.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "Simulated binary content that Tika will parse".getBytes(StandardCharsets.UTF_8)
        );

        // Upload
        String responseContent = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Tika Binary Test Doc")
                        .param("category", "Test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Verify it was saved correctly
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "Simulated binary content")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Tika Binary Test Doc"));
    }

    @Test
    public void testUnifiedSearchWithLocalAndLmsMetadata() throws Exception {
        // 1. Upload a local document
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "local_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Regulations on postgraduate residency research.".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file1)
                        .param("title", "Регламент аспирантуры")
                        .param("category", "Инструкции")
                        .param("tags", "аспирантура")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // 2. Insert LmsMetadata items representing synchronized SDO/Teachbase content
        LmsMetadata m1 = new LmsMetadata();
        m1.setExternalId("lms-888");
        m1.setMetadataKey("title");
        m1.setMetadataValue("Epidemiology Course Guidelines");
        lmsMetadataRepository.save(m1);

        LmsMetadata m2 = new LmsMetadata();
        m2.setExternalId("lms-888");
        m2.setMetadataKey("category");
        m2.setMetadataValue("Teaching Material");
        lmsMetadataRepository.save(m2);

        LmsMetadata m3 = new LmsMetadata();
        m3.setExternalId("lms-888");
        m3.setMetadataKey("filetype");
        m3.setMetadataValue("pdf");
        lmsMetadataRepository.save(m3);

        LmsMetadata m4 = new LmsMetadata();
        m4.setExternalId("lms-888");
        m4.setMetadataKey("tags");
        m4.setMetadataValue("epidemiology, sdo, teachbase");
        lmsMetadataRepository.save(m4);

        // 3. Search for local document query "аспирантуры"
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "аспирантуры")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Регламент аспирантуры"))
                .andExpect(jsonPath("$[0].id", greaterThan(0)));

        // 4. Search for LMS material query "Guidelines"
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "Guidelines")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Epidemiology Course Guidelines"))
                .andExpect(jsonPath("$[0].category").value("Teaching Material"))
                .andExpect(jsonPath("$[0].fileType").value("pdf"))
                .andExpect(jsonPath("$[0].id", lessThan(0))) // assigned negative ID
                .andExpect(jsonPath("$[0].tags", containsInAnyOrder("epidemiology", "sdo", "teachbase", "LMS", "SDO", "Teachbase")));

        // 5. Unified search query "epidemiology" or "sdo" returning virtual LMS document
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("query", "sdo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Epidemiology Course Guidelines"));

        // 6. Test specialty / category filter on virtual LMS document (Teaching Material)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("specialty", "Teaching Material")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Epidemiology Course Guidelines"));

        // 7. Test documentType filter on virtual LMS document (pdf)
        mockMvc.perform(get("/api/v1/integration/documents")
                        .param("documentType", "pdf")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Epidemiology Course Guidelines"));
    }
}
