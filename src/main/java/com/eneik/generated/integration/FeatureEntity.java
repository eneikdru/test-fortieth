package com.eneik.generated.integration;

import jakarta.persistence.*;

@Entity
@Table(name = "features")
public class FeatureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "readiness_ratio", nullable = false)
    private double readinessRatio = 0.0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getReadinessRatio() {
        return readinessRatio;
    }

    public void setReadinessRatio(double readinessRatio) {
        this.readinessRatio = readinessRatio;
    }
}
