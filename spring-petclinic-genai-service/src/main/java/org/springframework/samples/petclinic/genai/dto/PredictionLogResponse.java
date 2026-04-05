package org.springframework.samples.petclinic.genai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class PredictionLogResponse {
    @JsonProperty("predictionId")
    private UUID predictionId;
    
    @JsonProperty("status")
    private String status;
    
    public PredictionLogResponse() {}
    
    public PredictionLogResponse(UUID predictionId, String status) {
        this.predictionId = predictionId;
        this.status = status;
    }
    
    public UUID getPredictionId() {
        return predictionId;
    }
    
    public void setPredictionId(UUID predictionId) {
        this.predictionId = predictionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
