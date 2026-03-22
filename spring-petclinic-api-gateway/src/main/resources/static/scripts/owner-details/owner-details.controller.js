'use strict';

angular.module('ownerDetails')
    .controller('OwnerDetailsController', ['$http', '$state', '$stateParams', '$timeout', function ($http, $state, $stateParams, $timeout) {
        var self = this;
        self.loading = true;
        self.notFound = false;

        function loadOwner() {
            self.loading = true;
            self.notFound = false;
            // cache-buster to avoid any intermediate caching after mutations
            var url = 'api/customer/owners/' + $stateParams.ownerId + '?_=' + Date.now();
            $http.get(url).then(function (resp) {
                self.owner = resp.data;
                self.loading = false;
            }).catch(function (err) {
                self.loading = false;
                if (err && err.status === 404) {
                    self.notFound = true;
                    self.owner = null;
                    // Redirect back to owners after a short delay
                    $timeout(function () {
                        $state.go('owners');
                    }, 1500);
                }
            });
        }

        self.deleteOwner = function ($event) {
            if ($event) {
                if ($event.preventDefault) $event.preventDefault();
                if ($event.stopPropagation) $event.stopPropagation();
            }
            console.log('Owner delete clicked:', $stateParams.ownerId);
            if (!self.owner || !self.owner.id) return;
            if (!confirm('Delete this owner and all pets?')) return;
            $http.delete('api/orch/owners/' + $stateParams.ownerId).then(function () {
                $state.go('owners');
            }).catch(function (err) {
                alert('Delete owner failed (' + (err.status || 'unknown') + ')');
                console.log('Delete owner failed:', err);
            });
        };

        self.deletePet = function ($event, petId) {
            if ($event && $event.preventDefault) $event.preventDefault();
            if ($event && $event.stopPropagation) $event.stopPropagation();
            console.log('Pet delete clicked:', petId);
            if (petId === undefined || petId === null || petId === '') {
                alert('Invalid pet id: ' + petId);
                return;
            }
            if (!confirm('Delete this pet?')) return;
            var pid = Number(petId);
            $http.delete('api/orch/owners/' + $stateParams.ownerId + '/pets/' + pid).then(function () {
                // optimistic UI update in case reload is delayed
                if (self.owner && Array.isArray(self.owner.pets)) {
                    self.owner.pets = self.owner.pets.filter(function (p) { return p && Number(p.id) !== pid; });
                }
                loadOwner();
            }).catch(function (err) {
                alert('Delete pet failed (' + (err.status || 'unknown') + ')');
                console.log('Delete pet failed:', err);
            });
        };

        loadOwner();
    }]);
