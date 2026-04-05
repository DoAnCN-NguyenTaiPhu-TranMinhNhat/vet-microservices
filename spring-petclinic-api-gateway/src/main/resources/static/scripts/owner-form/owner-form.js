'use strict';

angular.module('ownerForm', ['ui.router'])
    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('ownerNew', {
                parent: 'shell',
                url: '/owners/new',
                template: '<owner-form></owner-form>'
            })
            .state('ownerEdit', {
                parent: 'shell',
                url: '/owners/:ownerId/edit',
                template: '<owner-form></owner-form>'
            })
    }]);
