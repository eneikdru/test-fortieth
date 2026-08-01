package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BackendObserverScript {

    private static final Logger log = LoggerFactory.getLogger(BackendObserverScript.class);

    private final MetricService metricService;

    public BackendObserverScript(MetricService metricService) {
        this.metricService = metricService;
    }

    public double calculateFeatureReadiness() {
        log.info("BackendObserverScript: calculating feature readiness.");
        return metricService.calculateFeatureReadiness();
    }

    public double getFeatureReadinessRatio() {
        return calculateFeatureReadiness();
    }
}
