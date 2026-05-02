'use strict';

angular.module('core')
    .constant('ApiConfig', {
        // Pet API
        PET_DETAILS: '/api/owners/',
        
        // Visit API
        VISITS: '/api/visit/owners/',
        
        // AI Diagnosis API
        DIAGNOSIS: '/api/genai/diagnosis',
        /** Vet-AI predict model list (via genai proxy): GET + optional ?clinicId= */
        DIAGNOSIS_MODELS: '/api/genai/diagnosis/models',
        /** Pin clinic active model (Vet-AI); POST JSON { modelVersion, clinicId? } */
        DIAGNOSIS_MODELS_ACTIVE: '/api/genai/diagnosis/models/active',
        
        // Feedback API
        FEEDBACK: '/api/genai/diagnosis/'
    });
