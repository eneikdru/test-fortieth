package com.eneik.generated.testfortieth;

import com.eneik.generated.testfortieth.model.*;
import com.eneik.generated.testfortieth.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SchemaPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentMetadataRepository documentMetadataRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    public void testSchemaAndPersistence() {
        // 1. Create and Save User
        User user = new User();
        user.setUsername("ivan_epidemiologist");
        user.setPasswordHash("argon2_hash_val");
        user.setRole("ADMIN");
        user.setEmail("ivan@cniie.ru");
        user = userRepository.save(user);
        assertNotNull(user.getId());

        // 2. Create and Save Document
        Document document = new Document();
        document.setTitle("Educational Program Guidelines 2026");
        document.setCategory("Regulation");
        document.setOwner(user);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        document.setTags(Set.of("ординатура", "нормативные акты", "ФГОС"));
        document = documentRepository.save(document);
        assertNotNull(document.getId());

        // 3. Save Metadata
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setDocument(document);
        metadata.setKey("educational_standard");
        metadata.setValue("FGOS-3-Epidemiology");
        metadata = documentMetadataRepository.save(metadata);
        assertNotNull(metadata.getId());

        // 4. Save Version
        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(1);
        version.setFilePath("/knowledge_base/files/guidelines_v1.pdf");
        version.setChecksum("d41d8cd98f00b204e9800998ecf8427e");
        version.setAuthor(user);
        version.setCreatedAt(LocalDateTime.now());
        version.setChangeDescription("Initial draft published");
        version = documentVersionRepository.save(version);
        assertNotNull(version.getId());

        // 5. Save Audit Log
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction("CREATE_DOCUMENT");
        auditLog.setDetails("Document 'Educational Program Guidelines 2026' was successfully created.");
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog = auditLogRepository.save(auditLog);
        assertNotNull(auditLog.getId());

        // 6. Verify Fetch and Associations
        Optional<Document> fetchedDocOpt = documentRepository.findById(document.getId());
        assertTrue(fetchedDocOpt.isPresent());
        Document fetchedDoc = fetchedDocOpt.get();
        assertEquals("Educational Program Guidelines 2026", fetchedDoc.getTitle());
        assertEquals("Regulation", fetchedDoc.getCategory());
        assertEquals(user.getId(), fetchedDoc.getOwner().getId());
        assertTrue(fetchedDoc.getTags().contains("ординатура"));
        assertTrue(fetchedDoc.getTags().contains("нормативные акты"));
        assertTrue(fetchedDoc.getTags().contains("ФГОС"));
    }
}
