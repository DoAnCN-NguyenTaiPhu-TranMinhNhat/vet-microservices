'use strict';

angular.module('visits')
    .controller('VisitsController', ['$http', '$state', '$stateParams', '$filter', function ($http, $state, $stateParams, $filter) {
        var self = this;
        var petId = $stateParams.petId || 0;
        var url = "api/visit/owners/" + ($stateParams.ownerId || 0) + "/pets/" + petId + "/visits";
        
        // Initialize form fields
        self.date = new Date();
        self.desc = "";
        self.temperature = null;
        self.weightKg = null;
        self.heartRate = null;
        self.symptomDuration = null;
        self.symptomsList = "";
        self.targetDiagnosis = "";
        
        // AI Required Fields
        self.animalType = "dog";
        self.gender = "male";
        self.ageMonths = 24;
        self.currentSeason = "summer";
        self.vaccinationStatus = "yes";

        // AI Diagnosis state
        self.aiLoading = false;
        self.aiPrediction = null;
        self.aiError = null;

        // Load existing visits and pet data
        $http.get(url).then(function (resp) {
            self.visits = resp.data;
        });

        // Load pet data for AI fields
        $http.get('/api/petDetails/' + petId).then(function (resp) {
            self.pet = resp.data;
            // Calculate age in months from birth date
            if (self.pet && self.pet.birthDate) {
                var birthDate = new Date(self.pet.birthDate);
                var now = new Date();
                self.ageMonths = Math.floor((now - birthDate) / (1000 * 60 * 60 * 24 * 30));
            }
        });

        // Reset form
        self.resetForm = function () {
            self.date = new Date();
            self.desc = "";
            self.temperature = null;
            self.weightKg = null;
            self.heartRate = null;
            self.symptomDuration = null;
            self.symptomsList = "";
            self.targetDiagnosis = "";
            
            // Reset AI fields to defaults
            self.animalType = "dog";
            self.gender = "male";
            self.ageMonths = 24;
            self.currentSeason = "summer";
            self.vaccinationStatus = "yes";
            
            // Reset AI state
            self.aiLoading = false;
            self.aiPrediction = null;
            self.aiError = null;
        };

        // Submit form
        self.submit = function () {
            var data = {
                date: $filter('date')(self.date, "yyyy-MM-dd"),
                description: self.desc
            };

            // Add medical data fields if provided
            if (self.temperature !== null && self.temperature !== "") {
                data.temperature = parseFloat(self.temperature);
            }
            if (self.weightKg !== null && self.weightKg !== "") {
                data.weightKg = parseFloat(self.weightKg);
            }
            if (self.heartRate !== null && self.heartRate !== "") {
                data.heartRate = parseInt(self.heartRate);
            }
            if (self.symptomDuration !== null && self.symptomDuration !== "") {
                data.symptomDuration = parseInt(self.symptomDuration);
            }
            if (self.symptomsList && self.symptomsList.trim() !== "") {
                data.symptomsList = self.symptomsList.trim();
            }
            if (self.targetDiagnosis && self.targetDiagnosis !== "") {
                data.targetDiagnosis = self.targetDiagnosis;
            }

            $http.post(url, data).then(function () {
                // Reload visits to show new data
                $http.get(url).then(function (resp) {
                    self.visits = resp.data;
                });
                // Reset form after successful submission
                self.resetForm();
            }, function (error) {
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

            self.aiLoading = true;
            self.aiError = null;
            self.aiPrediction = null;

            var diagnosisData = {
                symptoms_list: self.symptomsList,
                temperature: self.temperature,
                weight_kg: self.weightKg,
                heart_rate: self.heartRate,
                symptom_duration: self.symptomDuration,
                animal_type: self.animalType,
                gender: self.gender,
                age_months: self.ageMonths,
                current_season: self.currentSeason,
                vaccination_status: self.pet ? self.pet.vaccinationStatus : "yes"
            };

            // Include visit/pet/vet IDs for training integration
            var diagnosisUrl = '/api/genai/diagnosis';
            if (self.pet && self.pet.id) {
                diagnosisUrl += '?visitId=' + (self.visits ? self.visits.length + 1 : 1) + 
                               '&petId=' + self.pet.id + 
                               '&veterinarianId=1';
            }

            $http.post(diagnosisUrl, diagnosisData).then(function (response) {
                self.aiPrediction = response.data;
                self.aiLoading = false;
                console.log('AI diagnosis received:', response.data);
            }, function (error) {
                self.aiError = error.data?.message || error.statusText || 'AI diagnosis failed';
                self.aiLoading = false;
            });
        };

        // Helper function to get current season
        function getCurrentSeason() {
            var month = new Date().getMonth() + 1; // 1-12
            if (month >= 3 && month <= 5) return "spring";
            if (month >= 6 && month <= 8) return "summer";
            if (month >= 9 && month <= 11) return "fall";
            return "winter";
        }

        self.acceptAISuggestion = function () {
            if (self.aiPrediction && self.aiPrediction.diagnosis) {
                // Update UI
                self.targetDiagnosis = self.aiPrediction.diagnosis;
                
                // Send feedback to training system
                var feedbackData = {
                    finalDiagnosis: self.aiPrediction.diagnosis,
                    isCorrect: true,
                    confidenceRating: 4,
                    comments: "Doctor accepted AI suggestion",
                    veterinarianId: 1  // TODO: Get actual vet ID
                };
                
                // Send feedback API call
                $http.post('/api/genai/diagnosis/1/feedback', feedbackData).then(function () {
                    console.log('Feedback sent successfully');
                }, function (error) {
                    console.error('Failed to send feedback:', error);
                });
                
                // Clear AI prediction
                self.aiPrediction = null;
                self.aiError = null;
            }
        };

        self.rejectAISuggestion = function () {
            if (self.aiPrediction && self.aiPrediction.diagnosis) {
                // Send feedback to training system
                var feedbackData = {
                    finalDiagnosis: self.targetDiagnosis || "Unknown",
                    isCorrect: false,
                    confidenceRating: 2,
                    comments: "Doctor rejected AI suggestion",
                    veterinarianId: 1  // TODO: Get actual vet ID
                };
                
                // Send feedback API call
                $http.post('/api/genai/diagnosis/1/feedback', feedbackData).then(function () {
                    console.log('Rejection feedback sent successfully');
                }, function (error) {
                    console.error('Failed to send rejection feedback:', error);
                });
                
                // Clear AI prediction
                self.aiPrediction = null;
                self.aiError = null;
            }
        };
    }]);
