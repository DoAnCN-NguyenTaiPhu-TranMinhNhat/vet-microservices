'use strict';

angular.module('ownerDetails', ['ui.router'])
    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('ownerDetails', {
                parent: 'shell',
                url: '/owners/details/:ownerId',
                template: '<owner-details></owner-details>'
            })
    }]);