'use strict';

angular.module('core')
    .constant('ApiConfig', {
        // Pet API
        PET_DETAILS: '/api/owners/',
        
        // Visit API
        VISITS: '/api/visit/owners/',
        
        // AI Diagnosis API
        DIAGNOSIS: '/api/genai/diagnosis',
        
        // Feedback API
        FEEDBACK: '/api/genai/diagnosis/'
    });
