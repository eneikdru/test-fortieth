package com.eneik.generated.knowledgebase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class KnowledgeBaseSchemaTest {

    @Autowired
    private KbUserRepository userRepository;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbDocumentVersionRepository versionRepository;

    @Autowired
    private KbAuditLogRepository auditLogRepository;

    @Test
    public void testSchemaAndInsertOperations() {
        // 1. Create a user
        KbUser user = new KbUser();
        user.setUsername("testuser");
        user.setRole("AUTHOR");
        KbUser savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();

        // 2. Perform a document insert operation with metadata and tags
        KbDocument document = new KbDocument();
        document.setTitle("Test Document");
        document.setCategory("Test Category");
        document.setAuthor(savedUser);
        document.setTags(Set.of("tag1", "tag2"));

        KbDocument savedDocument = documentRepository.save(document);
        // Flush so that the tags are persisted into the element collection table
        documentRepository.flush();

        // Then the data is persisted correctly
        assertThat(savedDocument.getId()).isNotNull();
        assertThat(savedDocument.getCreatedAt()).isNotNull();
        assertThat(savedDocument.getUpdatedAt()).isNotNull();

        Optional<KbDocument> foundDocumentOpt = documentRepository.findById(savedDocument.getId());
        assertThat(foundDocumentOpt).isPresent();

        KbDocument foundDocument = foundDocumentOpt.get();
        assertThat(foundDocument.getTitle()).isEqualTo("Test Document");
        assertThat(foundDocument.getCategory()).isEqualTo("Test Category");
        assertThat(foundDocument.getAuthor().getId()).isEqualTo(savedUser.getId());
        assertThat(foundDocument.getTags()).containsExactlyInAnyOrder("tag1", "tag2");

        // 3. Insert a document version
        KbDocumentVersion version = new KbDocumentVersion();
        version.setDocument(foundDocument);
        version.setVersionNumber(1);
        version.setFilePath("/docs/test.pdf");
        version.setFileType("pdf");
        version.setCreatedBy(savedUser);

        KbDocumentVersion savedVersion = versionRepository.save(version);
        assertThat(savedVersion.getId()).isNotNull();
        assertThat(savedVersion.getCreatedAt()).isNotNull();

        // 4. Insert an audit log
        KbAuditLog log = new KbAuditLog();
        log.setUser(savedUser);
        log.setAction("DOCUMENT_CREATE");
        log.setTargetEntity("KbDocument");
        log.setTargetId(savedDocument.getId());
        log.setDetails("Document created successfully");

        KbAuditLog savedLog = auditLogRepository.save(log);
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getCreatedAt()).isNotNull();
    }
}
