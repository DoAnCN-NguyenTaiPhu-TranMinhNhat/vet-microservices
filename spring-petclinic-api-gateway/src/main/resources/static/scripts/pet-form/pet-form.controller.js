'use strict';

angular.module('petForm')
    .controller('PetFormController', ['$http', '$state', '$stateParams', '$filter', function ($http, $state, $stateParams, $filter) {
        var self = this;
        var ownerId = $stateParams.ownerId || '';

        $http.get('api/customer/petTypes').then(function (resp) {
            self.types = resp.data;
        }).then(function () {

            var petId = $stateParams.petId;

            if (petId) { // edit
                $http.get("api/customer/owners/" + ownerId + "/pets/" + petId).then(function (resp) {
                    self.pet = resp.data;
                    self.pet.birthDate = new Date(self.pet.birthDate);
                    self.petTypeId = "" + self.pet.type.id;
                });
            } else {
                $http.get('api/customer/owners/' + ownerId).then(function (resp) {
                    self.pet = {
                        owner: resp.data.firstName + " " + resp.data.lastName,
                        gender: "",
                        vaccinationStatus: "",
                        medicalNotes: ""
                    };
                    self.petTypeId = "1";
                })

            }
        });

        function formatBirthDate(bd) {
            if (bd === undefined || bd === null || bd === '') {
                return null;
            }
            if (angular.isDate(bd)) {
                return $filter('date')(bd, 'yyyy-MM-dd');
            }
            if (angular.isString(bd) && /^\d{4}-\d{2}-\d{2}/.test(bd)) {
                return bd.substring(0, 10);
            }
            return $filter('date')(bd, 'yyyy-MM-dd') || null;
        }

        self.submit = function () {
            // Use route param, not self.pet.id — avoids treating a stray numeric id as "edit" and sending id:0 (400).
            var isEdit = !!$stateParams.petId;
            var birthDateStr = formatBirthDate(self.pet.birthDate);
            if (!birthDateStr) {
                alert('Please select a valid birth date.');
                return;
            }
            var typeIdNum = parseInt(self.petTypeId, 10);
            var data = {
                name: self.pet.name,
                birthDate: birthDateStr,
                typeId: isNaN(typeIdNum) ? 1 : typeIdNum,
                gender: self.pet.gender,
                vaccinationStatus: self.pet.vaccinationStatus,
                medicalNotes: self.pet.medicalNotes
            };
            if (isEdit) {
                data.id = self.pet.id;
            }

            if (isEdit) {
                $http.put("api/customer/owners/" + encodeURIComponent(ownerId) + "/pets/" + encodeURIComponent(self.pet.id), data).then(function () {
                    $state.go('ownerDetails', {ownerId: ownerId});
                });
            } else {
                $http.post("api/customer/owners/" + encodeURIComponent(ownerId) + "/pets", data).then(function () {
                    $state.go('ownerDetails', {ownerId: ownerId});
                });
            }
        };
    }]);
