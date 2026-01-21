package org.springframework.samples.petclinic.genai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisRequest;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiDiagnosisClient {

    private final WebClient webClient;

    public AiDiagnosisClient(@Value("${vetai.diagnosis.url:http://localhost:8000}") String aiServiceUrl) {
        this.webClient = WebClient.builder().baseUrl(aiServiceUrl).build();
    }

    public AiDiagnosisResponse predict(AiDiagnosisRequest request) {
        return webClient
                .post()
                .uri("/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiDiagnosisResponse.class)
                .block();
    }
}
