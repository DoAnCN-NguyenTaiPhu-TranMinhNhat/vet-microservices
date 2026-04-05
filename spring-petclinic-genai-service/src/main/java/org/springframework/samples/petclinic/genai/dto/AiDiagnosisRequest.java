package org.springframework.samples.petclinic.genai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record AiDiagnosisRequest(
        String animal_type,
        String gender,
        Integer age_months,
        Float weight_kg,
        Float temperature,
        Integer heart_rate,
        String current_season,
        String vaccination_status,
        String medical_history,
        String symptoms_list,
        Integer symptom_duration,
        /** Forwarded to vet-ai /predict as clinicId (UUID string); null = shared default model. */
        @JsonProperty("clinicId") UUID clinicId,
        /** Pet UUID for continuous-training logs; may also be sent via query param. */
        @JsonProperty("petId") UUID petId,
        @JsonProperty("visitId") Integer visitId
) {
}
