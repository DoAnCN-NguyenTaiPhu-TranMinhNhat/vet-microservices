'use strict';

angular.module('ownerDetails')
    .controller('OwnerDetailsController', ['$http', '$stateParams', function ($http, $stateParams) {
        var self = this;

        $http.get('api/customer/owners/' + $stateParams.ownerId).then(function (resp) {
            self.owner = resp.data;
            console.log('OwnerDetailsController: Owner loaded:', self.owner);
            console.log('OwnerDetailsController: Pets data:', self.owner.pets);
            if (self.owner.pets) {
                self.owner.pets.forEach(function(pet, index) {
                    console.log('OwnerDetailsController: Pet[' + index + '] - ID:', pet.id, 'Name:', pet.name);
                });
            }
        });
    }]);
