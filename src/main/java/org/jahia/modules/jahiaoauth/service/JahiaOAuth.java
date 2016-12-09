package org.jahia.modules.jahiaoauth.service;

import org.jahia.services.content.JCRNodeWrapper;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dgaillard
 */
public interface JahiaOAuth {
    String getAuthorizationUrl(JCRNodeWrapper jahiaOAuthNode, String serviceName, String sessionId) throws RepositoryException;

    void extractTokenAndExecuteMappers(JCRNodeWrapper jahiaOAuthNode, String serviceName, String token, String state) throws Exception;

    void addDataToOAuthBaseApiMap(Map<String, Map<String, Object>> dataToLoad, String serviceName);

    void addDataToOAuthMapperPropertiesMap(Map<String, Map<String, Object>> mapperProperties, String mapperServiceName);

    JSONObject getConnectorProperties(String serviceName) throws JSONException;

    JSONObject getMapperProperties(String mapperServiceName) throws JSONException;

    HashMap<String, Object> getMapperResults(String mapperServiceName, String sessionId);

    void updateCacheEntry(String originalSessionId, String newSessionId);
}
