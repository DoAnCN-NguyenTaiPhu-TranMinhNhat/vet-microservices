package org.springframework.samples.petclinic.genai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Scheduled service for automatic training checks and triggers
 */
@Service
public class ScheduledTrainingService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTrainingService.class);
    
    private final ContinuousTrainingClient trainingClient;

    public ScheduledTrainingService(ContinuousTrainingClient trainingClient) {
        this.trainingClient = trainingClient;
    }

    /**
     * Check training eligibility every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void scheduledTrainingCheck() {
        logger.info("Running scheduled training eligibility check at {}", LocalDateTime.now());
        
        trainingClient.checkTrainingEligibility()
                .flatMap(eligibility -> {
                    Boolean isEligible = (Boolean) eligibility.get("is_eligible_for_training");
                    Integer eligibleCount = (Integer) eligibility.get("eligible_feedback_count");
                    Integer threshold = (Integer) eligibility.get("training_threshold");
                    
                    if (isEligible) {
                        logger.info("System eligible for scheduled training. Eligible count: {}, Threshold: {}", 
                                   eligibleCount, threshold);
                        return triggerScheduledTraining("scheduled", "Hourly check - threshold reached");
                    } else {
                        logger.info("System not eligible for scheduled training. Eligible count: {}, Threshold: {}", 
                                   eligibleCount, threshold);
                        return Mono.just(Map.of("status", "not_eligible", "check_time", LocalDateTime.now()));
                    }
                })
                .subscribe(
                    result -> logger.info("Scheduled training check completed: {}", result),
                    error -> logger.error("Scheduled training check failed: {}", error.getMessage())
                );
    }

    /**
     * Daily training trigger at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2:00 AM every day
    public void dailyTrainingTrigger() {
        logger.info("Running daily training trigger at {}", LocalDateTime.now());
        
        triggerScheduledTraining("scheduled_daily", "Daily scheduled training at 2 AM")
                .subscribe(
                    result -> logger.info("Daily training trigger completed: {}", result),
                    error -> logger.error("Daily training trigger failed: {}", error.getMessage())
                );
    }

    /**
     * Weekly comprehensive training check on Sundays at 3 AM
     */
    @Scheduled(cron = "0 0 3 ? * SUN") // 3:00 AM every Sunday
    public void weeklyTrainingCheck() {
        logger.info("Running weekly comprehensive training check at {}", LocalDateTime.now());
        
        triggerScheduledTraining("scheduled_weekly", "Weekly comprehensive training check")
                .subscribe(
                    result -> logger.info("Weekly training check completed: {}", result),
                    error -> logger.error("Weekly training check failed: {}", error.getMessage())
                );
    }

    /**
     * Helper method to trigger scheduled training
     */
    private Mono<Map<String, Object>> triggerScheduledTraining(String triggerType, String reason) {
        return trainingClient.triggerTraining(triggerType, reason, false)
                .doOnSuccess(result -> logger.info("Scheduled training triggered successfully: {}", result))
                .doOnError(error -> logger.error("Failed to trigger scheduled training: {}", error.getMessage()));
    }

    /**
     * Manual trigger for testing
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> manualTrainingCheck() {
        logger.info("Manual training check triggered at {}", LocalDateTime.now());
        
        return trainingClient.checkTrainingEligibility()
                .flatMap(eligibility -> {
                    Boolean isEligible = (Boolean) eligibility.get("is_eligible_for_training");
                    Integer eligibleCount = (Integer) eligibility.get("eligible_feedback_count");
                    Integer threshold = (Integer) eligibility.get("training_threshold");
                    
                    if (isEligible) {
                        logger.info("System eligible for manual training. Eligible count: {}, Threshold: {}", 
                                   eligibleCount, threshold);
                        return triggerScheduledTraining("manual", "Manual check - threshold reached");
                    } else {
                        logger.info("System not eligible for manual training. Eligible count: {}, Threshold: {}", 
                                   eligibleCount, threshold);
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", "not_eligible");
                        result.put("check_time", LocalDateTime.now());
                        return Mono.just(result);
                    }
                })
                .doOnSuccess(result -> logger.info("Manual training check completed: {}", result))
                .doOnError(error -> logger.error("Manual training check failed: {}", error.getMessage()));
    }
}
