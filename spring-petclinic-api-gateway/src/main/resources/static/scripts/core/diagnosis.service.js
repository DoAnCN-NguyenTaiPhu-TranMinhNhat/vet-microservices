'use strict';

angular.module('core')
    .service('DiagnosisService', ['$http', '$q', 'ApiConfig', function($http, $q, ApiConfig) {
        var self = this;

        /**
         * Clinic UUID for training scope: stored user object, then JWT claim (customers-service always sets clinicId on tokens).
         * Fixes feedback counting as "global" when vet_clinic_user is stale or uses clinic_id (snake_case).
         */
        self.readClinicIdForAi = function() {
            try {
                var u = JSON.parse(localStorage.getItem('vet_clinic_user') || '{}');
                var v = u.clinicId != null && u.clinicId !== '' ? u.clinicId : u.clinic_id;
                if (v != null && String(v).trim() !== '') {
                    return String(v).trim();
                }
            } catch (e1) { /* ignore */ }
            try {
                var t = localStorage.getItem('vet_clinic_jwt');
                if (!t) {
                    return null;
                }
                var parts = t.split('.');
                if (parts.length < 2) {
                    return null;
                }
                var b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
                while (b64.length % 4) {
                    b64 += '=';
                }
                var p = JSON.parse(atob(b64));
                if (p.clinicId != null && String(p.clinicId).trim() !== '') {
                    return String(p.clinicId).trim();
                }
            } catch (e2) { /* ignore */ }
            return null;
        };
        
        self.getAIDiagnosis = function(diagnosisData, visitId, petId, veterinarianId) {
            var url = ApiConfig.DIAGNOSIS;
            
            // Add query parameters only if they are provided
            var queryParams = [];
            if (visitId !== null && visitId !== undefined) {
                queryParams.push('visitId=' + encodeURIComponent(visitId));
            }
            if (petId !== null && petId !== undefined) {
                queryParams.push('petId=' + encodeURIComponent(petId));
            }
            if (veterinarianId !== null && veterinarianId !== undefined) {
                queryParams.push('veterinarianId=' + encodeURIComponent(veterinarianId));
            }
            
            if (queryParams.length > 0) {
                url += '?' + queryParams.join('&');
            }

            var body = angular.extend({}, diagnosisData);
            var resolved = self.readClinicIdForAi();
            if (resolved && !body.clinicId) {
                body.clinicId = resolved;
            }
            
            return $http.post(url, body)
                .then(function(response) {
                    return response.data;
                })
                .catch(function(error) {
                    console.error('AI diagnosis failed:', error);
                    return $q.reject(error);
                });
        };
        
        self.sendFeedback = function(predictionId, feedbackData, clinicId) {
            var url = ApiConfig.FEEDBACK + encodeURIComponent(predictionId) + '/feedback';
            var body = angular.extend({}, feedbackData);
            var cid =
                (clinicId != null && clinicId !== undefined && String(clinicId).trim() !== '')
                    ? String(clinicId).trim()
                    : self.readClinicIdForAi();
            if (cid) {
                body.clinicId = cid;
            }
            return $http.post(url, body)
                .then(function(response) {
                    return response.data;
                })
                .catch(function(error) {
                    console.error('Failed to send feedback:', error);
                    return $q.reject(error);
                });
        };
        
        self.createDiagnosisData = function(formData, petData, clinicId) {
            var payload = {
                symptoms_list: Array.isArray(formData.symptomsList) ? formData.symptomsList.join(', ') : formData.symptomsList,
                temperature: formData.temperature,
                weight_kg: formData.weightKg,
                heart_rate: formData.heartRate,
                symptom_duration: formData.symptomDuration,
                animal_type: petData.type ? petData.type.name : null,
                gender: petData.gender,
                age_months: petData.ageMonths,
                current_season: formData.currentSeason,
                vaccination_status: petData.vaccinationStatus,
                medical_history: formData.medicalHistory || null
            };
            if (clinicId != null && clinicId !== undefined && String(clinicId).trim() !== '') {
                payload.clinicId = String(clinicId).trim();
            }
            return payload;
        };
        
        self.createFeedbackData = function(finalDiagnosis, isCorrect, confidenceRating, comments, veterinarianId, aiDiagnosis) {
            var feedbackData = {
                finalDiagnosis: finalDiagnosis,
                isCorrect: isCorrect,
                comments: comments,
                veterinarianId: veterinarianId
            };

            // Keep track of the AI-suggested label so backend can apply a negative training signal on reject.
            if (aiDiagnosis !== null && aiDiagnosis !== undefined) {
                feedbackData.aiDiagnosis = aiDiagnosis;
            }
            
            // Only include confidenceRating if provided
            if (confidenceRating !== null && confidenceRating !== undefined) {
                feedbackData.confidenceRating = confidenceRating;
            }
            
            return feedbackData;
        };
        
        self.calculateConfidenceRating = function(aiPrediction, finalDiagnosis, isCorrect) {
            if (!aiPrediction || !aiPrediction.diagnosis || !finalDiagnosis) {
                return isCorrect ? 3 : 1; // Default fallback
            }
            
            var aiDiagnosis = aiPrediction.diagnosis.toLowerCase().trim();
            var finalDiag = finalDiagnosis.toLowerCase().trim();
            var aiConfidence = aiPrediction.confidence || 0;
            
            // Exact match
            if (aiDiagnosis === finalDiag) {
                if (isCorrect) {
                    // High confidence when AI is correct and matches exactly
                    return aiConfidence > 0.8 ? 5 : (aiConfidence > 0.6 ? 4 : 3);
                } else {
                    // AI was correct but doctor marked as incorrect - low confidence
                    return 1;
                }
            }
            
            // Partial match (contains similar keywords)
            if (self.isPartialMatch(aiDiagnosis, finalDiag)) {
                if (isCorrect) {
                    // Partial match with doctor confirmation
                    return aiConfidence > 0.7 ? 4 : 3;
                } else {
                    // Partial match but doctor disagrees
                    return 2;
                }
            }
            
            // No match
            if (isCorrect) {
                // Doctor confirmed AI despite different diagnosis - moderate confidence
                return 2;
            } else {
                // AI was wrong and doctor corrected - very low confidence
                return 0;
            }
        };
        
        self.isPartialMatch = function(aiDiagnosis, finalDiagnosis) {
            // Check for common medical terms and patterns
            var aiWords = aiDiagnosis.split(/\s+/);
            var finalWords = finalDiagnosis.split(/\s+/);
            
            // Common veterinary terms to match
            var medicalTerms = [
                'infection', 'inflammation', 'disease', 'syndrome', 'disorder',
                'acute', 'chronic', 'bacterial', 'viral', 'fungal', 'parasitic',
                'respiratory', 'gastrointestinal', 'dermatological', 'neurological',
                'cardiac', 'renal', 'hepatic', 'musculoskeletal', 'ocular',
                'fever', 'cough', 'diarrhea', 'vomiting', 'skin', 'joint', 'heart',
                'lung', 'liver', 'kidney', 'stomach', 'intestine', 'colon'
            ];
            
            var matchingTerms = 0;
            var totalTerms = 0;
            
            // Count medical terms in both diagnoses
            medicalTerms.forEach(function(term) {
                var aiHasTerm = aiDiagnosis.includes(term);
                var finalHasTerm = finalDiagnosis.includes(term);
                
                if (aiHasTerm || finalHasTerm) {
                    totalTerms++;
                    if (aiHasTerm && finalHasTerm) {
                        matchingTerms++;
                    }
                }
            });
            
            // If at least 50% of medical terms match, consider it partial match
            return totalTerms > 0 && (matchingTerms / totalTerms) >= 0.5;
        };
    }]);
