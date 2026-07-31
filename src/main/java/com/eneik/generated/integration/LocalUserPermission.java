package com.eneik.generated.integration;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "local_user_permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "internal_eios_identifier"})
})
public class LocalUserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "internal_eios_identifier", nullable = false)
    private String internalEiosIdentifier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getInternalEiosIdentifier() {
        return internalEiosIdentifier;
    }

    public void setInternalEiosIdentifier(String internalEiosIdentifier) {
        this.internalEiosIdentifier = internalEiosIdentifier;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
