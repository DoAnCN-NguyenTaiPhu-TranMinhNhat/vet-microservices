package org.springframework.samples.petclinic.genai.dto;

import java.util.List;
import java.util.Map;

public record AiDiagnosisResponse(
        String diagnosis,
        Double confidence,
        List<TopKItem> top_k,
        String modelVersion,
        List<Map<String, Object>> predictions
) {
    public record TopKItem(String label, Double prob) {
    }
    
    // Constructor for backward compatibility
    public AiDiagnosisResponse(String diagnosis, Double confidence, List<TopKItem> top_k) {
        this(diagnosis, confidence, top_k, "v1.0", List.of());
    }
}
