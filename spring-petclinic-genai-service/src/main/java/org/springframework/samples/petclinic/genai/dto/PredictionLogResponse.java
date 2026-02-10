package org.springframework.samples.petclinic.genai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionLogResponse {
    @JsonProperty("predictionId")
    private Long predictionId;
    
    @JsonProperty("status")
    private String status;
    
    // Constructors
    public PredictionLogResponse() {}
    
    public PredictionLogResponse(Long predictionId, String status) {
        this.predictionId = predictionId;
        this.status = status;
    }
    
    // Getters and setters
    public Long getPredictionId() {
        return predictionId;
    }
    
    public void setPredictionId(Long predictionId) {
        this.predictionId = predictionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
