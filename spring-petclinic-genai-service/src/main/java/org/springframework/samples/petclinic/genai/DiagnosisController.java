package org.springframework.samples.petclinic.genai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.genai.auth.ClinicAuthJwtSupport;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisRequest;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/")
public class DiagnosisController {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosisController.class);
    
    private final AiDiagnosisClient aiDiagnosisClient;
    private final ContinuousTrainingClient trainingClient;
    private final ScheduledTrainingService scheduledTrainingService;
    private final ClinicAuthJwtSupport clinicAuthJwt;

    public DiagnosisController(AiDiagnosisClient aiDiagnosisClient, 
                              ContinuousTrainingClient trainingClient,
                              ScheduledTrainingService scheduledTrainingService,
                              ClinicAuthJwtSupport clinicAuthJwt) {
        this.aiDiagnosisClient = aiDiagnosisClient;
        this.trainingClient = trainingClient;
        this.scheduledTrainingService = scheduledTrainingService;
        this.clinicAuthJwt = clinicAuthJwt;
    }

    /** When JSON body has no clinicId, use the same claim as customers-service JWT (clinic login). */
    private AiDiagnosisRequest mergeClinicFromJwt(AiDiagnosisRequest r, String authorization) {
        if (r.clinicId() != null) {
            return r;
        }
        Optional<UUID> fromJwt = clinicAuthJwt.readClinicId(authorization);
        if (fromJwt.isEmpty()) {
            logger.warn(
                "AiDiagnosisRequest has clinicId=null and JWT has no clinicId — prediction will not be scoped to a clinic. "
                    + "Ensure Authorization Bearer is sent (gateway) and JWT_SECRET matches customers-service.");
            return r;
        }
        logger.info("Resolved clinicId from JWT for diagnosis: {}", fromJwt.get());
        return new AiDiagnosisRequest(
            r.animal_type(),
            r.gender(),
            r.age_months(),
            r.weight_kg(),
            r.temperature(),
            r.heart_rate(),
            r.current_season(),
            r.vaccination_status(),
            r.medical_history(),
            r.symptoms_list(),
            r.symptom_duration(),
            fromJwt.get(),
            r.petId(),
            r.visitId());
    }

    @PostMapping("/diagnosis")
    public Mono<AiDiagnosisResponse> diagnosis(@RequestBody AiDiagnosisRequest request,
                                               @RequestParam(required = false) Integer visitId,
                                               @RequestParam(required = false) UUID petId,
                                               @RequestParam(required = false) UUID veterinarianId,
                                               @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        
        logger.info("Received diagnosis request for visit: {}, pet: {}, vet: {}", 
                   visitId, petId, veterinarianId);

        final AiDiagnosisRequest ctx = withVisitContext(mergeClinicFromJwt(request, authorization), visitId, petId);
        
        if (petId != null && veterinarianId != null && visitId != null) {
            // Full training integration with visit
            return trainingClient.diagnoseWithTraining(ctx, visitId, petId, veterinarianId);
        } else if (petId != null && veterinarianId != null) {
            // Simple diagnosis without visit - still log for training
            return trainingClient.diagnoseWithoutVisit(ctx, petId, veterinarianId);
        } else {
            // Simple diagnosis without training (no predictionId)
            return Mono.fromCallable(() -> aiDiagnosisClient.predict(ctx));
        }
    }

    private static AiDiagnosisRequest withVisitContext(AiDiagnosisRequest r, Integer visitId, UUID petId) {
        return new AiDiagnosisRequest(
            r.animal_type(),
            r.gender(),
            r.age_months(),
            r.weight_kg(),
            r.temperature(),
            r.heart_rate(),
            r.current_season(),
            r.vaccination_status(),
            r.medical_history(),
            r.symptoms_list(),
            r.symptom_duration(),
            r.clinicId(),
            petId != null ? petId : r.petId(),
            visitId != null ? visitId : r.visitId()
        );
    }

    @PostMapping("/diagnosis/{predictionId}/feedback")
    public Mono<Map<String, Object>> saveFeedback(@PathVariable UUID predictionId,
                                                 @RequestBody FeedbackRequest feedback,
                                                 @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("Saving feedback for prediction: {}", predictionId);

        // FastAPI expects `final_diagnosis` as a non-null string.
        // If the UI sends null (e.g., reject without selecting Target Diagnosis),
        // fail fast here with a clearer error.
        if (feedback == null || feedback.getFinalDiagnosis() == null || feedback.getFinalDiagnosis().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "finalDiagnosis is required (select Target Diagnosis before reject)");
        }

        UUID clinicForTraining = feedback.getClinicId();
        if (clinicForTraining == null) {
            clinicForTraining = clinicAuthJwt.readClinicId(authorization).orElse(null);
        }
        if (clinicForTraining != null) {
            logger.info("Feedback scoped to clinicId={} (body or JWT)", clinicForTraining);
        }
        
        return trainingClient.processFinalDiagnosis(
            predictionId,
            feedback.getFinalDiagnosis(),
            feedback.isCorrect(),
            feedback.getAiDiagnosis(),
            feedback.getConfidenceRating(),
            feedback.getComments(),
            feedback.getVeterinarianId(),
            clinicForTraining,
            feedback.getTrainingPool()
        );
    }

    @GetMapping("/training/eligibility")
    public Mono<Map<String, Object>> checkTrainingEligibility() {
        return trainingClient.checkTrainingEligibility();
    }

    @PostMapping("/training/trigger")
    public Mono<Map<String, Object>> triggerTraining(@RequestBody TrainingRequest request) {
        return trainingClient.triggerTraining(
            request.getTriggerType(), 
            request.getReason(), 
            request.isForce()
        );
    }

    @GetMapping("/training/status/{trainingId}")
    public Mono<Map<String, Object>> getTrainingStatus(@PathVariable Integer trainingId) {
        return trainingClient.getTrainingStatus(trainingId);
    }

    @PostMapping("/training/manual-check")
    public Mono<Map<String, Object>> manualTrainingCheck() {
        logger.info("Manual training check triggered");
        return scheduledTrainingService.manualTrainingCheck();
    }

    // DTOs for request bodies
    public static class FeedbackRequest {
        private String finalDiagnosis;
        private String aiDiagnosis;
        private boolean isCorrect;
        private int confidenceRating;
        private String comments;
        private UUID veterinarianId;
        /** When set, automatic training eligibility/trigger is scoped to this clinic (vet-ai clinic_id). */
        private UUID clinicId;
        /** Optional override: GLOBAL or CLINIC_ONLY (vet-ai DoctorFeedback.training_pool). */
        private String trainingPool;

        // Getters and setters
        public String getFinalDiagnosis() { return finalDiagnosis; }
        public void setFinalDiagnosis(String finalDiagnosis) { this.finalDiagnosis = finalDiagnosis; }

        public String getAiDiagnosis() { return aiDiagnosis; }
        public void setAiDiagnosis(String aiDiagnosis) { this.aiDiagnosis = aiDiagnosis; }
        
        public boolean isCorrect() { return isCorrect; }
        public void setCorrect(boolean correct) { isCorrect = correct; }
        
        public int getConfidenceRating() { return confidenceRating; }
        public void setConfidenceRating(int confidenceRating) { this.confidenceRating = confidenceRating; }
        
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        
        public UUID getVeterinarianId() { return veterinarianId; }
        public void setVeterinarianId(UUID veterinarianId) { this.veterinarianId = veterinarianId; }

        public UUID getClinicId() { return clinicId; }
        public void setClinicId(UUID clinicId) { this.clinicId = clinicId; }

        public String getTrainingPool() { return trainingPool; }
        public void setTrainingPool(String trainingPool) { this.trainingPool = trainingPool; }
    }

    public static class TrainingRequest {
        private String triggerType;
        private String reason;
        private boolean force;

        // Getters and setters
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public boolean isForce() { return force; }
        public void setForce(boolean force) { this.force = force; }
    }

    // Exception handlers to return consistent JSON error format
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        logger.error("ResponseStatusException: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = Map.of(
            "error", ex.getReason(),
            "message", ex.getMessage(),
            "status", ex.getStatusCode().value()
        );
        
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = Map.of(
            "error", "Internal Server Error",
            "message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
