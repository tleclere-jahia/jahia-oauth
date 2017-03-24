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
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaoauth.service.*;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author dgaillard
 */
public class JahiaOAuthServiceImpl implements JahiaOAuthService {
    private static final Logger logger = LoggerFactory.getLogger(JahiaOAuthServiceImpl.class);

    private Map<String, BaseApi<? extends OAuth20Service>> oAuthBase20ApiMap;
    private JahiaOAuthCacheService jahiaOAuthCacheService;

    @Override
    public String getAuthorizationUrl(JCRNodeWrapper jahiaOAuthNode, String serviceName, String sessionId) throws RepositoryException {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(serviceName);
        OAuth20Service service = createOAuth20Service(connectorNode, serviceName, sessionId);

        return service.getAuthorizationUrl();
    }

    @Override
    public HashMap<String, Object> getMapperResults(String mapperServiceName, String sessionId) {
        return jahiaOAuthCacheService.getMapperResultsCacheEntry(mapperServiceName + "_" + sessionId);
    }

    @Override
    public String getResultUrl(String siteUrl, Boolean isAuthenticate) {
        return StringUtils.substringBeforeLast(siteUrl, ".html") + "/oauth-result.html?isAuthenticate=" + isAuthenticate;
    }

    @Override
    public void extractTokenAndExecuteMappers(JCRNodeWrapper jahiaOAuthNode, String serviceName, String token, String state) throws Exception {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(serviceName);
        OAuth20Service service = createOAuth20Service(connectorNode, serviceName, state);
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class, "(" + JahiaOAuthConstants.CONNECTOR_SERVICE_NAME + "=" + serviceName + ")");

        if (connectorService == null) {
            logger.error("Connector service was null for service name: " + serviceName);
            throw new JahiaOAuthException("Connector service was null for service name: " + serviceName);
        }

        // Request all the properties available right now
        OAuthRequest request = new OAuthRequest(Verb.GET, connectorService.getProtectedResourceUrl(), service);
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
                List<Map<String, Object>> properties = connectorService.getAvailableProperties();
                for (Map<String, Object> entry : properties) {
                    String propertyName = (String) entry.get(JahiaOAuthConstants.PROPERTY_NAME);
                    if ((boolean) entry.get(JahiaOAuthConstants.CAN_BE_REQUESTED) && responseJson.has(propertyName)) {
                        propertiesResult.put(propertyName, responseJson.get(propertyName));
                    } else {
                        String propertyToRequest = (String) entry.get(JahiaOAuthConstants.PROPERTY_TO_REQUEST);
                        if (responseJson.has(propertyToRequest)) {
                            String pathToProperty = (String) entry.get(JahiaOAuthConstants.VALUE_PATH);
                            if (StringUtils.startsWith(pathToProperty, "/")) {
                                extractPropertyFromJSON(propertiesResult, responseJson.getJSONObject(propertyToRequest), null, pathToProperty, propertyName);
                            } else {
                                extractPropertyFromJSON(propertiesResult, null, responseJson.getJSONArray(propertyToRequest), pathToProperty, propertyName);
                            }
                        }
                    }
                }

                // Get Mappers node
                JCRNodeIteratorWrapper mappersNi = connectorNode.getNode(JahiaOAuthConstants.MAPPERS_NODE_NAME).getNodes();
                while (mappersNi.hasNext()) {
                    JCRNodeWrapper mapperNode = (JCRNodeWrapper) mappersNi.nextNode();
                    // make sure mappers is activate
                    if (mapperNode.getProperty(JahiaOAuthConstants.PROPERTY_IS_ACTIVATE).getBoolean()) {
                        HashMap<String, Object> mapperResult = new HashMap<>();
                        // add token to result
                        mapperResult.put(JahiaOAuthConstants.TOKEN, token);
                        mapperResult.put(JahiaOAuthConstants.CONNECTOR_NAME_AND_ID, serviceName + "_" + propertiesResult.get("id"));

                        JSONArray mapping = new JSONArray(mapperNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_MAPPING));
                        for (int i = 0 ; i < mapping.length() ; i++) {
                            JSONObject jsonObject = mapping.getJSONObject(i);
                            JSONObject mapper = jsonObject.getJSONObject(JahiaOAuthConstants.MAPPER);
                            JSONObject connector = jsonObject.getJSONObject(JahiaOAuthConstants.CONNECTOR);
                            if (mapper.getBoolean(JahiaOAuthConstants.PROPERTY_MANDATORY) && !propertiesResult.containsKey(connector.getString(JahiaOAuthConstants.PROPERTY_NAME))) {
                                logger.error("JSON response was: " + responseJson.toString());
                                throw new RepositoryException("Could not execute mapper: missing mandatory property");
                            }
                            if (propertiesResult.containsKey(connector.getString(JahiaOAuthConstants.PROPERTY_NAME))) {
                                Map<String, Object> propertyInfo = new HashMap<>();
                                propertyInfo.put(JahiaOAuthConstants.PROPERTY_VALUE, propertiesResult.get(connector.getString(JahiaOAuthConstants.PROPERTY_NAME)));
                                propertyInfo.put(JahiaOAuthConstants.PROPERTY_VALUE_TYPE, connector.getString(JahiaOAuthConstants.PROPERTY_VALUE_TYPE));
                                if (connector.has(JahiaOAuthConstants.PROPERTY_VALUE_FORMAT)) {
                                    propertyInfo.put(JahiaOAuthConstants.PROPERTY_VALUE_FORMAT, connector.getString(JahiaOAuthConstants.PROPERTY_VALUE_FORMAT));
                                }
                                mapperResult.put(mapper.getString(JahiaOAuthConstants.PROPERTY_NAME), propertyInfo);
                            }
                        }

                        jahiaOAuthCacheService.cacheMapperResults(mapperNode.getName() + "_" + state, mapperResult);
                        MapperService mapperService = BundleUtils.getOsgiService(MapperService.class,
                                "(" + JahiaOAuthConstants.MAPPER_SERVICE_NAME + "=" + mapperNode.getName() + ")");
                        if (mapperService != null) {
                            mapperService.executeMapper(mapperResult);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(response.getBody(), e);
                throw e;
            }
        } else {
            logger.error("Did not received expected json, response body was: ", response.getBody());
            throw new JahiaOAuthException("Did not received expected json, response body was: " + response.getBody());
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
        List <String> callbackUrls = new ArrayList<>();
        String callbackUrl;
        for (JCRValueWrapper wrapper : connectorNode.getProperty(JahiaOAuthConstants.PROPERTY_CALLBACK_URLS).getValues()) {
            callbackUrls.add(wrapper.getString());
        }
        callbackUrl = callbackUrls.get(new Random().nextInt(callbackUrls.size()));

        ServiceBuilder serviceBuilder = new ServiceBuilder()
                .apiKey(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_API_KEY))
                .apiSecret(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_API_SECRET))
                .callback(callbackUrl)
                .state(state);

        if (connectorNode.hasProperty(JahiaOAuthConstants.PROPERTY_SCOPE) && StringUtils.isNotBlank(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_SCOPE))) {
            serviceBuilder.scope(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_SCOPE));
        }

        return serviceBuilder.build(oAuthBase20ApiMap.get(serviceName));
    }

    public void setoAuthBase20ApiMap(Map<String, BaseApi<? extends OAuth20Service>> oAuthBase20ApiMap) {
        this.oAuthBase20ApiMap = oAuthBase20ApiMap;
    }

    public void setJahiaOAuthCacheService(JahiaOAuthCacheService jahiaOAuthCacheService) {
        this.jahiaOAuthCacheService = jahiaOAuthCacheService;
    }
}
