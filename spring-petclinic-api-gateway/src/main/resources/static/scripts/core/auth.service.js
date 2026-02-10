'use strict';

angular.module('core')
    .service('AuthService', ['$http', '$q', function($http, $q) {
        var self = this;
        
        self.getCurrentUser = function() {
            return $http.get('/api/user/current')
                .then(function(response) {
                    return response.data;
                })
                .catch(function(error) {
                    console.error('Failed to get current user:', error);
                    // No fallback - let the application handle authentication failure
                    return $q.reject(error);
                });
        };
        
        self.getCurrentVeterinarian = function() {
            return self.getCurrentUser()
                .then(function(user) {
                    if (user && user.veterinarianId) {
                        return user.veterinarianId;
                    }
                    return $q.reject(new Error('No veterinarian ID found in current session'));
                })
                .catch(function(error) {
                    console.error('Failed to get current veterinarian:', error);
                    return $q.reject(error);
                });
        };
    }]);
