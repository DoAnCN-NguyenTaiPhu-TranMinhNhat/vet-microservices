'use strict';

/**
 * Attaches Bearer JWT to every $http request when a token is present.
 */
angular.module('infrastructure')
    .factory('AuthRequestInterceptor', ['$injector', function ($injector) {
        return {
            request: function (config) {
                var AuthService = $injector.get('AuthService');
                var token = AuthService.getToken();
                if (token) {
                    config.headers = config.headers || {};
                    if (!config.headers.Authorization) {
                        config.headers.Authorization = 'Bearer ' + token;
                    }
                }
                return config;
            }
        };
    }]);
