package org.springframework.samples.petclinic.genai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisRequest;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/")
public class DiagnosisController {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosisController.class);
    
    private final AiDiagnosisClient aiDiagnosisClient;
    private final ContinuousTrainingClient trainingClient;
    private final ScheduledTrainingService scheduledTrainingService;

    public DiagnosisController(AiDiagnosisClient aiDiagnosisClient, 
                              ContinuousTrainingClient trainingClient,
                              ScheduledTrainingService scheduledTrainingService) {
        this.aiDiagnosisClient = aiDiagnosisClient;
        this.trainingClient = trainingClient;
        this.scheduledTrainingService = scheduledTrainingService;
    }

    @PostMapping("/diagnosis")
    public Mono<AiDiagnosisResponse> diagnosis(@RequestBody AiDiagnosisRequest request,
                                               @RequestParam(required = false) Integer visitId,
                                               @RequestParam(required = false) Integer petId,
                                               @RequestParam(required = false) Integer veterinarianId) {
        
        logger.info("Received diagnosis request for visit: {}, pet: {}, vet: {}", 
                   visitId, petId, veterinarianId);
        
        if (petId != null && veterinarianId != null && visitId != null) {
            // Full training integration with visit
            return trainingClient.diagnoseWithTraining(request, visitId, petId, veterinarianId);
        } else if (petId != null && veterinarianId != null) {
            // Simple diagnosis without visit - still log for training
            return trainingClient.diagnoseWithoutVisit(request, petId, veterinarianId);
        } else {
            // Simple diagnosis without training (no predictionId)
            return Mono.fromCallable(() -> aiDiagnosisClient.predict(request));
        }
    }

    @PostMapping("/diagnosis/{predictionId}/feedback")
    public Mono<Map<String, Object>> saveFeedback(@PathVariable Long predictionId,
                                                 @RequestBody FeedbackRequest feedback) {
        logger.info("Saving feedback for prediction: {}", predictionId);
        
        return trainingClient.processFinalDiagnosis(
            predictionId,
            feedback.getFinalDiagnosis(),
            feedback.isCorrect(),
            feedback.getConfidenceRating(),
            feedback.getComments(),
            feedback.getVeterinarianId()
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
        private boolean isCorrect;
        private int confidenceRating;
        private String comments;
        private int veterinarianId;

        // Getters and setters
        public String getFinalDiagnosis() { return finalDiagnosis; }
        public void setFinalDiagnosis(String finalDiagnosis) { this.finalDiagnosis = finalDiagnosis; }
        
        public boolean isCorrect() { return isCorrect; }
        public void setCorrect(boolean correct) { isCorrect = correct; }
        
        public int getConfidenceRating() { return confidenceRating; }
        public void setConfidenceRating(int confidenceRating) { this.confidenceRating = confidenceRating; }
        
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        
        public int getVeterinarianId() { return veterinarianId; }
        public void setVeterinarianId(int veterinarianId) { this.veterinarianId = veterinarianId; }
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
