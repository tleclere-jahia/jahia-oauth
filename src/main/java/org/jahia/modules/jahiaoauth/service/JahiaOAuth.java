package org.jahia.modules.jahiaoauth.service;

import java.util.Map;

/**
 * @author dgaillard
 */
public interface JahiaOAuth {
    String getAuthorizationUrl(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope) throws Exception;

    void storeTokenAndExecuteMapper(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope, String token) throws Exception;

    void addDataToOAuthBaseApiMap(Map<String, Map<String, Object>> dataToLoad, String serviceName);
}
