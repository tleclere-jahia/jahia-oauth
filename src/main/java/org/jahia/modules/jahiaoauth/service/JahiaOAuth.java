package org.jahia.modules.jahiaoauth.service;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * @author dgaillard
 */
public interface JahiaOAuth {
    String getAuthorizationUrl(JCRNodeWrapper jahiaOAuthNode, String serviceName) throws RepositoryException;

    void extractTokenAndExecuteMappers(JCRSessionWrapper session, JCRNodeWrapper jahiaOAuthNode, String serviceName, String token) throws Exception;

    void addDataToOAuthBaseApiMap(Map<String, Map<String, Object>> dataToLoad, String serviceName);

    void addDataToOAuthMapperPropertiesMap(Map<String, Map<String, Object>> mapperProperties, String mapperKey, String mapperServiceName);

    JSONObject getConnectorProperties(String serviceName) throws JSONException;

    JSONObject getMapperProperties(String mapperKey) throws JSONException;

    String resolveConnectorNodeName(String serviceName);
}
