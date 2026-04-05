'use strict';

angular.module('vetForm')
    .controller('VetFormController', ["$http", '$state', '$stateParams', 'AuthService', function ($http, $state, $stateParams, AuthService) {
        var self = this;

        self.vet = {};
        self.specialties = [];
        self.selectedSpecialtyIds = {};
        self.isEdit = !!$stateParams.vetId;

        /** Non-admin staff: only their own vet row; admins are not redirected. */
        function getRestrictedStaffVetId() {
            try {
                var u = JSON.parse(localStorage.getItem('vet_clinic_user') || '{}');
                var v = u.veterinarianId;
                var vid = (v != null && Number(v) > 0) ? Number(v) : null;
                var admin = u.clinicAdmin === true || u.clinicAdmin === 'true';
                return (vid != null && !admin) ? vid : null;
            } catch (e) {
                return null;
            }
        }

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

        function start() {
            AuthService.refreshStoredUserFromServer().then(function () {
                var restrictedVetId = getRestrictedStaffVetId();
                if (!self.isEdit && restrictedVetId) {
                    $state.go('vets');
                    return;
                }
                if (self.isEdit && restrictedVetId && parseInt($stateParams.vetId, 10) !== restrictedVetId) {
                    $state.go('vets');
                    return;
                }
                $http.get('api/vet/specialties').then(function (resp) {
                    self.specialties = resp.data || [];
                }).then(loadVetIfEditing);
            });
        }

        start();

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

