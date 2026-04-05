'use strict';

angular.module('clinicAuth', ['ui.router', 'core'])
    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('login', {
                parent: 'root',
                url: '/login?returnTo',
                params: { returnTo: null },
                template: '<clinic-login></clinic-login>'
            })
            .state('register', {
                parent: 'root',
                url: '/register',
                template: '<clinic-register></clinic-register>'
            });
    }]);
