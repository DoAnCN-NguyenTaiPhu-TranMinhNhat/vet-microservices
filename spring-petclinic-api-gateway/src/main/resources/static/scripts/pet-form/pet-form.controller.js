'use strict';

angular.module('petForm')
    .controller('PetFormController', ['$http', '$state', '$stateParams', function ($http, $state, $stateParams) {
        var self = this;
        var ownerId = $stateParams.ownerId || 0;

        $http.get('api/customer/petTypes').then(function (resp) {
            self.types = resp.data;
        }).then(function () {

            var petId = $stateParams.petId || 0;

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

        self.submit = function () {
            var id = self.pet.id || 0;
            
            console.log("Pet submit - ID:", id);
            console.log("Pet data:", self.pet);

            var data = {
                id: id,
                name: self.pet.name,
                birthDate: self.pet.birthDate,
                typeId: self.petTypeId,
                gender: self.pet.gender,
                vaccinationStatus: self.pet.vaccinationStatus,
                medicalNotes: self.pet.medicalNotes
            };

            if (id) {
                console.log("Updating pet with ID:", id);
                $http.put("api/customer/owners/" + ownerId + "/pets/" + id, data).then(function () {
                    $state.go('ownerDetails', {ownerId: ownerId});
                });
            } else {
                console.log("Creating new pet");
                $http.post("api/customer/owners/" + ownerId + "/pets", data).then(function () {
                    $state.go('ownerDetails', {ownerId: ownerId});
                });
            }
        };
    }]);
