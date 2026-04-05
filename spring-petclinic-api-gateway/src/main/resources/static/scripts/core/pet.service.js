'use strict';

angular.module('core')
    .service('PetService', ['$http', '$q', 'ApiConfig', function($http, $q, ApiConfig) {
        var self = this;
        
        self.getPetDetails = function(petId, ownerId) {
            console.log('PetService: Looking for petId:', petId, 'in ownerId:', ownerId);
            
            // Use the same API endpoint as owner details controller
            return $http.get('/api/customer/owners/' + ownerId)
                .then(function(response) {
                    var owner = response.data;
                    console.log('PetService: Owner data loaded, pets count:', owner.pets ? owner.pets.length : 0);
                    
                    // Search through owner's pets
                    if (owner.pets) {
                        for (var j = 0; j < owner.pets.length; j++) {
                            var pet = owner.pets[j];
                            console.log('PetService: Checking pet - ID:', pet.id, 'Name:', pet.name);
                            if (String(pet.id) === String(petId)) {
                                console.log('PetService: Found matching pet:', pet.name);
                                return pet;
                            }
                        }
                    }
                    
                    console.log('PetService: Pet not found. Available pet IDs:', 
                        owner.pets ? owner.pets.map(p => p.id) : 'none');
                    throw new Error('Pet not found with ID: ' + petId);
                })
                .catch(function(error) {
                    console.error('Failed to get pet details:', error);
                    return $q.reject(error);
                });
        };
        
        self.calculateAgeInMonths = function(birthDate) {
            if (!birthDate) return null;
            
            var birth = new Date(birthDate);
            var today = new Date();
            
            var months = (today.getFullYear() - birth.getFullYear()) * 12 + 
                       (today.getMonth() - birth.getMonth());
            
            return months;
        };
        
        self.getCurrentSeason = function() {
            var month = new Date().getMonth();
            
            if (month >= 2 && month <= 4) return 'Spring';
            if (month >= 5 && month <= 7) return 'Summer';
            if (month >= 8 && month <= 10) return 'Fall';
            return 'Winter';
        };
    }]);
