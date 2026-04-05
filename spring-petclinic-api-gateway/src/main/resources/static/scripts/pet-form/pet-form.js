'use strict';

angular.module('petForm', ['ui.router'])
    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('petNew', {
                parent: 'shell',
                url: '/owners/:ownerId/new-pet',
                template: '<pet-form></pet-form>'
            })
            .state('petEdit', {
                parent: 'shell',
                url: '/owners/:ownerId/pets/:petId',
                template: '<pet-form></pet-form>'
            })
    }]);
