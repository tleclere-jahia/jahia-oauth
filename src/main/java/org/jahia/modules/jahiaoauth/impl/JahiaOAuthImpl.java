/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.apache.commons.lang.StringUtils;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.jahia.modules.jahiaoauth.cache.JahiaOAuthCacheManager;
import org.jahia.modules.jahiaoauth.service.Constants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuth;
import org.jahia.modules.jahiaoauth.service.Mapper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author dgaillard
 */
public class JahiaOAuthImpl implements JahiaOAuth, BundleContextAware {
    private static final Logger logger = LoggerFactory.getLogger(JahiaOAuthImpl.class);

    private BundleContext bundleContext;
    private Map<String, Map<String, Object>> oAuthBase20ApiMap;
    private Map<String, Map<String, Object>> oAuthMapperPropertiesMap;
    private JahiaOAuthCacheManager jahiaOAuthCacheManager;

    @Override
    public String getAuthorizationUrl(JCRNodeWrapper jahiaOAuthNode, String serviceName, String sessionId) throws RepositoryException {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(serviceName);
        OAuth20Service service = createOAuth20Service(connectorNode, serviceName, sessionId);

        return service.getAuthorizationUrl();
    }

    @Override
    public void extractTokenAndExecuteMappers(JCRNodeWrapper jahiaOAuthNode, String serviceName, String token, String state) throws Exception {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(serviceName);
        OAuth20Service service = createOAuth20Service(connectorNode, serviceName, state);
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        // Request all the properties available right now
        List<Map<String, Object>> properties = (List<Map<String, Object>>) oAuthBase20ApiMap.get(serviceName).get(Constants.PROPERTIES);
        String protectedResourceUrl = (String) oAuthBase20ApiMap.get(serviceName).get(Constants.PROTECTED_RESOURCE_URL);
        if ((boolean) oAuthBase20ApiMap.get(serviceName).get(Constants.URL_CAN_TAKE_VALUE)) {
            StringBuilder propertiesAsString = new StringBuilder();
            boolean asPrevious = false;
            for (Map<String, Object> entry : properties) {
                if ((boolean) entry.get(Constants.CAN_BE_REQUESTED)) {
                    if (asPrevious) {
                        propertiesAsString.append(",");
                    }
                    propertiesAsString.append(entry.get(Constants.PROPERTY_NAME));
                    asPrevious = true;
                } else {
                    String propertyToRequest = (String) entry.get(Constants.PROPERTY_TO_REQUEST);
                    if (!StringUtils.contains(propertiesAsString.toString(), propertyToRequest)) {
                        if (asPrevious) {
                            propertiesAsString.append(",");
                        }
                        propertiesAsString.append(propertyToRequest);
                        asPrevious = true;
                    } else {
                        asPrevious = false;
                    }
                }
            }
            protectedResourceUrl = String.format(protectedResourceUrl, propertiesAsString.toString());
        }
        OAuthRequest request = new OAuthRequest(Verb.GET, protectedResourceUrl, service);
        request.addHeader("x-li-format", "json");
        service.signRequest(accessToken, request);
        Response response = request.send();

        // if we got the properties then execute mapper
        if (response.getCode() == HttpServletResponse.SC_OK) {
            try {
                JSONObject responseJson = new JSONObject(response.getBody());
                logger.debug(responseJson.toString());

                // Store in a simple map the results by properties as mapped in the connector
                HashMap<String, Object> propertiesResult = new HashMap<>();
                for (Map<String, Object> entry : properties) {
                    String propertyName = (String) entry.get(Constants.PROPERTY_NAME);
                    if ((boolean) entry.get(Constants.CAN_BE_REQUESTED) && responseJson.has(propertyName)) {
                        propertiesResult.put(propertyName, responseJson.get(propertyName));
                    } else {
                        String propertyToRequest = (String) entry.get(Constants.PROPERTY_TO_REQUEST);
                        if (responseJson.has(propertyToRequest)) {
                            extractPropertyFromJSON(propertiesResult, responseJson.getJSONObject(propertyToRequest), null, (String) entry.get(Constants.VALUE_PATH), propertyName);
                        }
                    }
                }

                // Get Mappers node
                JCRNodeIteratorWrapper mappersNi = connectorNode.getNode(Constants.MAPPERS_NODE_NAME).getNodes();
                while (mappersNi.hasNext()) {
                    JCRNodeWrapper mapperNode = (JCRNodeWrapper) mappersNi.nextNode();
                    // make sure mappers is activate
                    if (mapperNode.getProperty(Constants.PROPERTY_IS_ACTIVATE).getBoolean()) {
                        HashMap<String, Object> mapperResult = new HashMap<>();
                        // add token to result
                        mapperResult.put(Constants.TOKEN, token);

                        JSONArray jsonArray = new JSONArray(mapperNode.getPropertyAsString(Constants.PROPERTY_MAPPING));

                        for (int i = 0 ; i < jsonArray.length() ; i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            JSONObject mapper = jsonObject.getJSONObject(Constants.MAPPER);
                            JSONObject connector = jsonObject.getJSONObject(Constants.CONNECTOR);
                            if (mapper.getBoolean(Constants.PROPERTY_MANDATORY) && !propertiesResult.containsKey(connector.getString(Constants.PROPERTY_NAME))) {
                                logger.error("JSON response was: " + responseJson.toString());
                                throw new RepositoryException("Could not execute mapper: missing mandatory property");
                            }
                            if (propertiesResult.containsKey(connector.getString(Constants.PROPERTY_NAME))) {
                                mapperResult.put(mapper.getString(Constants.PROPERTY_NAME), propertiesResult.get(connector.getString(Constants.PROPERTY_NAME)));
                            }
                        }

                        jahiaOAuthCacheManager.cacheMapperResults(oAuthMapperPropertiesMap.get(mapperNode.getName()).get(Constants.MAPPER_SERVICE_NAME) + "_" + state, mapperResult);
                        String filter = "(" + Constants.MAPPER_SERVICE_NAME + "=" + oAuthMapperPropertiesMap.get(mapperNode.getName()).get(Constants.MAPPER_SERVICE_NAME) + ")";
                        ServiceReference[] serviceReference = bundleContext.getServiceReferences(Mapper.class.getName(), filter);
                        if (serviceReference != null) {
                            Mapper mapper = (Mapper) bundleContext.getService(serviceReference[0]);
                            mapper.executeMapper(mapperResult);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(response.getBody());
                throw e;
            }
        } else {
            logger.error(response.getBody());
            throw new OAuthException("Error throw by the server when trying to get data");
        }
    }

    private void extractPropertyFromJSON(HashMap<String, Object> propertiesResult, JSONObject jsonObject, JSONArray jsonArray, String pathToProperty, String propertyName) throws JSONException {
        if (StringUtils.startsWith(pathToProperty, "/")) {

            String key = StringUtils.substringAfter(pathToProperty, "/");
            String potentialKey1 = StringUtils.substringBefore(key, "[");
            String potentialKey2 = StringUtils.substringBefore(key, "/");

            if (potentialKey1.length() <= potentialKey2.length()) {
                key = potentialKey1;
            } else if (potentialKey1.length() > potentialKey2.length()) {
                key = potentialKey2;
            }

            pathToProperty = StringUtils.substringAfter(pathToProperty, "/" + key);

            if (StringUtils.isBlank(pathToProperty) && jsonObject.has(key)) {
                propertiesResult.put(propertyName, jsonObject.get(key));
            } else {
                if (StringUtils.startsWith(pathToProperty, "/") && jsonObject.has(key)) {
                    extractPropertyFromJSON(propertiesResult, jsonObject.getJSONObject(key), null, pathToProperty, propertyName);
                } else if (jsonObject.has(key)) {
                    extractPropertyFromJSON(propertiesResult, null, jsonObject.getJSONArray(key), pathToProperty, propertyName);
                }
            }
        } else {
            int arrayIndex = new Integer(StringUtils.substringBetween(pathToProperty, "[", "]"));
            pathToProperty = StringUtils.substringAfter(pathToProperty, "]");
            if (StringUtils.isBlank(pathToProperty) && jsonArray.length() >= arrayIndex) {
                propertiesResult.put(propertyName, jsonArray.get(arrayIndex));
            } else {
                if (StringUtils.startsWith(pathToProperty, "/") && jsonArray.length() >= arrayIndex) {
                    extractPropertyFromJSON(propertiesResult, jsonArray.getJSONObject(arrayIndex), null, pathToProperty, propertyName);
                } else if (jsonArray.length() >= arrayIndex) {
                    extractPropertyFromJSON(propertiesResult, null, jsonArray.getJSONArray(arrayIndex), pathToProperty, propertyName);
                }
            }
        }
    }

    private OAuth20Service createOAuth20Service(JCRNodeWrapper connectorNode, String serviceName, String state) throws RepositoryException {
        ServiceBuilder serviceBuilder = new ServiceBuilder()
                .apiKey(connectorNode.getPropertyAsString(Constants.PROPERTY_API_KEY))
                .apiSecret(connectorNode.getPropertyAsString(Constants.PROPERTY_API_SECRET))
                .callback(connectorNode.getPropertyAsString(Constants.PROPERTY_CALLBACK_URL))
                .state(state);

        if (connectorNode.hasProperty(Constants.PROPERTY_SCOPE) && StringUtils.isNotBlank(connectorNode.getPropertyAsString(Constants.PROPERTY_SCOPE))) {
            serviceBuilder.scope(connectorNode.getPropertyAsString(Constants.PROPERTY_SCOPE));
        }

        return serviceBuilder.build((BaseApi<? extends OAuth20Service>) oAuthBase20ApiMap.get(serviceName).get(Constants.API));
    }

    @Override
    public void addDataToOAuthBaseApiMap(Map<String, Map<String, Object>> dataToLoad, String serviceName) {
        if (oAuthBase20ApiMap.containsKey(serviceName)) {
            for (Map.Entry<String, Object> entry : dataToLoad.get(serviceName).entrySet()) {
                oAuthBase20ApiMap.get(serviceName).put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void addDataToOAuthMapperPropertiesMap(List<Map<String, Object>> mapperProperties, String mapperServiceName) {
        if (oAuthMapperPropertiesMap == null) {
            oAuthMapperPropertiesMap = new HashMap<>();
        }

        if (!oAuthMapperPropertiesMap.containsKey(mapperServiceName)) {
            oAuthMapperPropertiesMap.put(mapperServiceName, new HashMap<String, Object>());
        }

        if (mapperProperties != null) {
            oAuthMapperPropertiesMap.get(mapperServiceName).put(Constants.PROPERTIES, mapperProperties);
        }
        oAuthMapperPropertiesMap.get(mapperServiceName).put(Constants.MAPPER_SERVICE_NAME, mapperServiceName);
    }

    @Override
    public JSONArray getConnectorProperties(String serviceName) throws JSONException {
        List<Map<String, Object>> list = (List<Map<String, Object>>) oAuthBase20ApiMap.get(serviceName).get(Constants.PROPERTIES);
        JSONArray jsonArray = new JSONArray(list);
        return jsonArray;
    }

    @Override
    public JSONArray getMapperProperties(String mapperServiceName) throws JSONException {
        List<Map<String, Object>> map = (List<Map<String, Object>>) oAuthMapperPropertiesMap.get(mapperServiceName).get(Constants.PROPERTIES);
        JSONArray jsonArray = new JSONArray(map);
        return jsonArray;
    }

    public void setoAuthBase20ApiMap(Map<String, Map<String, Object>> oAuthBase20ApiMap) {
        this.oAuthBase20ApiMap = oAuthBase20ApiMap;
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setJahiaOAuthCacheManager(JahiaOAuthCacheManager jahiaOAuthCacheManager) {
        this.jahiaOAuthCacheManager = jahiaOAuthCacheManager;
    }

    @Override
    public HashMap<String, Object> getMapperResults(String mapperServiceName, String sessionId) {
        return jahiaOAuthCacheManager.getMapperResultsCacheEntry(mapperServiceName + "_" + sessionId);
    }

    @Override
    public void updateCacheEntry(String originalSessionId, String newSessionId) {
        jahiaOAuthCacheManager.updateCacheEntry(originalSessionId, newSessionId);
    }

    @Override
    public String getResultUrl(String siteUrl, Boolean isAuthenticate) {
        return StringUtils.substringBeforeLast(siteUrl, ".html") + "/oauth-result.html?isAuthenticate=" + isAuthenticate;
    }
}
