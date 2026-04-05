'use strict';

angular.module('visits', ['ui.router', 'core'])
    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('visits', {
                parent: 'shell',
                url: '/owners/:ownerId/pets/:petId/visits',
                template: '<visits></visits>'
            })
    }]);
