(function() {
    'use strict';

    angular.module('JahiaOAuthApp').controller('GoogleController', GoogleController);

    GoogleController.$inject = ['$location', 'settingsService', 'helperService', 'i18nService'];

    function GoogleController($location, settingsService, helperService, i18nService) {
        var vm = this;

        // Variables
        vm.expandedCard = false;
        vm.callbackUrl = '';

        // Functions
        vm.saveSettings = saveSettings;
        vm.goToMappers = goToMappers;
        vm.toggleCard = toggleCard;

        init();

        function saveSettings() {
            // Value can't be empty
            if (!vm.apiKey
                || !vm.apiSecret
                || !vm.callbackUrl) {
                helperService.errorToast(i18nService.message('joant_googleOAuthView.message.error.missingMandatoryProperties'));
                return false;
            }

            // the node name here must be the same as the one in your spring file
            settingsService.setConnectorData({
                connectorServiceName: 'GoogleApi20',
                properties: {
                    enabled: vm.enabled,
                    apiKey: vm.apiKey,
                    apiSecret: vm.apiSecret,
                    callbackUrl: vm.callbackUrl,
                    scope: vm.scope
                }
            }).success(function() {
                vm.connectorHasSettings = true;
                helperService.successToast(i18nService.message('joant_googleOAuthView.message.succes.saveSuccess'));
            }).error(function(data) {
                helperService.errorToast(i18nService.message('joant_googleOAuthView.message.label') + ' ' + data.error);
                console.log(data);
            });
        }

        function goToMappers() {
            // the second part of the path must be the service name
            $location.path('/mappers/GoogleApi20');
        }

        function toggleCard() {
            vm.expandedCard = !vm.expandedCard;
        }

        function init() {
            i18nService.addKey(oauthi18n);

            settingsService.getConnectorData('GoogleApi20', ['enabled', 'apiKey', 'apiSecret', 'callbackUrl', 'scope']).success(function(data) {
                if (data && !angular.equals(data, { })) {
                    vm.connectorHasSettings = true;
                    vm.enabled = data.enabled;
                    vm.apiKey = data.apiKey;
                    vm.apiSecret = data.apiSecret;
                    vm.callbackUrl = data.callbackUrl;
                    vm.scope = data.scope
                    vm.expandedCard = true;
                } else {
                    vm.connectorHasSettings = false;
                    vm.enabled = false;
                }
            }).error(function(data) {
                helperService.errorToast(i18nService.message('joant_googleOAuthView.message.label') + ' ' + data.error);
            });
        }
    }
})();
