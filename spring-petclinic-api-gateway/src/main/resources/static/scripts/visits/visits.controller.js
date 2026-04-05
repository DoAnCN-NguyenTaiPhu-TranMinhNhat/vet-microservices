'use strict';

angular.module('visits')
    .controller('VisitsController', ['$http', '$state', '$stateParams', '$filter', 
                                  'AuthService', 'PetService', 'VisitService', 'DiagnosisService',
                                  function ($http, $state, $stateParams, $filter, 
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
        
        // Loading states
        self.petLoading = true;
        self.petLoadError = null;
        
        // Current veterinarian ID (loaded from session)
        self.currentVeterinarianId = null;
        
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
                })
                .catch(function(error) {
                    console.error('Failed to send feedback:', error);
                });
            
            // Clear AI prediction
            self.aiPrediction = null;
            self.aiError = null;
        };
        
        self.rejectAISuggestion = function () {
            if (!self.aiPrediction || !self.aiPrediction.diagnosis) {
                return;
            }
            
            if (!self.currentVeterinarianId) {
                console.error('Cannot reject suggestion: No veterinarian ID');
                return;
            }

            // Reject requires doctor's selected final diagnosis (dropdown).
            // If empty, we'd send null/empty to backend and FastAPI returns 422.
            if (!self.targetDiagnosis || self.targetDiagnosis.toString().trim() === "") {
                alert('Please select a Target Diagnosis before rejecting the AI suggestion.');
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
                })
                .catch(function(error) {
                    // Backend may return 400 with message when finalDiagnosis is missing.
                    // Show it as UI notification instead of only logging to console.
                    var msg = error && (error.data?.message || error.data?.error) ? (error.data.message || error.data.error) : null;
                    if (!msg && error && error.statusText) msg = error.statusText;
                    if (!msg) msg = 'Failed to send rejection feedback. Please try again.';
                    alert(msg);
                });
            
            // Clear AI prediction
            self.aiPrediction = null;
            self.aiError = null;
        };
    }]);
