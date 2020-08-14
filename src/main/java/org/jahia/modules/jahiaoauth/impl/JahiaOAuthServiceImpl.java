/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
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
import org.jahia.api.content.JCRTemplate;
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

    private JCRTemplate jcrTemplate;
    private Map<String, DefaultApi20> oAuthDefaultApi20Map;
    private JahiaOAuthCacheService jahiaOAuthCacheService;

    @Override
    public String getAuthorizationUrl(OAuthConnectorConfig config, String sessionId) {
        return getAuthorizationUrl(config, sessionId, null);
    }

    @Override
    public String getAuthorizationUrl(OAuthConnectorConfig config, String sessionId, Map<String, String> additionalParams) {
        OAuth20Service service = createOAuth20Service(config);

        return service.createAuthorizationUrlBuilder().state(sessionId).build();
    }

    @Override
    public Map<String, Object> getMapperResults(String mapperServiceName, String sessionId) {
        return jahiaOAuthCacheService.getMapperResultsCacheEntry(mapperServiceName + "_" + sessionId);
    }

    @Override
    public String getResultUrl(String siteUrl, Boolean isAuthenticate) {
        return StringUtils.substringBeforeLast(siteUrl, ".html") + "/oauth-result.html?isAuthenticate=" + isAuthenticate;
    }

    @Override
    public Map<String, Object> refreshAccessToken(OAuthConnectorConfig config, String refreshToken) throws Exception {
        OAuth20Service service = createOAuth20Service(config);
        OAuth2AccessToken accessToken = service.refreshAccessToken(refreshToken);
        return extractAccessTokenData(accessToken);
    }

    @Override
    public Map<String, Object> requestUserData(OAuthConnectorConfig config, String mapperServiceName, String refreshToken) throws Exception {
        OAuth20Service service = createOAuth20Service(config);
        OAuth2AccessToken accessToken = service.refreshAccessToken(refreshToken);

        ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class, "(" + JahiaOAuthConstants.CONNECTOR_SERVICE_NAME + "=" + config.getConnectorName() + ")");
        if (connectorService == null) {
            logger.error("Connector service was null for service name: {}", config.getConnectorName());
            throw new JahiaOAuthException("Connector service was null for service name: "+ config.getConnectorName());
        }

        // Request all the properties available right now
        OAuthRequest request = new OAuthRequest(Verb.GET, connectorService.getProtectedResourceUrl());
        request.addHeader("x-li-format", "json");
        service.signRequest(accessToken, request);
        Response response = service.execute(request);

        // if we got the properties then execute mapper
        if (response.getCode() == HttpServletResponse.SC_OK) {
            try {
                JSONObject responseJson = new JSONObject(response.getBody());
                if (logger.isDebugEnabled()) {
                    logger.debug(responseJson.toString());
                }

                // Store in a simple map the results by properties as mapped in the connector
                Map<String, Object> propertiesResult = getPropertiesResult(connectorService, responseJson);

                Map<String, Object> mapperResult = getMapperResults(propertiesResult, config.getMapper(mapperServiceName));
                addTokensData(config.getConnectorName(), accessToken, propertiesResult, mapperResult, config.getSiteKey());

                return mapperResult;
            } catch (Exception e) {
                logger.error("Did not received expected json, response message was: {} and response body was: {}",response.getMessage(), response.getBody());
                throw e;
            }
        } else {
            logger.error("Did not received expected response, response code: {} response message: {} response body was: ", response.getCode(), response.getBody());
            throw new JahiaOAuthException("Did not received expected response, response code: " + response.getCode() + ", response message: " + response.getMessage() + " response body was: " + response.getBody());
        }
    }

    @Override
    public void extractAccessTokenAndExecuteMappers(OAuthConnectorConfig config, String token, String state) throws Exception {
        OAuth20Service service = createOAuth20Service(config);
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class, "(" + JahiaOAuthConstants.CONNECTOR_SERVICE_NAME + "=" + config.getConnectorName() + ")");
        if (connectorService == null) {
            logger.error("Connector service was null for service name: {}", config.getConnectorName());
            throw new JahiaOAuthException("Connector service was null for service name: " + config.getConnectorName());
        }

        // Request all the properties available right now
        OAuthRequest request = new OAuthRequest(Verb.GET, connectorService.getProtectedResourceUrl());
        request.addHeader("x-li-format", "json");
        service.signRequest(accessToken, request);
        Response response = service.execute(request);

        // if we got the properties then execute mapper
        if (response.getCode() == HttpServletResponse.SC_OK) {
            try {
                JSONObject responseJson = new JSONObject(response.getBody());
                if (logger.isDebugEnabled()) {
                    logger.debug(responseJson.toString());
                }

                // Store in a simple map the results by properties as mapped in the connector
                Map<String, Object> propertiesResult = getPropertiesResult(connectorService, responseJson);

                // Get Mappers
                for (MapperConfig mapper : config.getMappers()) {
                    if (mapper.isActive()) {
                        Map<String, Object> mapperResult = getMapperResults(propertiesResult, mapper);
                        addTokensData(config.getConnectorName(), accessToken, propertiesResult, mapperResult, config.getSiteKey());
                        executeMapper(state, mapper.getMapperName(), mapperResult);
                    }
                }
            } catch (Exception e) {
                logger.error("Did not received expected json, response message was: {} and response body was: {}",response.getMessage(), response.getBody());
                throw e;
            }
        } else {
            logger.error("Did not received expected response, response code: {}, response message: {} response body was: {}", response.getCode(), response.getMessage(), response.getBody());
            throw new JahiaOAuthException("Did not received expected response, response code: " + response.getCode() + ", response message: " + response.getMessage() + " response body was: " + response.getBody());
        }
    }

    public void executeMapper(String sessionId, String mapperName, Map<String, Object> mapperResult) {
        MapperService mapperService = BundleUtils.getOsgiService(MapperService.class, "(" + JahiaOAuthConstants.MAPPER_SERVICE_NAME + "=" + mapperName + ")");
        if (mapperService != null) {
            mapperService.executeMapper(mapperResult);
        }
        jahiaOAuthCacheService.cacheMapperResults(mapperName + "_" + sessionId, mapperResult);
    }

    private Map<String, Object> extractAccessTokenData(OAuth2AccessToken accessToken) {
        Map<String, Object> tokenData = new HashMap<>();

        tokenData.put(JahiaOAuthConstants.ACCESS_TOKEN, accessToken.getAccessToken());
        tokenData.put(JahiaOAuthConstants.TOKEN_EXPIRES_IN, accessToken.getExpiresIn());
        tokenData.put(JahiaOAuthConstants.REFRESH_TOKEN, accessToken.getRefreshToken());
        tokenData.put(JahiaOAuthConstants.TOKEN_SCOPE, accessToken.getScope());
        tokenData.put(JahiaOAuthConstants.TOKEN_TYPE, accessToken.getTokenType());

        return tokenData;
    }

    private Map<String, Object> getPropertiesResult(ConnectorService connectorService, JSONObject responseJson) throws JSONException {
        Map<String, Object> propertiesResult = new HashMap<>();
        List<Map<String, Object>> properties = connectorService.getAvailableProperties();
        for (Map<String, Object> entry : properties) {
            String propertyName = (String) entry.get(JahiaOAuthConstants.PROPERTY_NAME);
            if ((boolean) entry.get(JahiaOAuthConstants.CAN_BE_REQUESTED) && responseJson.has(propertyName)) {
                propertiesResult.put(propertyName, responseJson.get(propertyName));
            } else if (entry.containsKey(JahiaOAuthConstants.PROPERTY_TO_REQUEST)) {
                String propertyToRequest = (String) entry.get(JahiaOAuthConstants.PROPERTY_TO_REQUEST);
                getPropertyResult(responseJson, propertiesResult, entry, propertyName, propertyToRequest);
            }
        }
        return propertiesResult;
    }

    private void getPropertyResult(JSONObject responseJson, Map<String, Object> propertiesResult, Map<String, Object> entry, String propertyName, String propertyToRequest) throws JSONException {
        if (responseJson.has(propertyToRequest)) {
            if (entry.containsKey(JahiaOAuthConstants.VALUE_PATH)) {
                String pathToProperty = (String) entry.get(JahiaOAuthConstants.VALUE_PATH);
                if (StringUtils.startsWith(pathToProperty, "/")) {
                    extractPropertyFromJSONObject(propertiesResult, responseJson.getJSONObject(propertyToRequest), pathToProperty, propertyName);
                } else {
                    extractPropertyFromJSONArray(propertiesResult, responseJson.getJSONArray(propertyToRequest), pathToProperty, propertyName);
                }
            } else {
                propertiesResult.put(propertyName, responseJson.get(propertyToRequest));
            }
        }
    }

    private Map<String, Object> getMapperResults(Map<String, Object> propertiesResult, MapperConfig mapper) throws JahiaOAuthException {
        Map<String, Object> mapperResult = new HashMap<>();
        for (Mapping mapping : mapper.getMappings()) {
            if (mapping.isMapperPropertyMandatory() && !propertiesResult.containsKey(mapping.getConnectorPropertyName())) {
                throw new JahiaOAuthException("Could not execute mapper: missing mandatory property");
            }
            if (propertiesResult.containsKey(mapping.getConnectorPropertyName())) {
                Map<String, Object> propertyInfo = new HashMap<>();
                propertyInfo.put(JahiaOAuthConstants.PROPERTY_VALUE, propertiesResult.get(mapping.getConnectorPropertyName()));
                propertyInfo.put(JahiaOAuthConstants.PROPERTY_VALUE_TYPE, mapping.getConnectorPropertyType());
                if (mapping.getConnectorPropertyFormat() != null) {
                    propertyInfo.put(JahiaOAuthConstants.PROPERTY_VALUE_FORMAT, mapping.getConnectorPropertyFormat());
                }
                mapperResult.put(mapping.getMapperPropertyName(), propertyInfo);
            }
        }

        return mapperResult;
    }

    private void addTokensData(String connectorServiceName, OAuth2AccessToken accessToken, Map<String, Object> propertiesResult, Map<String, Object> mapperResult, String siteKey) {
        // add token to result
        mapperResult.put(JahiaOAuthConstants.TOKEN_DATA, extractAccessTokenData(accessToken));
        mapperResult.put(JahiaOAuthConstants.CONNECTOR_SERVICE_NAME, connectorServiceName);
        mapperResult.put(JahiaOAuthConstants.CONNECTOR_NAME_AND_ID, connectorServiceName + "_" + propertiesResult.get("id"));
        mapperResult.put(JahiaOAuthConstants.PROPERTY_SITE_KEY, siteKey);
    }

    private void extractPropertyFromJSONObject(Map<String, Object> propertiesResult, JSONObject jsonObject, String pathToProperty, String propertyName) throws JSONException {
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
                    extractPropertyFromJSONObject(propertiesResult, jsonObject.getJSONObject(key), pathToProperty, propertyName);
                } else if (jsonObject.has(key)) {
                    extractPropertyFromJSONArray(propertiesResult, jsonObject.getJSONArray(key), pathToProperty, propertyName);
                }
            }
        }
    }

    private void extractPropertyFromJSONArray(Map<String, Object> propertiesResult, JSONArray jsonArray, String pathToProperty, String propertyName) throws JSONException {
        int arrayIndex = Integer.parseInt(StringUtils.substringBetween(pathToProperty, "[", "]"));
        pathToProperty = StringUtils.substringAfter(pathToProperty, "]");
        if (StringUtils.isBlank(pathToProperty) && jsonArray.length() >= arrayIndex) {
            propertiesResult.put(propertyName, jsonArray.get(arrayIndex));
        } else {
            if (StringUtils.startsWith(pathToProperty, "/") && jsonArray.length() >= arrayIndex) {
                extractPropertyFromJSONObject(propertiesResult, jsonArray.getJSONObject(arrayIndex), pathToProperty, propertyName);
            } else if (jsonArray.length() >= arrayIndex) {
                extractPropertyFromJSONArray(propertiesResult, jsonArray.getJSONArray(arrayIndex), pathToProperty, propertyName);
            }
        }
    }

    private OAuth20Service createOAuth20Service(OAuthConnectorConfig config) {
        List<String> callbackUrls = config.getCallbackUrls();
        String callbackUrl = callbackUrls.get(new Random().nextInt(callbackUrls.size()));

        ServiceBuilder serviceBuilder = new ServiceBuilder(config.getApiKey()).apiSecret(config.getApiSecret()).callback(callbackUrl);

        if (StringUtils.isNotBlank(config.getScopes())) {
            serviceBuilder.withScope(config.getScopes());
        }

        return serviceBuilder.build(oAuthDefaultApi20Map.get(config.getConnectorName()));
    }

    public Map<String, OAuthConnectorConfig> getOAuthConfig(String siteKey) throws RepositoryException {
        return jcrTemplate.doExecuteWithSystemSession(systemSession -> {
            try {
                JCRNodeWrapper jahiaOAuthNode = systemSession.getNode("/sites/" + siteKey).getNode(JahiaOAuthConstants.JAHIA_OAUTH_NODE_NAME);
                Map<String,OAuthConnectorConfig> l = new HashMap<>();
                JCRNodeIteratorWrapper mappersNi = jahiaOAuthNode.getNodes();
                while (mappersNi.hasNext()) {
                    JCRNodeWrapper node = (JCRNodeWrapper) mappersNi.nextNode();
                    OAuthConnectorConfig config = getConnectorConfig(node);
                    l.put(config.getConnectorName(), config);
                }
                return l;
            } catch (Exception ex) {
                logger.error("Could not read config", ex);
                return null;
            }
        });
    }

    private OAuthConnectorConfig getConnectorConfig(JCRNodeWrapper connectorNode) throws JSONException, RepositoryException {
        OAuthConnectorConfig connectorConfig = new OAuthConnectorConfig(connectorNode.getName());
        connectorConfig.setActive(connectorNode.getProperty(JahiaOAuthConstants.PROPERTY_IS_ACTIVATE).getBoolean());
        connectorConfig.setSiteKey(connectorNode.getResolveSite().getSiteKey());
        connectorConfig.setApiKey(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_API_KEY));
        connectorConfig.setApiSecret(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_API_SECRET));
        for (JCRValueWrapper wrapper : connectorNode.getProperty(JahiaOAuthConstants.PROPERTY_CALLBACK_URLS).getValues()) {
            connectorConfig.getCallbackUrls().add(wrapper.getString());
        }
        if (connectorNode.hasProperty(JahiaOAuthConstants.PROPERTY_SCOPE) && StringUtils.isNotBlank(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_SCOPE))) {
            connectorConfig.setScopes(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_SCOPE));
        }

        JCRNodeIteratorWrapper mappersNi = connectorNode.getNode(JahiaOAuthConstants.MAPPERS_NODE_NAME).getNodes();
        while (mappersNi.hasNext()) {
            JCRNodeWrapper mapperNode = (JCRNodeWrapper) mappersNi.nextNode();

            MapperConfig mapperConfig = new MapperConfig(mapperNode.getName());
            mapperConfig.setSiteKey(mapperNode.getResolveSite().getSiteKey());
            mapperConfig.setActive(mapperNode.getProperty(JahiaOAuthConstants.PROPERTY_IS_ACTIVATE).getBoolean());
            JSONArray mappingsJson = new JSONArray(mapperNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_MAPPING));
            for (int i = 0; i < mappingsJson.length(); i++) {
                JSONObject jsonObject = mappingsJson.getJSONObject(i);
                JSONObject mapperJson = jsonObject.getJSONObject(JahiaOAuthConstants.MAPPER);
                JSONObject connectorJson = jsonObject.getJSONObject(JahiaOAuthConstants.CONNECTOR);

                Mapping mapping = new Mapping();
                mapping.setConnectorPropertyName(connectorJson.getString(JahiaOAuthConstants.PROPERTY_NAME));
                mapping.setConnectorPropertyType(connectorJson.getString(JahiaOAuthConstants.PROPERTY_VALUE_TYPE));
                if (connectorJson.has(JahiaOAuthConstants.PROPERTY_VALUE_FORMAT)) {
                    mapping.setConnectorPropertyFormat(connectorJson.getString(JahiaOAuthConstants.PROPERTY_VALUE_FORMAT));
                }
                mapping.setMapperPropertyName(mapperJson.getString(JahiaOAuthConstants.PROPERTY_NAME));
                mapping.setMapperPropertyMandatory(mapperJson.getBoolean(JahiaOAuthConstants.PROPERTY_MANDATORY));
                mapperConfig.getMappings().add(mapping);
            }
            connectorConfig.getMappers().add(mapperConfig);
        }
        return connectorConfig;
    }

    public void setoAuthDefaultApi20Map(Map<String, DefaultApi20> oAuthDefaultApi20Map) {
        this.oAuthDefaultApi20Map = oAuthDefaultApi20Map;
    }

    public void setJahiaOAuthCacheService(JahiaOAuthCacheService jahiaOAuthCacheService) {
        this.jahiaOAuthCacheService = jahiaOAuthCacheService;
    }

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }
}
