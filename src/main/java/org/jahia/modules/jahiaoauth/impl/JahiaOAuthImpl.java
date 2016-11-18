package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.jahia.modules.jahiaoauth.service.JahiaOAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dgaillard
 */
public class JahiaOAuthImpl implements JahiaOAuth {
    private static final Logger logger = LoggerFactory.getLogger(JahiaOAuthImpl.class);

    private Map<String, OAuth20Service> oAuth20ServiceMap;
    private Map<String, BaseApi<? extends OAuth20Service>> oAuthBaseApiMap;

    public String getAuthorizationUrl(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope) throws Exception {
        OAuth20Service service = getOrCreateOAuth20Service(serviceName, apiKey, apiSecret, callbackUrl, scope);

        return service.getAuthorizationUrl();
    }

    private OAuth20Service getOrCreateOAuth20Service(String serviceName, String apiKey, String apiSecret, String callbackUrl, String scope) throws Exception {
        if (oAuth20ServiceMap != null && oAuth20ServiceMap.containsKey(serviceName)) {
            return oAuth20ServiceMap.get(serviceName);
        }

        if (oAuth20ServiceMap == null) {
            oAuth20ServiceMap = new HashMap<>();
        }

        OAuth20Service oAuth20Service = new ServiceBuilder().apiKey(apiKey).apiSecret(apiSecret)
                .callback(callbackUrl)
                .scope(scope)
                .build(oAuthBaseApiMap.get(serviceName));
        oAuth20ServiceMap.put(serviceName, oAuth20Service);

        return oAuth20Service;
    }

    public void storeTokenAndExecuteMapper(String token) throws IOException {
        OAuth20Service service = oAuth20ServiceMap.get("LinkedInApi20");
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        OAuthRequest request = new OAuthRequest(Verb.GET, String.format("https://api.linkedin.com/v1/people/~:(%s)", "id"), service);
        request.addHeader("x-li-format", "json");
//        request.addHeader("Accept-Language", "ru-RU");
        service.signRequest(accessToken, request);
        Response response = request.send();

        logger.info(Integer.toString(response.getCode()));
        logger.info(response.getBody());
    }

    public void setoAuthBaseApiMap(Map<String, BaseApi<? extends OAuth20Service>> oAuthBaseApiMap) {
        this.oAuthBaseApiMap = oAuthBaseApiMap;
    }
}
