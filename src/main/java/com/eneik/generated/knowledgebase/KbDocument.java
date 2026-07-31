package com.eneik.generated.knowledgebase;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "kb_documents")
public class KbDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private KbUser author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "kb_document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag_name")
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KbDocumentVersion> versions;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KbDocumentComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KbDocumentUpdateRequest> updateRequests = new ArrayList<>();

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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public KbUser getAuthor() { return author; }
    public void setAuthor(KbUser author) { this.author = author; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public List<KbDocumentVersion> getVersions() { return versions; }
    public void setVersions(List<KbDocumentVersion> versions) { this.versions = versions; }

    public List<KbDocumentComment> getComments() { return comments; }
    public void setComments(List<KbDocumentComment> comments) { this.comments = comments; }

    public List<KbDocumentUpdateRequest> getUpdateRequests() { return updateRequests; }
    public void setUpdateRequests(List<KbDocumentUpdateRequest> updateRequests) { this.updateRequests = updateRequests; }
}
