package org.springframework.samples.petclinic.genai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisRequest;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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

    public ContinuousTrainingClient(@Value("${vetai.diagnosis.url:http://localhost:8000}") String aiServiceUrl) {
        this.aiServiceUrl = aiServiceUrl;
        this.webClient = WebClient.builder().baseUrl(aiServiceUrl).build();
    }

    /**
     * Log prediction to continuous training system
     */
    public Mono<Void> logPrediction(AiDiagnosisRequest request, AiDiagnosisResponse response, 
                                    Integer visitId, Integer petId, Integer veterinarianId) {
        Map<String, Object> predictionLog = new HashMap<>();
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
        predictionLog.put("model_version", response.modelVersion());
        predictionLog.put("confidence_score", response.confidence());
        predictionLog.put("top_k_predictions", response.top_k());
        predictionLog.put("veterinarian_id", veterinarianId);
        predictionLog.put("clinic_id", 1); // Default clinic ID

        return webClient
                .post()
                .uri("/continuous-training/predictions/log")
                .bodyValue(predictionLog)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> logger.info("Prediction logged successfully for visit: {}", visitId))
                .doOnError(e -> logger.error("Failed to log prediction: {}", e.getMessage()));
    }

    /**
     * Save doctor feedback for a prediction
     */
    public Mono<Void> saveFeedback(Integer predictionId, String finalDiagnosis, boolean isCorrect,
                                  Integer confidenceRating, String comments, Integer veterinarianId) {
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("prediction_id", predictionId);
        feedback.put("final_diagnosis", finalDiagnosis);
        feedback.put("is_correct", isCorrect);
        feedback.put("confidence_rating", confidenceRating);
        feedback.put("comments", comments);
        feedback.put("veterinarian_id", veterinarianId);
        feedback.put("is_training_eligible", true);
        feedback.put("data_quality_score", 1.0);

        return webClient
                .post()
                .uri("/continuous-training/feedback")
                .bodyValue(feedback)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> logger.info("Feedback saved successfully for prediction: {}", predictionId))
                .doOnError(e -> logger.error("Failed to save feedback: {}", e.getMessage()));
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
                    .thenReturn(response);
        })
        .doOnSuccess(response -> logger.info("Diagnosis completed and logged for visit: {}", visitId))
        .doOnError(e -> logger.error("Diagnosis failed for visit {}: {}", visitId, e.getMessage()));
    }

    /**
     * Process final diagnosis with feedback and auto-training check
     */
    public Mono<Map<String, Object>> processFinalDiagnosis(Integer predictionId, String finalDiagnosis,
                                                          boolean isCorrect, Integer confidenceRating,
                                                          String comments, Integer veterinarianId) {
        // 1. Save feedback
        return saveFeedback(predictionId, finalDiagnosis, isCorrect, confidenceRating, comments, veterinarianId)
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
