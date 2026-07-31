package com.eneik.generated.knowledgebase;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kb_saved_queries")
public class KbSavedQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private KbUser user;

    @Column(name = "query_text", nullable = false, length = 1000)
    private String queryText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public KbSavedQuery() {}

    public KbSavedQuery(KbUser user, String queryText) {
        this.user = user;
        this.queryText = queryText;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KbUser getUser() { return user; }
    public void setUser(KbUser user) { this.user = user; }

    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
