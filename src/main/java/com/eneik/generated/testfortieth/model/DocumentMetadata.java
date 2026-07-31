package com.eneik.generated.testfortieth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "document_metadata", uniqueConstraints = {
    @UniqueConstraint(name = "uk_document_metadata", columnNames = {"document_id", "meta_key"})
})
public class DocumentMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "meta_key", nullable = false, length = 100)
    private String key;

    @Column(name = "meta_value", nullable = false, length = 1024)
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
