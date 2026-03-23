package org.springframework.samples.petclinic.genai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisRequest;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisResponse;
import org.springframework.samples.petclinic.genai.dto.PredictionLogResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for integrating with Continuous Training API
 * Handles prediction logging, feedback collection, and training triggers
 */
@Service
public class ContinuousTrainingClient {

    private static final Logger logger = LoggerFactory.getLogger(ContinuousTrainingClient.class);
    
    private final WebClient webClient;
    private final String aiServiceUrl;
    private final String adminToken;

    public ContinuousTrainingClient(
        @Value("${vetai.diagnosis.url:http://localhost:8000}") String aiServiceUrl,
        @Value("${vetai.admin.token:}") String adminToken
    ) {
        this.aiServiceUrl = aiServiceUrl;
        this.adminToken = adminToken;
        this.webClient = WebClient.builder().baseUrl(aiServiceUrl).build();
    }

    /**
     * Log prediction to continuous training system
     */
    public Mono<Long> logPrediction(AiDiagnosisRequest request, AiDiagnosisResponse response, 
                                    Integer visitId, Integer petId, Integer veterinarianId) {
        // Generate unique ID using timestamp and random
        long predictionId = System.currentTimeMillis() + (long)(Math.random() * 1000);
        
        Map<String, Object> predictionLog = new HashMap<>();
        predictionLog.put("id", predictionId);
        predictionLog.put("visit_id", visitId);
        predictionLog.put("pet_id", petId);
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("animal_type", request.animal_type());
        inputMap.put("gender", request.gender());
        inputMap.put("age_months", request.age_months());
        inputMap.put("weight_kg", request.weight_kg());
        inputMap.put("temperature", request.temperature());
        inputMap.put("heart_rate", request.heart_rate());
        inputMap.put("current_season", request.current_season());
        inputMap.put("vaccination_status", request.vaccination_status());
        inputMap.put("medical_history", request.medical_history());
        inputMap.put("symptoms_list", request.symptoms_list());
        inputMap.put("symptom_duration", request.symptom_duration());
        
        Map<String, Object> outputMap = new HashMap<>();
        outputMap.put("diagnosis", response.diagnosis());
        outputMap.put("confidence", response.confidence());
        outputMap.put("top_k", response.top_k());
        
        predictionLog.put("prediction_input", inputMap);
        predictionLog.put("prediction_output", outputMap);
        predictionLog.put("model_version", response.modelVersion() != null ? response.modelVersion() : "v2.0");
        
        // Handle null confidence - default to 0.0 for validation
        Double confidence = response.confidence();
        predictionLog.put("confidence_score", confidence != null ? confidence.floatValue() : 0.0f);
        
        // Handle null top_k - default to empty list
        predictionLog.put("top_k_predictions", response.top_k() != null ? response.top_k() : List.of());
        predictionLog.put("veterinarian_id", veterinarianId);
        predictionLog.put("clinic_id", 1); // Default clinic ID

        logger.info("Sending prediction log to FastAPI: {}", predictionLog);

        return webClient
                .post()
                .uri("/continuous-training/predictions/log")
                .bodyValue(predictionLog)
                .retrieve()
                .bodyToMono(PredictionLogResponse.class)
                .map(logResponse -> logResponse.getPredictionId())
                .doOnSuccess(loggedPredictionId -> logger.info("Prediction logged successfully for visit: {}, predictionId: {}", visitId, loggedPredictionId))
                .doOnError(e -> {
                    logger.error("Failed to log prediction: {}", e.getMessage());
                    if (e instanceof WebClientResponseException) {
                        WebClientResponseException webEx = (WebClientResponseException) e;
                        logger.error("FastAPI error response - Status: {}, Body: {}", 
                                   webEx.getStatusCode(), webEx.getResponseBodyAsString());
                    }
                });
    }

    /**
     * Save doctor feedback for a prediction
     */
    public Mono<Void> saveFeedback(Long predictionId, String finalDiagnosis, boolean isCorrect,
                                  String aiDiagnosis,
                                  Integer confidenceRating, String comments, Integer veterinarianId) {
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("prediction_id", predictionId);
        feedback.put("final_diagnosis", finalDiagnosis);
        feedback.put("is_correct", isCorrect);

        if (aiDiagnosis != null && !aiDiagnosis.isBlank()) {
            feedback.put("ai_diagnosis", aiDiagnosis);
        }
        
        // Validate confidence_rating - FastAPI requires 1-5, not 0-5
        if (confidenceRating != null) {
            if (confidenceRating < 1) {
                logger.warn("Confidence rating {} is too low, setting to 1 (minimum allowed)", confidenceRating);
                confidenceRating = 1;
            } else if (confidenceRating > 5) {
                logger.warn("Confidence rating {} is too high, setting to 5 (maximum allowed)", confidenceRating);
                confidenceRating = 5;
            }
            feedback.put("confidence_rating", confidenceRating);
            logger.info("Including validated confidence rating: {}", confidenceRating);
        } else {
            logger.info("No confidence rating provided - will use default null");
        }
        
        feedback.put("comments", comments);
        feedback.put("veterinarian_id", veterinarianId);
        feedback.put("is_training_eligible", true);
        feedback.put("data_quality_score", 1.0);

        logger.info("Sending feedback request: {}", feedback);

        return webClient
                .post()
                .uri("/continuous-training/feedback")
                .bodyValue(feedback)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> logger.info("Feedback saved successfully for prediction: {}", predictionId))
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException) {
                        WebClientResponseException webEx = (WebClientResponseException) e;
                        logger.error("FastAPI feedback error - Status: {}, Body: {}", 
                                   webEx.getStatusCode(), webEx.getResponseBodyAsString());
                        
                        if (webEx.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                            logger.error("FastAPI 422 validation error in feedback: {}", webEx.getResponseBodyAsString());
                        }
                    } else {
                        logger.error("Failed to save feedback: {}", e.getMessage(), e);
                    }
                })
                .onErrorMap(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                        String responseBody = e.getResponseBodyAsString();
                        logger.error("FastAPI 422 validation error in feedback: {}", responseBody);
                        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                            "FastAPI feedback validation error: " + responseBody, e);
                    }
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Failed to save feedback: " + e.getMessage(), e);
                });
    }

    /**
     * Check if system is eligible for training
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> checkTrainingEligibility() {
        return webClient
                .get()
                .uri("/continuous-training/training/eligibility")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .doOnSuccess(response -> logger.info("Training eligibility checked: {}", response))
                .doOnError(e -> logger.error("Failed to check training eligibility: {}", e.getMessage()));
    }

    /**
     * Trigger training job
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> triggerTraining(String triggerType, String reason, boolean force) {
        Map<String, Object> request = new HashMap<>();
        request.put("trigger_type", triggerType);
        request.put("trigger_reason", reason);
        request.put("force", force);

        return webClient
                .post()
                .uri("/continuous-training/training/trigger")
                .headers(h -> {
                    if (adminToken != null && !adminToken.isBlank()) {
                        h.setBearerAuth(adminToken);
                    }
                })
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .doOnSuccess(response -> logger.info("Training triggered successfully: {}", response))
                .doOnError(e -> logger.error("Failed to trigger training: {}", e.getMessage()));
    }

    /**
     * Get training status
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getTrainingStatus(Integer trainingId) {
        return webClient
                .get()
                .uri("/continuous-training/training/status?training_id={trainingId}", trainingId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .doOnSuccess(response -> logger.info("Training status retrieved: {}", response))
                .doOnError(e -> logger.error("Failed to get training status: {}", e.getMessage()));
    }

    /**
     * Complete diagnosis workflow with training integration
     */
    public Mono<AiDiagnosisResponse> diagnoseWithTraining(AiDiagnosisRequest request, 
                                                         Integer visitId, Integer petId, 
                                                         Integer veterinarianId) {
        // 1. Get AI diagnosis
        return Mono.fromCallable(() -> {
            AiDiagnosisClient aiClient = new AiDiagnosisClient(aiServiceUrl);
            return aiClient.predict(request);
        })
        .flatMap(response -> {
            // 2. Log prediction for training
            return logPrediction(request, response, visitId, petId, veterinarianId)
                    .map(predictionId -> {
                        // Create new response with predictionId
                        return new AiDiagnosisResponse(
                            response.diagnosis(),
                            response.confidence(),
                            response.top_k(),
                            response.modelVersion(),
                            response.predictions(),
                            predictionId
                        );
                    });
        })
        .doOnSuccess((AiDiagnosisResponse response) -> logger.info("Diagnosis completed and logged for visit: {}, predictionId: {}", visitId, response.predictionId()))
        .doOnError(e -> logger.error("Diagnosis failed for visit {}: {}", visitId, e.getMessage()));
    }

    /**
     * Diagnosis without visit - still log for training with null visitId
     */
    public Mono<AiDiagnosisResponse> diagnoseWithoutVisit(AiDiagnosisRequest request, 
                                                         Integer petId, Integer veterinarianId) {
        // 1. Get AI diagnosis
        return Mono.fromCallable(() -> {
            AiDiagnosisClient aiClient = new AiDiagnosisClient(aiServiceUrl);
            return aiClient.predict(request);
        })
        .flatMap(response -> {
            // 2. Log prediction for training with null visitId
            return logPrediction(request, response, null, petId, veterinarianId)
                    .map(predictionId -> {
                        // Create new response with predictionId
                        return new AiDiagnosisResponse(
                            response.diagnosis(),
                            response.confidence(),
                            response.top_k(),
                            response.modelVersion(),
                            response.predictions(),
                            predictionId
                        );
                    });
        })
        .doOnSuccess((AiDiagnosisResponse response) -> logger.info("Diagnosis completed and logged for pet: {}, predictionId: {}", petId, response.predictionId()))
        .doOnError(e -> logger.error("Diagnosis failed for pet {}: {}", petId, e.getMessage()));
    }

    /**
     * Process final diagnosis with feedback and auto-training check
     */
    public Mono<Map<String, Object>> processFinalDiagnosis(Long predictionId, String finalDiagnosis,
                                                          boolean isCorrect, String aiDiagnosis, Integer confidenceRating,
                                                          String comments, Integer veterinarianId) {
        // 1. Save feedback
        return saveFeedback(predictionId, finalDiagnosis, isCorrect, aiDiagnosis, confidenceRating, comments, veterinarianId)
                .then(checkTrainingEligibility())
                .flatMap(eligibility -> {
                    Boolean isEligible = (Boolean) eligibility.get("is_eligible_for_training");
                    if (isEligible) {
                        logger.info("System eligible for training, triggering automatic training...");
                        return triggerTraining("automatic", "Threshold reached: " + eligibility.get("eligible_feedback_count"), false);
                    } else {
                        logger.info("System not eligible for training. Current: {}, Required: {}", 
                                eligibility.get("eligible_feedback_count"), eligibility.get("training_threshold"));
                        return Mono.just(Map.of("status", "not_eligible", "eligibility", eligibility));
                    }
                })
                .doOnSuccess(result -> logger.info("Final diagnosis processing completed: {}", result))
                .doOnError(e -> logger.error("Failed to process final diagnosis: {}", e.getMessage()));
    }
}
