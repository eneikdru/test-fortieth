package com.eneik.generated.integration;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "synced_roles")
public class SyncedRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_role_name", nullable = false, unique = true)
    private String externalRoleName;

    @Column(name = "internal_eios_identifier", nullable = false)
    private String internalEiosIdentifier;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalRoleName() {
        return externalRoleName;
    }

    public void setExternalRoleName(String externalRoleName) {
        this.externalRoleName = externalRoleName;
    }

    public String getInternalEiosIdentifier() {
        return internalEiosIdentifier;
    }

    public void setInternalEiosIdentifier(String internalEiosIdentifier) {
        this.internalEiosIdentifier = internalEiosIdentifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
