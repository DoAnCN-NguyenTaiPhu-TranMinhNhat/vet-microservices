/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.visits.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Simple JavaBean domain object representing a visit.
 *
 * @author Ken Krebs
 * @author Maciej Szarlinski
 * @author Ramazan Sakin
 */
@Entity
@Table(name = "visits")
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "visit_date")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date date = new Date();

    @Size(max = 8192)
    @Column(name = "description")
    private String description;

    @Column(name = "pet_id")
    private int petId;

    // Medical data fields
    @Column(name = "temperature", precision = 4, scale = 1)
    @DecimalMin(value = "35.0", message = "Temperature must be at least 35.0°C")
    @DecimalMax(value = "43.0", message = "Temperature must be at most 43.0°C")
    private BigDecimal temperature;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
    @DecimalMax(value = "100.0", message = "Weight must be at most 100.0 kg")
    private BigDecimal weightKg;

    @Size(max = 5000)
    @Column(name = "symptoms_list", length = 5000)
    private String symptomsList;

    @Column(name = "heart_rate")
    @Min(value = 40, message = "Heart rate must be at least 40 bpm")
    private Integer heartRate;

    @Column(name = "symptom_duration")
    @Min(value = 0, message = "Symptom duration must be non-negative")
    private Integer symptomDuration;

    @Size(max = 100)
    @Column(name = "target_diagnosis", length = 100)
    private String targetDiagnosis;

    public Integer getId() {
        return this.id;
    }

    public Date getDate() {
        return this.date;
    }

    public String getDescription() {
        return this.description;
    }

    public int getPetId() {
        return this.petId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPetId(int petId) {
        this.petId = petId;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public String getSymptomsList() {
        return symptomsList;
    }

    public void setSymptomsList(String symptomsList) {
        this.symptomsList = symptomsList;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public Integer getSymptomDuration() {
        return symptomDuration;
    }

    public void setSymptomDuration(Integer symptomDuration) {
        this.symptomDuration = symptomDuration;
    }

    public String getTargetDiagnosis() {
        return targetDiagnosis;
    }

    public void setTargetDiagnosis(String targetDiagnosis) {
        this.targetDiagnosis = targetDiagnosis;
    }

    public static final class VisitBuilder {
        private Integer id;
        private Date date;
        private @Size(max = 8192) String description;
        private int petId;
        private BigDecimal temperature;
        private BigDecimal weightKg;
        private String symptomsList;
        private Integer heartRate;
        private Integer symptomDuration;
        private String targetDiagnosis;

        private VisitBuilder() {
        }

        public static VisitBuilder aVisit() {
            return new VisitBuilder();
        }

        public VisitBuilder id(Integer id) {
            this.id = id;
            return this;
        }

        public VisitBuilder date(Date date) {
            this.date = date;
            return this;
        }

        public VisitBuilder description(String description) {
            this.description = description;
            return this;
        }

        public VisitBuilder petId(int petId) {
            this.petId = petId;
            return this;
        }

        public VisitBuilder temperature(BigDecimal temperature) {
            this.temperature = temperature;
            return this;
        }

        public VisitBuilder weightKg(BigDecimal weightKg) {
            this.weightKg = weightKg;
            return this;
        }

        public VisitBuilder symptomsList(String symptomsList) {
            this.symptomsList = symptomsList;
            return this;
        }

        public VisitBuilder heartRate(Integer heartRate) {
            this.heartRate = heartRate;
            return this;
        }

        public VisitBuilder symptomDuration(Integer symptomDuration) {
            this.symptomDuration = symptomDuration;
            return this;
        }

        public VisitBuilder targetDiagnosis(String targetDiagnosis) {
            this.targetDiagnosis = targetDiagnosis;
            return this;
        }

        public Visit build() {
            Visit visit = new Visit();
            visit.setId(id);
            visit.setDate(date);
            visit.setDescription(description);
            visit.setPetId(petId);
            visit.setTemperature(temperature);
            visit.setWeightKg(weightKg);
            visit.setSymptomsList(symptomsList);
            visit.setHeartRate(heartRate);
            visit.setSymptomDuration(symptomDuration);
            visit.setTargetDiagnosis(targetDiagnosis);
            return visit;
        }
    }
}
