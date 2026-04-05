'use strict';

angular.module('clinicAuth')
    .component('clinicRegister', {
        templateUrl: 'scripts/clinic-auth/clinic-register.template.html',
        controller: ['AuthService', '$state', function (AuthService, $state) {
            var vm = this;
            vm.clinicName = '';
            vm.clinicPhone = '';
            vm.clinicAddress = '';
            vm.displayName = '';
            vm.email = '';
            vm.password = '';
            vm.veterinarianId = null;
            vm.error = '';

            vm.submit = function () {
                vm.error = '';
                var payload = {
                    clinicName: vm.clinicName,
                    email: vm.email,
                    password: vm.password,
                    displayName: vm.displayName
                };
                if (vm.clinicPhone) {
                    payload.clinicPhone = vm.clinicPhone;
                }
                if (vm.clinicAddress) {
                    payload.clinicAddress = vm.clinicAddress;
                }
                if (vm.veterinarianId != null && vm.veterinarianId !== '') {
                    payload.veterinarianId = Number(vm.veterinarianId);
                }
                AuthService.register(payload)
                    .then(function () {
                        $state.go('owners');
                    })
                    .catch(function (err) {
                        var body = err.data || {};
                        vm.error = body.message || body.error || err.statusText || 'Registration failed';
                    });
            };
        }],
        controllerAs: 'vm'
    });
