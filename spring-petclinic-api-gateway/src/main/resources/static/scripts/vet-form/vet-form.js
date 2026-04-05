'use strict';

angular.module('vetForm', ['ui.router', 'core'])
    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('vetNew', {
                parent: 'shell',
                url: '/vets/new',
                template: '<vet-form></vet-form>'
            })
            .state('vetEdit', {
                parent: 'shell',
                url: '/vets/:vetId/edit',
                template: '<vet-form></vet-form>'
            });
    }]);

