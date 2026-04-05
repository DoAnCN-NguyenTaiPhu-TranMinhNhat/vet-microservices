'use strict';

angular.module('vetList')
    .controller('VetListController', ['$http', 'AuthService', function ($http, AuthService) {
        var self = this;

        function clinicVetRestricted() {
            try {
                var u = JSON.parse(localStorage.getItem('vet_clinic_user') || '{}');
                var hasVet = u.veterinarianId != null && Number(u.veterinarianId) > 0;
                var admin = u.clinicAdmin === true || u.clinicAdmin === 'true';
                return hasVet && !admin;
            } catch (e) {
                return false;
            }
        }

        function load() {
            $http.get('api/vet/vets').then(function (resp) {
                self.vetList = resp.data;
            });
        }

        AuthService.refreshStoredUserFromServer().then(function () {
            self.restrictVetManagement = clinicVetRestricted();
            load();
        });

        self.deleteVet = function ($event, vetId) {
            if ($event) {
                if ($event.preventDefault) $event.preventDefault();
                if ($event.stopPropagation) $event.stopPropagation();
            }
            console.log('Vet delete clicked:', vetId);
            if (!confirm('Delete this veterinarian?')) return;
            $http.delete('api/vet/vets/' + vetId).then(load).catch(function (err) {
                alert('Delete vet failed (' + (err.status || 'unknown') + ')');
                console.log('Delete vet failed:', err);
            });
        };
    }]);
