'use strict';

angular.module('core')
    .service('AuthService', ['$http', '$q', function ($http, $q) {
        var self = this;
        var TOKEN_KEY = 'vet_clinic_jwt';
        var USER_KEY = 'vet_clinic_user';

        self.getToken = function () {
            try {
                return localStorage.getItem(TOKEN_KEY);
            } catch (e) {
                return null;
            }
        };

        self.getStoredUser = function () {
            try {
                var raw = localStorage.getItem(USER_KEY);
                return raw ? JSON.parse(raw) : null;
            } catch (e) {
                return null;
            }
        };

        self.setSession = function (authResponse) {
            if (!authResponse || !authResponse.accessToken) {
                return;
            }
            localStorage.setItem(TOKEN_KEY, authResponse.accessToken);
            if (authResponse.user) {
                localStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
            }
        };

        self.logout = function () {
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(USER_KEY);
        };

        self.login = function (email, password) {
            return $http.post('/api/customer/auth/login', { email: email, password: password })
                .then(function (response) {
                    self.setSession(response.data);
                    return response.data;
                });
        };

        self.register = function (payload) {
            return $http.post('/api/customer/auth/register', payload)
                .then(function (response) {
                    self.setSession(response.data);
                    return response.data;
                });
        };

        self.getCurrentUser = function () {
            return $http.get('/api/user/current')
                .then(function (response) {
                    return response.data;
                })
                .catch(function (error) {
                    console.error('Failed to get current user:', error);
                    return $q.reject(error);
                });
        };

        /** Loads clinic user from customers DB (incl. clinicAdmin) and updates vet_clinic_user — fixes UI after backend adds fields. */
        self.refreshStoredUserFromServer = function () {
            if (!self.getToken()) {
                return $q.resolve(null);
            }
            return $http.get('/api/customer/auth/me')
                .then(function (response) {
                    if (response.data) {
                        localStorage.setItem(USER_KEY, JSON.stringify(response.data));
                    }
                    return response.data;
                })
                .catch(function () {
                    return null;
                });
        };

        self.getCurrentVeterinarian = function () {
            return self.getCurrentUser()
                .then(function (user) {
                    if (user && user.veterinarianId != null && Number(user.veterinarianId) > 0) {
                        return user.veterinarianId;
                    }
                    return $q.reject(new Error('No veterinarian ID found in current session'));
                })
                .catch(function (error) {
                    console.error('Failed to get current veterinarian:', error);
                    return $q.reject(error);
                });
        };
    }]);
