package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaoauth.service.JahiaOAuth;
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
//    private Map<String, BaseApi<? extends OAuth20Service>> oAuthBaseApiMap;

    public String getAuthorizationUrl(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope) throws Exception {
        OAuth20Service service = getOrCreateOAuth20Service(serviceName, apiKey, apiSecret, callbackUrl, scope);

        return service.getAuthorizationUrl();
    }

    public void storeTokenAndExecuteMapper(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope, String token) throws Exception {
        OAuth20Service service = getOrCreateOAuth20Service(serviceName, apiKey, apiSecret, callbackUrl, scope);
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        HashMap<String, Map<String, Object>> properties = (HashMap<String, Map<String, Object>>) oAuthBaseApiMap.get(serviceName).get("properties");
        String protectedResourceUrl = (String) oAuthBaseApiMap.get(serviceName).get("protectedResourceUrl");
        String urlCanTakeValue = (String) oAuthBaseApiMap.get(serviceName).get("urlCanTakeValue");
        if (urlCanTakeValue.equals("true")) {

            StringBuilder propertiesAsString = new StringBuilder();
            boolean asPrevious = false;
            for (Map.Entry<String, Map<String, Object>> entry : properties.entrySet()) {
                if (asPrevious) {
                    propertiesAsString.append(",");
                }

                if (entry.getValue().get("canBeRequested").equals("true")) {
                    propertiesAsString.append(entry.getKey());
                    asPrevious = true;
                } else {
                    String propertyToRequest = (String) entry.getValue().get("propertyToRequest");
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

    private OAuth20Service getOrCreateOAuth20Service(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope) throws Exception {
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

        OAuth20Service oAuth20Service = serviceBuilder.build((BaseApi<? extends OAuth20Service>) oAuthBaseApiMap.get(serviceName).get("api"));

        oAuth20ServiceMap.put(serviceName, oAuth20Service);

        return oAuth20Service;
    }

    public void addDataToOAuthBaseApiMap(Map<String, Map<String, Object>> dataToLoad, String serviceName) {
        for (Map.Entry<String, Object> entry : dataToLoad.get(serviceName).entrySet()) {
            oAuthBaseApiMap.get(serviceName).put(entry.getKey(), entry.getValue());
        }
    }

    public void setoAuthBaseApiMap(Map<String, Map<String, Object>> oAuthBaseApiMap) {
        this.oAuthBaseApiMap = oAuthBaseApiMap;
    }
}
