'use strict';

angular.module('vetList', ['ui.router', 'core'])
    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('vets', {
                parent: 'shell',
                url: '/vets',
                template: '<vet-list></vet-list>'
            })
    }]);