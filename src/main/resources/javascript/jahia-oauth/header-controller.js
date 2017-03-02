(function() {
    'use strict';

    angular.module('JahiaOAuthApp').controller('HeaderController', HeaderController);

    HeaderController.$inject = ['$location', 'i18nService'];

    function HeaderController($location, i18nService) {
        var vm = this;

        // Functions
        vm.isMapperView = isMapperView;
        vm.goToConnectors = goToConnectors;

        init();

        function isMapperView() {
            return $location.path() != '/connectors';
        }

        function goToConnectors() {
            $location.path('/connectors');
        }

        function init() {
            i18nService.addKey(joai18n);
        }
    }
})();