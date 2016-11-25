angular.module('JahiaOAuth', ['ngMaterial', 'ngRoute', 'ngAnimate', 'ngMessages'])
    .config(function($mdThemingProvider, $mdToastProvider, $routeProvider) {
        $routeProvider
            .when('/connectors', {
                templateUrl: 'connectors.html'
            }).when('/mappers/:connectorNodeName', {
                templateUrl: 'mappers.html'
            }).otherwise('/connectors');

        $mdThemingProvider.theme('jahiaOAuth')
            .primaryPalette('blue-grey')
            .accentPalette('blue')
            .warnPalette('red');

        $mdThemingProvider.theme('errorToast')
            .backgroundPalette('red');

        $mdThemingProvider.setDefaultTheme('jahiaOAuth');

        // TODO make it works
        // $mdToastProvider.addPreset('errorToast', {
        //     argOption: 'textContent',
        //     methods: ['textContent', 'content', 'action', 'highlightAction', 'highlightClass', 'theme', 'parent' ],
        //     options: ["$mdToast", "$mdTheming", function($mdToast, $mdTheming) {
        //         return {
        //             template:
        //             '<md-toast md-theme="{{ toast.theme }}" ng-class="{\'md-capsule\': toast.capsule}">' +
        //             '   <div class="md-toast-content">' +
        //             '       <span class="md-toast-text" role="alert" aria-relevant="all" aria-atomic="true">' +
        //             '           {{ toast.content }}' +
        //             '       </span>' +
        //             '   </div>' +
        //             '</md-toast>',
        //             theme: $mdTheming.defaultTheme(),
        //             toastClass: 'md-warn',
        //             position: 'bottom right',
        //             hideDelay: 6000,
        //             controllerAs: 'toast',
        //             bindToController: true
        //         }
        //     }]
        // });
    })
    .controller('headerController', ['$scope', '$location', function ($scope, $location) {
        $scope.isMapperView = function() {
            return $location.path() != '/connectors';
        };

        $scope.goToConnectors = function() {
            $location.path('/connectors');
        }
    }])
    .service('settingsService', ['$http', function($http) {
        this.getConnectorData = function(nodeName, properties) {
            var propertiesAsString = '';
            angular.forEach(properties, function(property) {
                propertiesAsString += '&properties=' + property;
            });
            return $http.get(jahiaOAuthContext.baseEdit + jahiaOAuthContext.sitePath + '.manageConnectorsSettingsAction.do?nodeName=' + nodeName + propertiesAsString);
        };

        this.setConnectorData = function(data) {
            return $http({
                method: 'POST',
                url: jahiaOAuthContext.baseEdit + jahiaOAuthContext.sitePath + '.manageConnectorsSettingsAction.do',
                params: data
            });
        };

        this.toggleMapper = function(data) {
            return $http({
                method: 'POST',
                url: jahiaOAuthContext.baseEdit + jahiaOAuthContext.sitePath + '.manageMappersAction.do',
                params: data
            })
        };
    }]);
