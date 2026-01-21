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

        // Load existing visits
        $http.get(url).then(function (resp) {
            self.visits = resp.data;
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
    }]);
