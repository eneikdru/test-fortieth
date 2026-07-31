package com.eneik.generated.knowledgebase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@RestController
@RequestMapping("/api/kb/documents")
public class KbDocumentController {

    private final KbDocumentRepository documentRepository;
    private final KbAuditLogRepository auditLogRepository;
    private final KbUserRepository userRepository;

    public KbDocumentController(KbDocumentRepository documentRepository,
                                KbAuditLogRepository auditLogRepository,
                                KbUserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @RequestHeader(value = "X-Moodle-Role", required = false) String moodleRole,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        // Default Deny (Fail Closed)
        if (moodleRole == null || "student".equalsIgnoreCase(moodleRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Allowed roles according to the brief: Admin, Content Manager
        // Treating missing/student as blocked, explicitly checking allowed roles is safer
        if (!("admin".equalsIgnoreCase(moodleRole) || "content_manager".equalsIgnoreCase(moodleRole) || "admin_user".equalsIgnoreCase(moodleRole))) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<KbDocument> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        KbDocument document = docOpt.get();

        KbUser user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // Log the action securely
        KbAuditLog log = new KbAuditLog();
        log.setUser(user);
        log.setAction("DOCUMENT_DELETE");
        log.setTargetEntity("KbDocument");
        log.setTargetId(id);
        log.setDetails("Document deleted with title: " + document.getTitle());
        auditLogRepository.save(log);

        // Delete the document
        documentRepository.delete(document);

        return ResponseEntity.noContent().build();
    }
}
