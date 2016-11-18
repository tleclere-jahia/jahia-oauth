angular.module('JahiaOAuth', ['ngMaterial'])
    .config(function($mdThemingProvider, $mdToastProvider) {
        $mdThemingProvider.theme('jahiaOAuth')
            .primaryPalette('blue-grey')
            .accentPalette('blue')
            .warnPalette('red');

        $mdThemingProvider.setDefaultTheme('jahiaOAuth');


        // TODO make it work
        $mdToastProvider.addPreset('errorToast', {
            argOption: 'textContent',
            methods: ['textContent', 'content', 'action', 'highlightAction', 'highlightClass', 'theme', 'parent' ],
            options: ["$mdToast", "$mdTheming", function($mdToast, $mdTheming) {
                return {
                    template:
                    '<md-toast md-theme="{{ toast.theme }}" ng-class="{\'md-capsule\': toast.capsule}">' +
                    '  <div class="md-toast-content">' +
                    '    <span class="md-toast-text" role="alert" aria-relevant="all" aria-atomic="true">' +
                    '      {{ toast.content }}' +
                    '    </span>' +
                    '  </div>' +
                    '</md-toast>',
                    theme: $mdTheming.defaultTheme(),
                    toastClass: 'md-warn',
                    position: 'bottom right',
                    hideDelay: 6000,
                    controllerAs: 'toast',
                    bindToController: true
                }
            }]
        });
        // .addPreset('simple', {
        //     argOption: 'textContent',
        //     methods: ['textContent', 'content', 'action', 'highlightAction', 'highlightClass', 'theme', 'parent' ],
        //     options: /* @ngInject */ ["$mdToast", "$mdTheming", function($mdToast, $mdTheming) {
        //         return {
        //             template:
        //             '<md-toast md-theme="{{ toast.theme }}" ng-class="{\'md-capsule\': toast.capsule}">' +
        //             '  <div class="md-toast-content">' +
        //             '    <span class="md-toast-text" role="alert" aria-relevant="all" aria-atomic="true">' +
        //             '      {{ toast.content }}' +
        //             '    </span>' +
        //             '    <md-button class="md-action" ng-if="toast.action" ng-click="toast.resolve()" ' +
        //             '        ng-class="highlightClasses">' +
        //             '      {{ toast.action }}' +
        //             '    </md-button>' +
        //             '  </div>' +
        //             '</md-toast>',
        //             controller: /* @ngInject */ ["$scope", function mdToastCtrl($scope) {
        //                 var self = this;
        //
        //                 if (self.highlightAction) {
        //                     $scope.highlightClasses = [
        //                         'md-highlight',
        //                         self.highlightClass
        //                     ]
        //                 }
        //
        //                 $scope.$watch(function() { return activeToastContent; }, function() {
        //                     self.content = activeToastContent;
        //                 });
        //
        //                 this.resolve = function() {
        //                     $mdToast.hide( ACTION_RESOLVE );
        //                 };
        //             }],
        //             theme: $mdTheming.defaultTheme(),
        //             controllerAs: 'toast',
        //             bindToController: true
        //         };
        //     }]
        // });
    })
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
    }]);
