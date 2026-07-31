package com.eneik.generated.knowledgebase;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kb_document_update_requests")
public class KbDocumentUpdateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private KbDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private KbUser user;

    @Column(name = "request_text", nullable = false, columnDefinition = "TEXT")
    private String requestText;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KbDocument getDocument() { return document; }
    public void setDocument(KbDocument document) { this.document = document; }

    public KbUser getUser() { return user; }
    public void setUser(KbUser user) { this.user = user; }

    public String getRequestText() { return requestText; }
    public void setRequestText(String requestText) { this.requestText = requestText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
