'use strict';

angular.module('ownerForm')
    .controller('OwnerFormController', ["$http", '$state', '$stateParams', function ($http, $state, $stateParams) {
        var self = this;

        var ownerId = $stateParams.ownerId || 0;

        if (!ownerId) {
            self.owner = {};
        } else {
            $http.get("api/customer/owners/" + ownerId).then(function (resp) {
                self.owner = resp.data;
                initPhoneFromOwner();
            });
        }

        // Simple country code list for phone prefix selection
        self.countries = [
            { code: '+84', name: 'Vietnam' },
            { code: '+1', name: 'United States / Canada' },
            { code: '+44', name: 'United Kingdom' },
            { code: '+81', name: 'Japan' },
            { code: '+61', name: 'Australia' }
        ];

        // Selected country code and local phone part are kept only in UI.
        // We still persist a single telephone string on the backend.
        self.selectedCountry = self.countries[0];
        self.localPhone = '';

        // When editing an existing owner, try to split telephone into country code + local part
        function initPhoneFromOwner() {
            if (!self.owner || !self.owner.telephone) return;
            var tel = self.owner.telephone.trim();
            var match = self.countries.find(function (c) { return tel.indexOf(c.code) === 0; });
            if (match) {
                self.selectedCountry = match;
                self.localPhone = tel.replace(match.code, '').trim();
            } else {
                self.localPhone = tel;
            }
        }

        self.submitOwnerForm = function () {
            var id = self.owner.id;

            // Compose full international phone number before sending to backend
            if (self.localPhone) {
                self.owner.telephone = (self.selectedCountry.code + ' ' + self.localPhone).trim();
            }

            if (id) {
                $http.put('api/customer/owners/' + id, self.owner).then(function () {
                    $state.go('ownerDetails', {ownerId: ownerId});
                });
            } else {
                $http.post('api/customer/owners', self.owner).then(function () {
                    $state.go('owners');
                });
            }
        };
    }]);
