package org.springframework.samples.petclinic.genai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AiDiagnosisResponse(
        String diagnosis,
        Double confidence,
        @JsonProperty("top_k") List<TopKItem> top_k,
        @JsonProperty("modelVersion") String modelVersion,
        @JsonProperty("predictions") List<Map<String, Object>> predictions,
        @JsonProperty("predictionId") UUID predictionId,
        /** Echo from vet-ai /predict: canonical clinic UUID string applied to the prediction row. */
        @JsonProperty("clinicId") String clinicId
) {
    public record TopKItem(@JsonProperty("label") String label, @JsonProperty("prob") Double prob) {
    }

    public AiDiagnosisResponse(String diagnosis, Double confidence, List<TopKItem> top_k) {
        this(diagnosis, confidence, top_k, "v1.0", List.of(), null, null);
    }

    public AiDiagnosisResponse(String diagnosis, Double confidence, List<TopKItem> top_k,
                              String modelVersion, List<Map<String, Object>> predictions, UUID predictionId) {
        this(diagnosis, confidence, top_k, modelVersion, predictions, predictionId, null);
    }
}
