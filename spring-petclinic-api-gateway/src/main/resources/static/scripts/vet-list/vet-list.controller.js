'use strict';

angular.module('vetList')
    .controller('VetListController', ['$http', function ($http) {
        var self = this;

        function load() {
            $http.get('api/vet/vets').then(function (resp) {
                self.vetList = resp.data;
            });
        }

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

        load();
    }]);
