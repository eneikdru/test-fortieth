package com.eneik.generated.knowledgebase;

import jakarta.persistence.*;

@Entity
@Table(name = "kb_user_favorites")
public class KbUserFavorite {

    @EmbeddedId
    private KbUserFavoriteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private KbUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("documentId")
    @JoinColumn(name = "document_id")
    private KbDocument document;

    public KbUserFavorite() {}

    public KbUserFavorite(KbUser user, KbDocument document) {
        this.user = user;
        this.document = document;
        this.id = new KbUserFavoriteId(user.getId(), document.getId());
    }

    public KbUserFavoriteId getId() { return id; }
    public void setId(KbUserFavoriteId id) { this.id = id; }

    public KbUser getUser() { return user; }
    public void setUser(KbUser user) { this.user = user; }

    public KbDocument getDocument() { return document; }
    public void setDocument(KbDocument document) { this.document = document; }
}
