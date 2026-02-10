package org.springframework.samples.petclinic.genai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisRequest;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiDiagnosisClient {

     private static final Logger logger = LoggerFactory.getLogger(AiDiagnosisClient.class);
    private final WebClient webClient;

    public AiDiagnosisClient(@Value("${vetai.diagnosis.url:http://localhost:8000}") String aiServiceUrl) {
        this.webClient = WebClient.builder().baseUrl(aiServiceUrl).build();
    }

    public AiDiagnosisResponse predict(AiDiagnosisRequest request) {
        logger.info("Sending AI diagnosis request: {}", request);
        
        try {
            return webClient
                    .post()
                    .uri("/predict")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiDiagnosisResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            logger.error("FastAPI error response - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // If FastAPI returns 422, propagate it properly instead of converting to 500
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                String responseBody = e.getResponseBodyAsString();
                logger.error("FastAPI 422 validation error: {}", responseBody);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                    "FastAPI validation error: " + responseBody, e);
            }
            
            // For other 4xx errors, also preserve the original status
            if (e.getStatusCode().is4xxClientError()) {
                String responseBody = e.getResponseBodyAsString();
                logger.error("FastAPI client error - Status: {}, Body: {}", e.getStatusCode(), responseBody);
                throw new ResponseStatusException(e.getStatusCode(), 
                    "FastAPI client error: " + responseBody, e);
            }
            
            // For 5xx errors, log and rethrow
            logger.error("FastAPI server error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "FastAPI service error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error calling FastAPI: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Unexpected error calling AI service: " + e.getMessage(), e);
        }
    }
}
