package org.springframework.samples.petclinic.genai.dto;

public record AiDiagnosisRequest(
        String animal_type,
        String gender,
        Integer age_months,
        Double weight_kg,
        Double temperature,
        Integer heart_rate,
        String current_season,
        String vaccination_status,
        String medical_history,
        String symptoms_list,
        Integer symptom_duration
) {
}
