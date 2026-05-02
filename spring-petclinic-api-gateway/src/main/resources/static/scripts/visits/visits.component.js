'use strict';

angular.module('visits')
    .component('visits', {
        // Leading slash = always resolved against gateway host; ?v= bumps browser + Angular $templateCache after deploys.
        templateUrl: '/scripts/visits/visits.template.html?v=5',
        controller: 'VisitsController'
    });
