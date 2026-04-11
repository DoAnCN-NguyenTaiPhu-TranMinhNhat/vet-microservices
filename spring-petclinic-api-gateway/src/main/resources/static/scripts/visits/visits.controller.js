'use strict';

angular.module('visits')
    .controller('VisitsController', ['$http', '$state', '$stateParams', '$filter', '$timeout',
                                  'AuthService', 'PetService', 'VisitService', 'DiagnosisService',
                                  function ($http, $state, $stateParams, $filter, $timeout,
                                           AuthService, PetService, VisitService, DiagnosisService) {
        var self = this;
        // UUID path params must stay strings — parseInt() truncates at the first non-digit (e.g. 76524f8a-… → 76524).
        var petId = $stateParams.petId != null ? String($stateParams.petId).trim() : '';
        var ownerId = $stateParams.ownerId != null ? String($stateParams.ownerId).trim() : '';

        if (!petId) {
            self.petLoadError = 'Invalid pet ID. Please navigate from the owner details page.';
            return;
        }
        if (!ownerId) {
            self.petLoadError = 'Invalid owner ID. Please navigate from the owner details page.';
            return;
        }
        
        // Define loadData function before calling it
        self.loadData = function() {
            self.petLoading = true;
            self.petLoadError = null;
            
            // Load pet details FIRST using original IDs
            PetService.getPetDetails(petId, ownerId)
                .then(function(pet) {
                    self.pet = pet;
                    
                    // Initialize AI fields from pet data AFTER pet is loaded
                    self.animalType = pet.type ? pet.type.name : null;
                    self.gender = pet.gender || null;
                    self.ageMonths = PetService.calculateAgeInMonths(pet.birthDate);
                    self.currentSeason = PetService.getCurrentSeason();
                    self.vaccinationStatus = pet.vaccinationStatus || null;
                    self.medicalHistory = pet.medicalNotes || "";
                    
                    // Set form date to current date
                    self.date = new Date();
                    
                    self.petLoading = false;
                    
                    // Load visits after pet data is loaded using original IDs
                    VisitService.getVisits(ownerId, petId)
                        .then(function(visits) {
                            self.visits = visits;
                        })
                        .catch(function(error) {
                            console.error('Failed to load visits:', error);
                        });
                    
                    // Load veterinarian authentication
                    AuthService.getCurrentUser()
                        .then(function(user) {
                            self.currentVeterinarianId = user.veterinarianId;
                        })
                        .catch(function(error) {
                            console.error('Failed to load current user:', error);
                            self.authError = 'Authentication failed. Please try again.';
                        });
                })
                .catch(function(error) {
                    console.error('Failed to load pet details:', error);
                    self.petLoadError = 'Failed to load pet details. Please try again.';
                    self.petLoading = false;
                });
        };
        
        // Initialize form fields - no hardcoded defaults
        self.date = null; // Will be set to current date when form is ready
        self.desc = "";
        self.temperature = null;
        self.weightKg = null;
        self.heartRate = null;
        self.symptomDuration = null;
        self.symptomsList = "";
        self.medicalHistory = null; // Will be loaded from pet data
        self.targetDiagnosis = "";
        
        // AI-related fields - initialized as null until data loads
        self.animalType = null;
        self.gender = null;
        self.ageMonths = null;
        self.currentSeason = null;
        self.vaccinationStatus = null;
        
        // AI Diagnosis state
        self.aiLoading = false;
        self.aiPrediction = null;
        self.aiError = null;
        /** Accept/Reject feedback (inline; avoids duplicate global HTTP alerts). */
        self.aiFeedbackNotice = null;
        self.aiFeedbackError = null;
        
        // Loading states
        self.petLoading = true;
        self.petLoadError = null;
        
        // Current veterinarian ID (loaded from session)
        self.currentVeterinarianId = null;
        
        function normalizeDiagnosisLabel(s) {
            if (s == null) {
                return '';
            }
            return String(s).trim().toLowerCase();
        }

        /** After a failed Reject (validation or API), move focus to Target Diagnosis — not Description. */
        function focusTargetDiagnosisSelect() {
            $timeout(function () {
                var el = document.getElementById('visit-target-diagnosis-select');
                if (el && typeof el.focus === 'function') {
                    el.focus();
                    try {
                        el.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
                    } catch (e) { /* ignore */ }
                }
            }, 0);
        }

        /**
         * Lấy nội dung lỗi từ Spring / FastAPI / gateway (một lần, không alert).
         */
        function extractFeedbackErrorMessage(error) {
            if (!error) {
                return 'Could not save feedback. Please try again.';
            }
            var d = error.data;
            if (d && typeof d === 'object') {
                if (d.message && String(d.message).trim()) {
                    return String(d.message).trim();
                }
                if (d.error && String(d.error).trim() && d.error !== 'Bad Request') {
                    return String(d.error).trim();
                }
                if (Array.isArray(d.detail)) {
                    var parts = d.detail.map(function (e) {
                        if (!e) {
                            return '';
                        }
                        if (e.msg) {
                            return e.msg;
                        }
                        return JSON.stringify(e);
                    }).filter(Boolean);
                    if (parts.length) {
                        return parts.join(' ');
                    }
                }
                if (Array.isArray(d.errors)) {
                    var errs = d.errors.map(function (e) {
                        return (e && (e.defaultMessage || e.message)) || '';
                    }).filter(Boolean);
                    if (errs.length) {
                        return errs.join(' ');
                    }
                }
            }
            if (error.statusText) {
                return error.statusText;
            }
            return 'Could not save feedback. Please try again.';
        }

        function clinicIdForSession() {
            if (DiagnosisService.readClinicIdForAi) {
                return DiagnosisService.readClinicIdForAi();
            }
            try {
                var u = JSON.parse(localStorage.getItem('vet_clinic_user') || '{}');
                var v = u && u.clinicId != null && u.clinicId !== '' ? u.clinicId : u && u.clinic_id;
                if (v != null && v !== '') {
                    return String(v).trim();
                }
            } catch (e) { /* ignore */ }
            return null;
        }

        // Call loadData to start the process
        self.loadData();
        
        // Reset form - restore from pet profile, not hardcoded defaults
        self.resetForm = function () {
            self.date = new Date(); // Current date
            self.desc = "";
            self.temperature = null;
            self.weightKg = null;
            self.heartRate = null;
            self.symptomDuration = null;
            self.symptomsList = "";
            self.medicalHistory = self.pet && self.pet.medicalNotes ? self.pet.medicalNotes : null;
            self.targetDiagnosis = "";
            
            // Reset AI fields from pet data
            if (self.pet) {
                self.animalType = self.pet.type ? self.pet.type.name : null;
                self.gender = self.pet.gender || null;
                self.ageMonths = PetService.calculateAgeInMonths(self.pet.birthDate);
                self.currentSeason = PetService.getCurrentSeason();
                self.vaccinationStatus = self.pet.vaccinationStatus || null;
            }
            
            // Reset AI state
            self.aiLoading = false;
            self.aiPrediction = null;
            self.aiError = null;
            self.aiFeedbackNotice = null;
            self.aiFeedbackError = null;
        };
        
        // Submit form - create visit first, then optionally AI diagnosis
        self.submit = function () {
            if (!self.currentVeterinarianId) {
                alert('Veterinarian authentication required');
                return;
            }
            
            var visitData = {
                date: $filter('date')(self.date, "yyyy-MM-dd"),
                description: self.desc
            };

            // Add medical data fields if provided
            if (self.temperature !== null && self.temperature !== "") {
                visitData.temperature = parseFloat(self.temperature);
            }
            if (self.weightKg !== null && self.weightKg !== "") {
                visitData.weightKg = parseFloat(self.weightKg);
            }
            if (self.heartRate !== null && self.heartRate !== "") {
                visitData.heartRate = parseInt(self.heartRate);
            }
            if (self.symptomDuration !== null && self.symptomDuration !== "") {
                visitData.symptomDuration = parseInt(self.symptomDuration);
            }
            if (self.symptomsList && self.symptomsList.trim() !== "") {
                visitData.symptomsList = self.symptomsList.trim();
            }
            if (self.targetDiagnosis && self.targetDiagnosis !== "") {
                visitData.targetDiagnosis = self.targetDiagnosis;
            }

            VisitService.createVisit(ownerId, petId, visitData)
                .then(function(createdVisit) {
                    console.log('Visit created successfully:', createdVisit);
                    
                    // Reload visits to show new data using original IDs
                    return VisitService.getVisits(ownerId, petId);
                })
                .then(function(visits) {
                    self.visits = visits;
                    self.resetForm();
                })
                .catch(function(error) {
                    console.error('Error creating visit:', error);
                    alert('Error creating visit: ' + (error.data?.message || error.statusText || 'Unknown error'));
                });
        };
        
        // AI Diagnosis functions
        self.getAIDiagnosis = function () {
            if (!self.symptomsList || self.symptomsList.trim() === "") {
                self.aiError = "Please enter symptoms list first";
                return;
            }
            
            if (!self.currentVeterinarianId) {
                self.aiError = "Veterinarian authentication required for AI diagnosis";
                return;
            }
            
            if (!self.pet || self.pet.id === undefined || self.pet.id === null) {
                self.aiError = "Pet information required for AI diagnosis";
                return;
            }

            self.aiLoading = true;
            self.aiError = null;
            self.aiPrediction = null;
            self.aiFeedbackNotice = null;
            self.aiFeedbackError = null;

            // Create diagnosis data using service
            var formData = {
                symptomsList: self.symptomsList,
                temperature: self.temperature,
                weightKg: self.weightKg,
                heartRate: self.heartRate,
                symptomDuration: self.symptomDuration,
                medicalHistory: self.medicalHistory,
                currentSeason: self.currentSeason
            };
            
            var petData = {
                type: self.pet.type,
                gender: self.gender,
                ageMonths: self.ageMonths,
                vaccinationStatus: self.vaccinationStatus
            };
            
            var diagnosisData = DiagnosisService.createDiagnosisData(formData, petData, clinicIdForSession());

            // Don't estimate visitId - let backend handle it or require visit creation first
            DiagnosisService.getAIDiagnosis(diagnosisData, null, self.pet.id, self.currentVeterinarianId)
                .then(function(response) {
                    self.aiPrediction = response;
                    self.aiLoading = false;
                    console.log('AI diagnosis received:', response);
                })
                .catch(function(error) {
                    self.aiError = error.data?.message || error.statusText || 'AI diagnosis failed';
                    self.aiLoading = false;
                });
        };
        
        self.acceptAISuggestion = function () {
            if (!self.aiPrediction || !self.aiPrediction.diagnosis) {
                return;
            }
            
            if (!self.currentVeterinarianId) {
                console.error('Cannot accept suggestion: No veterinarian ID');
                return;
            }

            self.aiFeedbackNotice = null;
            self.aiFeedbackError = null;
            
            // Update UI
            self.targetDiagnosis = self.aiPrediction.diagnosis;
            
            // Calculate confidence rating based on AI prediction vs final diagnosis
            var calculatedConfidence = DiagnosisService.calculateConfidenceRating(
                self.aiPrediction,
                self.aiPrediction.diagnosis,
                true  // isCorrect = true for accept
            );
            
            console.log('Calculated confidence rating for accept:', calculatedConfidence);
            
            // Create feedback data using service
            var feedbackData = DiagnosisService.createFeedbackData(
                self.aiPrediction.diagnosis,
                true,
                calculatedConfidence,  // Use calculated confidence
                "Doctor accepted AI suggestion (confidence: " + calculatedConfidence + ")",
                self.currentVeterinarianId,
                self.aiPrediction.diagnosis
            );
            
            // Use predictionId from response
            var predictionId = self.aiPrediction.predictionId;
            
            DiagnosisService.sendFeedback(predictionId, feedbackData, clinicIdForSession())
                .then(function() {
                    console.log('Feedback sent successfully for prediction:', predictionId);
                    self.aiFeedbackNotice = 'Feedback saved: you agreed with the AI suggestion.';
                    self.aiFeedbackError = null;
                    self.aiPrediction = null;
                    self.aiError = null;
                })
                .catch(function(error) {
                    console.error('Failed to send feedback:', error);
                    self.aiFeedbackError = extractFeedbackErrorMessage(error);
                    self.aiFeedbackNotice = null;
                });
        };
        
        self.rejectAISuggestion = function () {
            if (!self.aiPrediction || !self.aiPrediction.diagnosis) {
                return;
            }
            
            if (!self.currentVeterinarianId) {
                console.error('Cannot reject suggestion: No veterinarian ID');
                return;
            }

            self.aiFeedbackNotice = null;
            self.aiFeedbackError = null;

            // Reject requires doctor's selected final diagnosis (dropdown).
            if (!self.targetDiagnosis || self.targetDiagnosis.toString().trim() === "") {
                self.aiFeedbackError = 'Please select a Target Diagnosis before rejecting the AI suggestion.';
                focusTargetDiagnosisSelect();
                return;
            }

            var aiLabel = normalizeDiagnosisLabel(self.aiPrediction.diagnosis);
            var targetLabel = normalizeDiagnosisLabel(self.targetDiagnosis);
            if (aiLabel === targetLabel) {
                self.aiFeedbackError = 'Invalid reject: your Target Diagnosis matches the AI suggestion. '
                    + 'Choose a different diagnosis, or use Accept if you agree with the AI.';
                focusTargetDiagnosisSelect();
                return;
            }
            
            // Calculate confidence rating based on AI prediction vs final diagnosis
            var calculatedConfidence = DiagnosisService.calculateConfidenceRating(
                self.aiPrediction,
                self.targetDiagnosis || "Unknown diagnosis",  // Use doctor's final diagnosis
                false  // isCorrect = false for reject
            );
            
            console.log('Calculated confidence rating for reject:', calculatedConfidence);
            
            // Create feedback data using service
            var feedbackData = DiagnosisService.createFeedbackData(
                self.targetDiagnosis,
                false,
                calculatedConfidence,  // Use calculated confidence
                "Doctor rejected AI suggestion (confidence: " + calculatedConfidence + ")",
                self.currentVeterinarianId,
                self.aiPrediction.diagnosis
            );
            
            // Use predictionId from response
            var predictionId = self.aiPrediction.predictionId;
            
            DiagnosisService.sendFeedback(predictionId, feedbackData, clinicIdForSession())
                .then(function() {
                    console.log('Rejection feedback sent successfully for prediction:', predictionId);
                    self.aiFeedbackNotice = 'Feedback saved: you rejected the AI suggestion with a different diagnosis.';
                    self.aiFeedbackError = null;
                    self.aiPrediction = null;
                    self.aiError = null;
                })
                .catch(function(error) {
                    console.error('Failed to send rejection feedback:', error);
                    self.aiFeedbackError = extractFeedbackErrorMessage(error);
                    self.aiFeedbackNotice = null;
                    focusTargetDiagnosisSelect();
                });
        };
    }]);
