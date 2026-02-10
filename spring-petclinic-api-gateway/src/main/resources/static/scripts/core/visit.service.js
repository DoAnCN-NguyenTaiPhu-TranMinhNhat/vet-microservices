'use strict';

angular.module('core')
    .service('VisitService', ['$http', '$q', 'ApiConfig', function($http, $q, ApiConfig) {
        var self = this;
        
        self.getVisits = function(ownerId, petId) {
            // Backend uses pattern: owners/{ownerId}/pets/{petId}/visits
            // Pet ID 0 is now supported
            var url = '/api/visit/owners/' + ownerId + '/pets/' + petId + '/visits';
            console.log('VisitService.getVisits: calling URL =', url, 'ownerId =', ownerId, 'petId =', petId);
            return $http.get(url)
                .then(function(response) {
                    return response.data;
                })
                .catch(function(error) {
                    console.error('Failed to load visits:', error);
                    return $q.reject(error);
                });
        };
        
        self.createVisit = function(ownerId, petId, visitData) {
            // Backend uses pattern: owners/{ownerId}/pets/{petId}/visits
            // Pet ID 0 is now supported
            var url = '/api/visit/owners/' + ownerId + '/pets/' + petId + '/visits';
            console.log('VisitService.createVisit: calling URL =', url, 'ownerId =', ownerId, 'petId =', petId);
            return $http.post(url, visitData)
                .then(function(response) {
                    return response.data; // Return created visit with ID
                })
                .catch(function(error) {
                    console.error('Failed to create visit:', error);
                    return $q.reject(error);
                });
        };
    }]);
