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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

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
    private final boolean mlairAutoTriggerEnabled;
    private final String mlairDefaultPipelineId;

    public ContinuousTrainingClient(
        @Value("${vetai.diagnosis.url:http://localhost:8000}") String aiServiceUrl,
        @Value("${vetai.admin.token:}") String adminToken,
        @Value("${vetai.mlair.auto-trigger-enabled:false}") boolean mlairAutoTriggerEnabled,
        @Value("${vetai.mlair.pipeline-id:vet_ai_training_pipeline}") String mlairDefaultPipelineId
    ) {
        this.aiServiceUrl = aiServiceUrl;
        this.adminToken = adminToken;
        this.mlairAutoTriggerEnabled = mlairAutoTriggerEnabled;
        this.mlairDefaultPipelineId = mlairDefaultPipelineId;
        this.webClient = WebClient.builder().baseUrl(aiServiceUrl).build();
    }

    /**
     * Log prediction to continuous training system (upserts same id as vet-ai /predict when present).
     */
    public Mono<UUID> logPrediction(AiDiagnosisRequest request, AiDiagnosisResponse response, 
                                    Integer visitId, UUID petId, UUID veterinarianId) {
        UUID rowId = response.predictionId() != null ? response.predictionId() : UUID.randomUUID();

        Map<String, Object> predictionLog = new HashMap<>();
        predictionLog.put("id", rowId.toString());
        predictionLog.put("visit_id", visitId);
        UUID effectivePet = petId != null ? petId : request.petId();
        predictionLog.put(
            "pet_id",
            effectivePet != null ? effectivePet.toString() : "00000000-0000-0000-0000-000000000000");
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
        String effectiveClinic = null;
        if (response.clinicId() != null && !response.clinicId().isBlank()) {
            effectiveClinic = response.clinicId().trim();
        } else if (request.clinicId() != null) {
            effectiveClinic = request.clinicId().toString();
        }
        if (effectiveClinic != null) {
            inputMap.put("clinic_id", effectiveClinic);
        }

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
        predictionLog.put("veterinarian_id", veterinarianId != null ? veterinarianId.toString() : null);
        predictionLog.put("clinic_id", effectiveClinic);

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
     * Save doctor feedback for a prediction; returns vet-ai JSON (training_pool, auto_trigger_scope, …).
     */
    public Mono<Map<String, Object>> saveFeedback(UUID predictionId, String finalDiagnosis, boolean isCorrect,
                                  String aiDiagnosis,
                                  Integer confidenceRating, String comments, UUID veterinarianId,
                                  String trainingPoolOverride) {
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("prediction_id", predictionId.toString());
        feedback.put("final_diagnosis", finalDiagnosis);
        feedback.put("is_correct", isCorrect);

        if (trainingPoolOverride != null && !trainingPoolOverride.isBlank()) {
            feedback.put("training_pool", trainingPoolOverride.trim());
        }

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
        feedback.put("veterinarian_id", veterinarianId != null ? veterinarianId.toString() : null);
        feedback.put("is_training_eligible", true);
        feedback.put("data_quality_score", 1.0);

        logger.info("Sending feedback request: {}", feedback);

        return webClient
                .post()
                .uri("/continuous-training/feedback")
                .bodyValue(feedback)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
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
     * Check if system is eligible for training (global aggregate).
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> checkTrainingEligibility() {
        return checkTrainingEligibility(null);
    }

    /**
     * Check eligibility for global scope or for one clinic (matches vet-ai query param clinic_id).
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> checkTrainingEligibility(UUID clinicId) {
        String path = "/continuous-training/training/eligibility";
        if (clinicId != null) {
            path = path + "?clinic_id=" + clinicId;
        }
        return webClient
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .doOnSuccess(response -> logger.info("Training eligibility checked (clinicId={}): {}", clinicId, response))
                .doOnError(e -> logger.error("Failed to check training eligibility: {}", e.getMessage()));
    }

    /**
     * Trigger training job (global scope).
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> triggerTraining(String triggerType, String reason, boolean force) {
        return triggerTraining(triggerType, reason, force, null);
    }

    /**
     * Trigger training job, optionally scoped to a clinic (vet-ai body field clinic_id).
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> triggerTraining(String triggerType, String reason, boolean force, UUID clinicId) {
        Map<String, Object> request = new HashMap<>();
        request.put("trigger_type", triggerType);
        request.put("trigger_reason", reason);
        request.put("force", force);
        if (clinicId != null) {
            request.put("clinic_id", clinicId.toString());
        }

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
     * Trigger MLAir training run via vet-ai bridge endpoint.
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> triggerMlairTrainingRun(String pipelineId, String idempotencyKey, UUID clinicId) {
        Map<String, Object> request = new HashMap<>();
        request.put("pipeline_id", pipelineId);
        request.put("idempotency_key", idempotencyKey);
        if (clinicId != null) {
            request.put("clinic_id", clinicId.toString());
        }

        return webClient
                .post()
                .uri("/mlair/runs/training")
                .headers(h -> {
                    if (adminToken != null && !adminToken.isBlank()) {
                        h.setBearerAuth(adminToken);
                    }
                })
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .doOnSuccess(response -> logger.info("MLAir run triggered successfully: {}", response))
                .doOnError(e -> logger.error("Failed to trigger MLAir run: {}", e.getMessage()));
    }

    /**
     * Read MLAir run status via vet-ai bridge endpoint.
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getMlairRunStatus(String runId) {
        return webClient
                .get()
                .uri("/mlair/runs/{runId}", runId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .doOnSuccess(response -> logger.info("MLAir run status retrieved: {}", response))
                .doOnError(e -> logger.error("Failed to get MLAir run status: {}", e.getMessage()));
    }

    /**
     * Complete diagnosis workflow with training integration
     */
    public Mono<AiDiagnosisResponse> diagnoseWithTraining(AiDiagnosisRequest request, 
                                                         Integer visitId, UUID petId, 
                                                         UUID veterinarianId) {
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
                            predictionId,
                            response.clinicId()
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
                                                         UUID petId, UUID veterinarianId) {
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
                            predictionId,
                            response.clinicId()
                        );
                    });
        })
        .doOnSuccess((AiDiagnosisResponse response) -> logger.info("Diagnosis completed and logged for pet: {}, predictionId: {}", petId, response.predictionId()))
        .doOnError(e -> logger.error("Diagnosis failed for pet {}: {}", petId, e.getMessage()));
    }

    /**
     * Process final diagnosis with feedback and auto-training check
     */
    public Mono<Map<String, Object>> processFinalDiagnosis(UUID predictionId, String finalDiagnosis,
                                                          boolean isCorrect, String aiDiagnosis, Integer confidenceRating,
                                                          String comments, UUID veterinarianId, UUID clinicId,
                                                          String trainingPoolOverride) {
        // Eligibility/trigger scope follows vet-ai feedback row (GLOBAL vs CLINIC_ONLY), not only JWT clinic.
        return saveFeedback(predictionId, finalDiagnosis, isCorrect, aiDiagnosis, confidenceRating, comments,
                veterinarianId, trainingPoolOverride)
                .flatMap(feedbackResp -> {
                    String autoScope = feedbackResp.get("auto_trigger_scope") != null
                            ? String.valueOf(feedbackResp.get("auto_trigger_scope"))
                            : "clinic";
                    boolean useGlobal = "global".equalsIgnoreCase(autoScope);
                    UUID effectiveClinic = useGlobal ? null : clinicId;

                    return checkTrainingEligibility(effectiveClinic)
                            .flatMap(eligibility -> {
                                Boolean isEligible = (Boolean) eligibility.get("is_eligible_for_training");
                                if (Boolean.TRUE.equals(isEligible)) {
                                    logger.info(
                                            "Eligible for training (scope={}, effectiveClinic={}), triggering automatic training...",
                                            autoScope,
                                            effectiveClinic);
                                    return triggerTraining(
                                            "automatic",
                                            "Threshold reached: " + eligibility.get("eligible_feedback_count"),
                                            false,
                                            effectiveClinic)
                                            .flatMap(trigger -> {
                                                Map<String, Object> combined = new HashMap<>(trigger);
                                                combined.put("feedback_saved", feedbackResp);
                                                combined.put("training_scope_used", useGlobal ? "global" : "clinic");
                                                if (!mlairAutoTriggerEnabled) {
                                                    combined.put("mlair_auto_trigger", "disabled");
                                                    return Mono.just(combined);
                                                }

                                                String idempotencyKey = "vet-ai-auto-"
                                                        + predictionId
                                                        + "-"
                                                        + Instant.now().getEpochSecond();
                                                return triggerMlairTrainingRun(mlairDefaultPipelineId, idempotencyKey, effectiveClinic)
                                                        .map(mlairResp -> {
                                                            combined.put("mlair_auto_trigger", "triggered");
                                                            combined.put("mlair", mlairResp);
                                                            return combined;
                                                        })
                                                        .onErrorResume(ex -> {
                                                            logger.error("MLAir auto-trigger failed: {}", ex.getMessage());
                                                            combined.put("mlair_auto_trigger", "failed");
                                                            combined.put("mlair_error", ex.getMessage());
                                                            return Mono.just(combined);
                                                        });
                                            });
                                }
                                logger.info(
                                        "Not eligible for training (scope={}, effectiveClinic={}). Current: {}, Required: {}",
                                        autoScope,
                                        effectiveClinic,
                                        eligibility.get("eligible_feedback_count"),
                                        eligibility.get("training_threshold"));
                                Map<String, Object> body = new HashMap<>();
                                body.put("status", "not_eligible");
                                body.put("eligibility", eligibility);
                                body.put("feedback_saved", feedbackResp);
                                body.put("training_scope_used", useGlobal ? "global" : "clinic");
                                return Mono.just(body);
                            });
                })
                .doOnSuccess(result -> logger.info("Final diagnosis processing completed: {}", result))
                .doOnError(e -> logger.error("Failed to process final diagnosis: {}", e.getMessage()));
    }
}
