package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaoauth.service.Constants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuth;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author dgaillard
 */
public class JahiaOAuthImpl implements JahiaOAuth {
    private static final Logger logger = LoggerFactory.getLogger(JahiaOAuthImpl.class);

    private Map<String, OAuth20Service> oAuth20ServiceMap;
    private Map<String, Map<String, Object>> oAuthBaseApiMap;
    private Map<String, Map<String, Object>> oAuthMapperPropertiesMap;

    public String getAuthorizationUrl(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope, String state) throws Exception {
        OAuth20Service service = getOrCreateOAuth20Service(serviceName, apiKey, apiSecret, callbackUrl, scope, state);

        return service.getAuthorizationUrl();
    }

    public void storeTokenAndExecuteMapper(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope, String state, String token) throws Exception {
        OAuth20Service service = getOrCreateOAuth20Service(serviceName, apiKey, apiSecret, callbackUrl, scope, state);
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        HashMap<String, Map<String, Object>> properties = (HashMap<String, Map<String, Object>>) oAuthBaseApiMap.get(serviceName).get(Constants.PROPERTIES);
        String protectedResourceUrl = (String) oAuthBaseApiMap.get(serviceName).get(Constants.PROTECTED_RESOURCE_URL);
        if ((boolean) oAuthBaseApiMap.get(serviceName).get(Constants.URL_CAN_TAKE_VALUE)) {

            StringBuilder propertiesAsString = new StringBuilder();
            boolean asPrevious = false;
            for (Map.Entry<String, Map<String, Object>> entry : properties.entrySet()) {
                if (asPrevious) {
                    propertiesAsString.append(",");
                }

                if ((boolean) entry.getValue().get(Constants.CAN_BE_REQUESTED)) {
                    propertiesAsString.append(entry.getKey());
                    asPrevious = true;
                } else {
                    String propertyToRequest = (String) entry.getValue().get(Constants.PROPERTY_TO_REQUEST);
                    if (!StringUtils.contains(propertiesAsString.toString(), propertyToRequest)) {
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

        logger.info(Integer.toString(response.getCode()));
        logger.info(response.getBody());
    }

    private OAuth20Service getOrCreateOAuth20Service(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope, String state) throws Exception {
        if (oAuth20ServiceMap != null && oAuth20ServiceMap.containsKey(serviceName)) {
            return oAuth20ServiceMap.get(serviceName);
        }

        if (oAuth20ServiceMap == null) {
            oAuth20ServiceMap = new HashMap<>();
        }

        ServiceBuilder serviceBuilder = new ServiceBuilder().apiKey(apiKey).apiSecret(apiSecret).callback(callbackUrl);

        if (scope != null) {
            serviceBuilder.scope(scope);
        }

        if (state != null) {
            serviceBuilder.state(state);
        }

        OAuth20Service oAuth20Service = serviceBuilder.build((BaseApi<? extends OAuth20Service>) oAuthBaseApiMap.get(serviceName).get(Constants.API));

        oAuth20ServiceMap.put(serviceName, oAuth20Service);

        return oAuth20Service;
    }

    public void addDataToOAuthBaseApiMap(Map<String, Map<String, Object>> dataToLoad, String serviceName) {
        for (Map.Entry<String, Object> entry : dataToLoad.get(serviceName).entrySet()) {
            oAuthBaseApiMap.get(serviceName).put(entry.getKey(), entry.getValue());
        }
    }

    public void addDataToOAuthMapperPropertiesMap(Map<String, Map<String, Object>> mapperProperties, String mapperKey) {
        if (oAuthMapperPropertiesMap == null) {
            oAuthMapperPropertiesMap = new HashMap<>();
        }

        if (!oAuthMapperPropertiesMap.containsKey(mapperKey)) {
            oAuthMapperPropertiesMap.put(mapperKey, new HashMap<String, Object>());
        }

        oAuthMapperPropertiesMap.get(mapperKey).put(Constants.PROPERTIES, mapperProperties);
    }

    public void setoAuthBaseApiMap(Map<String, Map<String, Object>> oAuthBaseApiMap) {
        this.oAuthBaseApiMap = oAuthBaseApiMap;
    }

    public JSONObject getConnectorProperties(String serviceName) throws JSONException {
        HashMap<String, Map<String, Object>> map = (HashMap<String, Map<String, Object>>) oAuthBaseApiMap.get(serviceName).get(Constants.PROPERTIES);
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject;
    }

    public JSONObject getMapperProperties(String mapperKey) throws JSONException {
        HashMap<String, Map<String, Object>> map = (HashMap<String, Map<String, Object>>) oAuthMapperPropertiesMap.get(mapperKey).get(Constants.PROPERTIES);
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject;
    }

    public String resolveConnectorNodeName(String serviceName) {
        return (String) oAuthBaseApiMap.get(serviceName).get("settingsNodeName");
    }
}
