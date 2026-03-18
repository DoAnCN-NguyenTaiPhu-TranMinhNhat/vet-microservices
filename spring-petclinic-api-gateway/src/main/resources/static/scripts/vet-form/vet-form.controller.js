'use strict';

angular.module('vetForm')
    .controller('VetFormController', ["$http", '$state', '$stateParams', function ($http, $state, $stateParams) {
        var self = this;

        self.vet = {};
        self.specialties = [];
        self.selectedSpecialtyIds = {};
        self.isEdit = !!$stateParams.vetId;

        function loadVetIfEditing() {
            if (!self.isEdit) return;
            $http.get('api/vet/vets/' + $stateParams.vetId).then(function (resp) {
                self.vet = resp.data || {};
                // pre-select specialties
                (self.vet.specialties || []).forEach(function (s) {
                    if (s && s.id != null) self.selectedSpecialtyIds[s.id] = true;
                });
            });
        }

        $http.get('api/vet/specialties').then(function (resp) {
            self.specialties = resp.data || [];
        }).then(loadVetIfEditing);

        self.submitVetForm = function () {
            var specialtyIds = Object.keys(self.selectedSpecialtyIds)
                .filter(function (k) { return self.selectedSpecialtyIds[k]; })
                .map(function (k) { return parseInt(k, 10); });

            var payload = {
                firstName: self.vet.firstName,
                lastName: self.vet.lastName,
                specialtyIds: specialtyIds
            };

            if (self.isEdit) {
                $http.put('api/vet/vets/' + $stateParams.vetId, payload).then(function () {
                    $state.go('vets');
                });
            } else {
                $http.post('api/vet/vets', payload).then(function () {
                    $state.go('vets');
                });
            }
        };
    }]);

