'use strict';

angular.module('ownerList')
    .controller('OwnerListController', ['$http', function ($http) {
        var self = this;

        var u = {};
        try {
            u = JSON.parse(localStorage.getItem('vet_clinic_user') || '{}');
        } catch (e) {
            u = {};
        }
        var q = (u && u.clinicId != null && u.clinicId !== '')
            ? ('?clinicId=' + encodeURIComponent(u.clinicId))
            : '';

        $http.get('api/customer/owners' + q).then(function (resp) {
            self.owners = resp.data;
        });
    }]);
