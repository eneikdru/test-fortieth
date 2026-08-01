package com.eneik.generated.integration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MetricController {

    private final MetricService metricService;

    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @GetMapping({"/metrics/feature-readiness", "/integration/metrics/feature-readiness"})
    public ResponseEntity<Map<String, Object>> getFeatureReadiness() {
        double readiness = metricService.calculateFeatureReadiness();
        Map<String, Object> resp = new HashMap<>();
        resp.put("readiness", readiness);
        resp.put("status", readiness >= 1.0 - 1e-9 ? "COMPLETED" : "IN_PROGRESS");
        return ResponseEntity.ok(resp);
    }

    @GetMapping({"/metrics/deliverables", "/integration/metrics/deliverables"})
    public ResponseEntity<Map<String, Object>> getDeliverableDetail() {
        String detail = metricService.calculateDeliverableDetail();
        Map<String, Object> resp = new HashMap<>();
        resp.put("deliverable_detail", detail);
        resp.put("status", detail);
        return ResponseEntity.ok(resp);
    }
}
