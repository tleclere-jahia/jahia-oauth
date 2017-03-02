(function() {
    'use strict';

    angular.module('JahiaOAuthApp', ['ngMaterial', 'ngRoute', 'ngAnimate', 'ngMessages', 'i18n']);

    angular.module('JahiaOAuthApp').config(jahiaOAuthAppConfig);

    jahiaOAuthAppConfig.$inject = ['$mdThemingProvider', '$routeProvider'];

    function jahiaOAuthAppConfig($mdThemingProvider, $routeProvider) {
        $routeProvider
            .when('/connectors', {
                templateUrl: 'connectors.html'
            }).when('/mappers/:connectorServiceName', {
                templateUrl: 'mappers.html'
            }).otherwise('/connectors');

        $mdThemingProvider.theme('error-toast');
        $mdThemingProvider.theme('success-toast');

        $mdThemingProvider.theme('JahiaOAuthApp')
            .primaryPalette('blue-grey')
            .accentPalette('blue')
            .warnPalette('red');

        $mdThemingProvider.setDefaultTheme('JahiaOAuthApp');
    }
})();