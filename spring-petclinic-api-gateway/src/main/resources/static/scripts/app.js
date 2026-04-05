'use strict';
/* App Module */
var petClinicApp = angular.module('petClinicApp', [
    'ui.router', 'infrastructure', 'layoutNav', 'layoutFooter', 'layoutWelcome',
    'core', 'clinicAuth', 'ownerList', 'ownerDetails', 'ownerForm', 'petForm', 'visits', 'vetList', 'vetForm']);

var shellTemplate =
    '<layout-nav></layout-nav>' +
    '<div class="container-fluid">' +
    '<div class="container xd-container"><div ui-view></div></div></div>' +
    '<layout-footer></layout-footer>';

petClinicApp.config(['$stateProvider', '$urlRouterProvider', '$locationProvider', '$httpProvider', function (
    $stateProvider, $urlRouterProvider, $locationProvider, $httpProvider) {

    $httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
    $httpProvider.interceptors.push('AuthRequestInterceptor');
    $httpProvider.interceptors.push('HttpErrorHandlingInterceptor');

    $locationProvider.hashPrefix('!');

    $urlRouterProvider.otherwise('/welcome');
    $stateProvider
        .state('root', {
            abstract: true,
            template: '<ui-view></ui-view>'
        })
        .state('shell', {
            parent: 'root',
            abstract: true,
            template: shellTemplate
        })
        .state('welcome', {
            parent: 'shell',
            url: '/welcome',
            template: '<layout-welcome></layout-welcome>'
        });
}]);

petClinicApp.controller('NavController', ['AuthService', '$state', function (AuthService, $state) {
    var nav = this;
    nav.isLoggedIn = function () {
        return !!AuthService.getToken();
    };
    nav.userLabel = function () {
        var u = AuthService.getStoredUser();
        if (!u) {
            return '';
        }
        var parts = [];
        if (u.clinicName) {
            parts.push(u.clinicName);
        }
        if (u.displayName) {
            parts.push(u.displayName);
        }
        return parts.join(' — ');
    };
    nav.logout = function () {
        AuthService.logout();
        $state.go('welcome');
    };
}]);

petClinicApp.run(['$transitions', 'AuthService', '$state', function ($transitions, AuthService, $state) {
    var publicStates = ['welcome', 'login', 'register'];
    $transitions.onBefore({}, function (trans) {
        var name = trans.to().name;
        if (!name || publicStates.indexOf(name) >= 0) {
            return true;
        }
        if (!AuthService.getToken()) {
            return $state.target('login', { returnTo: name });
        }
    });
}]);

['welcome', 'nav', 'footer'].forEach(function (c) {
    var mod = 'layout' + c.toUpperCase().substring(0, 1) + c.substring(1);
    angular.module(mod, []);
    var opts = { templateUrl: 'scripts/fragments/' + c + '.html' };
    if (c === 'nav') {
        opts.controller = 'NavController';
        opts.controllerAs = 'nav';
    }
    angular.module(mod).component(mod, opts);
});
