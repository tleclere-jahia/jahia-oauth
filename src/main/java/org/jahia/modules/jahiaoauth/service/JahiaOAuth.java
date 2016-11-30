package org.jahia.modules.jahiaoauth.service;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author dgaillard
 */
public interface JahiaOAuth {
    String getAuthorizationUrl(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope, String state) throws Exception;

    void storeTokenAndExecuteMapper(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope, String state, String token) throws Exception;

    void addDataToOAuthBaseApiMap(Map<String, Map<String, Object>> dataToLoad, String serviceName);

    void addDataToOAuthMapperPropertiesMap(Map<String, Map<String, Object>> mapperProperties, String mapperKey);

    JSONObject getConnectorProperties(String serviceName) throws JSONException;

    JSONObject getMapperProperties(String mapperKey) throws JSONException;

    String resolveConnectorNodeName(String serviceName);
}
