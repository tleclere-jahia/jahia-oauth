package org.jahia.modules.jahiaoauth.service;

import java.io.IOException;

/**
 * @author dgaillard
 */
public interface JahiaOAuth {
    static final String JAHIA_OAUTH = "jahiaOAuth";

    String getAuthorizationUrl(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope) throws Exception;

    void storeTokenAndExecuteMapper(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope, String token) throws Exception;
}
