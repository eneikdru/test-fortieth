package com.eneik.generated.knowledgebase;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class KbUserFavoriteId implements Serializable {

    private Long userId;
    private Long documentId;

    public KbUserFavoriteId() {}

    public KbUserFavoriteId(Long userId, Long documentId) {
        this.userId = userId;
        this.documentId = documentId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbUserFavoriteId that = (KbUserFavoriteId) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, documentId);
    }
}
