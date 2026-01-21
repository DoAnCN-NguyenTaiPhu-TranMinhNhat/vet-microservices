package org.springframework.samples.petclinic.genai.dto;

import java.util.List;

public record AiDiagnosisResponse(
        String diagnosis,
        Double confidence,
        List<TopKItem> top_k
) {
    public record TopKItem(String label, Double prob) {
    }
}
