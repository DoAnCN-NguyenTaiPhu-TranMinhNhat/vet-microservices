'use strict';

angular.module('clinicAuth')
    .component('clinicLogin', {
        templateUrl: 'scripts/clinic-auth/clinic-login.template.html',
        controller: ['AuthService', '$state', '$stateParams', function (AuthService, $state, $stateParams) {
            var vm = this;
            vm.email = '';
            vm.password = '';
            vm.error = '';

            vm.submit = function () {
                vm.error = '';
                AuthService.login(vm.email, vm.password)
                    .then(function () {
                        var dest = $stateParams.returnTo || 'owners';
                        $state.go(dest);
                    })
                    .catch(function (err) {
                        var body = err.data || {};
                        vm.error = body.message || body.error || err.statusText || 'Sign in failed';
                    });
            };
        }],
        controllerAs: 'vm'
    });
