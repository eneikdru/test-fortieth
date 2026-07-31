package com.eneik.generated.knowledgebase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class KbDocumentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbUserRepository userRepository;

    @Autowired
    private KbAuditLogRepository auditLogRepository;

    private KbUser author;
    private KbDocument document;

    @BeforeEach
    public void setup() {
        documentRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        author = new KbUser();
        author.setUsername("admin_user");
        author.setRole("ADMIN");
        author = userRepository.save(author);

        document = new KbDocument();
        document.setTitle("Important Standard");
        document.setCategory("Standards");
        document.setAuthor(author);
        document = documentRepository.save(document);
    }

    @Test
    public void studentCannotDeleteDocumentReturns403() throws Exception {
        // Given a student user attempting to delete a document
        mockMvc.perform(delete("/api/kb/documents/" + document.getId())
                .header("X-Moodle-Role", "student")
                .header("X-User-Id", author.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Document should still exist
        assertThat(documentRepository.findById(document.getId())).isPresent();
    }

    @Test
    public void adminCanDeleteDocumentAndActionIsAudited() throws Exception {
        // Given a non-student user attempting to delete a document
        mockMvc.perform(delete("/api/kb/documents/" + document.getId())
                .header("X-Moodle-Role", "admin")
                .header("X-User-Id", author.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Document should be deleted
        assertThat(documentRepository.findById(document.getId())).isEmpty();

        // Audit log should be recorded securely
        List<KbAuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        KbAuditLog log = logs.get(0);
        assertThat(log.getAction()).isEqualTo("DOCUMENT_DELETE");
        assertThat(log.getTargetEntity()).isEqualTo("KbDocument");
        assertThat(log.getTargetId()).isEqualTo(document.getId());
        assertThat(log.getUser().getId()).isEqualTo(author.getId());
        assertThat(log.getDetails()).contains("Important Standard");
    }
}
