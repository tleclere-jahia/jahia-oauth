package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.SystemAction;
import org.jahia.modules.jahiaoauth.service.Constants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuth;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author dgaillard
 */
public class JahiaOAuthImpl implements JahiaOAuth {
    private static final Logger logger = LoggerFactory.getLogger(JahiaOAuthImpl.class);

    private JCRTemplate jcrTemplate;
    private JahiaTemplateManagerService templateManagerService;
    private Map<String, OAuth20Service> oAuth20ServiceMap;
    private Map<String, Map<String, Object>> oAuthBase20ApiMap;
    private Map<String, Map<String, Object>> oAuthMapperPropertiesMap;

    public String getAuthorizationUrl(JCRNodeWrapper jahiaOAuthNode, String serviceName) throws RepositoryException {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(serviceName);
        OAuth20Service service = getOrCreateOAuth20Service(connectorNode, serviceName);

        return service.getAuthorizationUrl();
    }

    public void extractTokenAndExecuteMapper(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                             URLResolver urlResolver, JCRSessionWrapper session,
                                             JCRNodeWrapper jahiaOAuthNode, String serviceName, String token) throws Exception {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(serviceName);
        OAuth20Service service = getOrCreateOAuth20Service(connectorNode, serviceName);
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        // Request all the properties available right now
        HashMap<String, Map<String, Object>> properties = (HashMap<String, Map<String, Object>>) oAuthBase20ApiMap.get(serviceName).get(Constants.PROPERTIES);
        String protectedResourceUrl = (String) oAuthBase20ApiMap.get(serviceName).get(Constants.PROTECTED_RESOURCE_URL);
        if ((boolean) oAuthBase20ApiMap.get(serviceName).get(Constants.URL_CAN_TAKE_VALUE)) {
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

        // if we got the properties then execute mapper
        if (response.getCode() == HttpServletResponse.SC_OK) {
            JSONObject responseJson = new JSONObject(response.getBody());
            logger.info(responseJson.toString());

            // Store in a simple map the results by properties as mapped in the connector
            HashMap<String, Object> propertiesResult = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry : properties.entrySet()) {
                if ((boolean) entry.getValue().get(Constants.CAN_BE_REQUESTED)) {
                    propertiesResult.put(entry.getKey(), responseJson.get(entry.getKey()));
                } else {
                    String propertyToRequest = (String) entry.getValue().get(Constants.PROPERTY_TO_REQUEST);
                    String pathToProperty = (String) entry.getValue().get(Constants.VALUE_PATH);
                    JSONObject jsonObject = responseJson.getJSONObject(propertyToRequest);
                    extractPropertyFromJSON(propertiesResult, jsonObject, null, pathToProperty, entry.getKey());
                }
            }

            // Get Mappers node
            JCRNodeIteratorWrapper mappersNi = connectorNode.getNode(Constants.MAPPERS_NODE_NAME).getNodes();
            while (mappersNi.hasNext()) {
                JCRNodeWrapper mapper = (JCRNodeWrapper) mappersNi.nextNode();
                // make sure mappers is activate
                if (mapper.getProperty(Constants.PROPERTY_IS_ACTIVATE).getBoolean()) {
                    HashMap<String, List<String>> mapperResult = new HashMap<>();
                    JSONArray jsonArray = new JSONArray(mapper.getPropertyAsString(Constants.PROPERTY_MAPPING));

                    for (int i = 0 ; i < jsonArray.length() ; i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        List<String> list = new ArrayList<>();
                        list.add((String) propertiesResult.get(jsonObject.getString(Constants.CONNECTOR)));
                        mapperResult.put(jsonObject.getString(Constants.MAPPER), list);
                    }

                    Action action = templateManagerService.getActions().get(oAuthMapperPropertiesMap.get(mapper.getName()).get(Constants.MAPPER_ACTION_NAME));
                    if (action != null) {
                        executeAction(action, req, renderContext, resource, urlResolver, session, mapperResult);
                    }
                }
            }
        } else {
            logger.error(response.getBody());
        }
    }

    private void executeAction(final Action originalAction, HttpServletRequest req, RenderContext renderContext,
                                       Resource resource, URLResolver urlResolver, JCRSessionWrapper session, Map<String, List<String>> mapperResul) {
        try {
            Action action = new SystemAction() {
                @Override
                public ActionResult doExecuteAsSystem(HttpServletRequest req, RenderContext renderContext,
                                                      JCRSessionWrapper systemSession, Resource resource,
                                                      Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
                    return originalAction.doExecute(req, renderContext, resource, systemSession, parameters, urlResolver);
                }
            };
            action.doExecute(req, renderContext, resource, session, mapperResul, urlResolver);
        } catch (Exception e) {
            logger.error(e.getMessage());
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

            if (StringUtils.isBlank(pathToProperty)) {
                propertiesResult.put(propertyName, jsonObject.get(key));
            } else {
                if (StringUtils.startsWith(pathToProperty, "/")) {
                    extractPropertyFromJSON(propertiesResult, jsonObject.getJSONObject(key), null, pathToProperty, propertyName);
                } else {
                    extractPropertyFromJSON(propertiesResult, null, jsonObject.getJSONArray(key), pathToProperty, propertyName);
                }
            }
        } else {
            int arrayIndex = new Integer(StringUtils.substringBetween(pathToProperty, "[", "]"));
            pathToProperty = StringUtils.substringAfter(pathToProperty, "]");
            if (StringUtils.isBlank(pathToProperty)) {
                propertiesResult.put(propertyName, jsonArray.get(arrayIndex));
            } else {
                if (StringUtils.startsWith(pathToProperty, "/")) {
                    extractPropertyFromJSON(propertiesResult, jsonArray.getJSONObject(arrayIndex), null, pathToProperty, propertyName);
                } else {
                    extractPropertyFromJSON(propertiesResult, null, jsonArray.getJSONArray(arrayIndex), pathToProperty, propertyName);
                }
            }
        }
    }

    private OAuth20Service getOrCreateOAuth20Service(JCRNodeWrapper connectorNode, String serviceName) throws RepositoryException {
        if (oAuth20ServiceMap != null && oAuth20ServiceMap.containsKey(serviceName)) {
            return oAuth20ServiceMap.get(serviceName);
        }

        if (oAuth20ServiceMap == null) {
            oAuth20ServiceMap = new HashMap<>();
        }

        ServiceBuilder serviceBuilder = new ServiceBuilder()
                .apiKey(connectorNode.getPropertyAsString(Constants.PROPERTY_API_KEY))
                .apiSecret(connectorNode.getPropertyAsString(Constants.PROPERTY_API_SECRET))
                .callback(connectorNode.getPropertyAsString(Constants.PROPERTY_CALLBACK_URL));

        if (connectorNode.hasProperty(Constants.PROPERTY_SCOPE)) {
            serviceBuilder.scope(connectorNode.getPropertyAsString(Constants.PROPERTY_SCOPE));
        }

        if (connectorNode.hasProperty(Constants.PROPERTY_STATE)) {
            serviceBuilder.state(connectorNode.getPropertyAsString(Constants.PROPERTY_STATE));
        }

        OAuth20Service oAuth20Service = serviceBuilder.build((BaseApi<? extends OAuth20Service>) oAuthBase20ApiMap.get(serviceName).get(Constants.API));

        oAuth20ServiceMap.put(serviceName, oAuth20Service);

        return oAuth20Service;
    }

    public void addDataToOAuthBaseApiMap(Map<String, Map<String, Object>> dataToLoad, String serviceName) {
        if (oAuthBase20ApiMap.containsKey(serviceName)) {
            for (Map.Entry<String, Object> entry : dataToLoad.get(serviceName).entrySet()) {
                oAuthBase20ApiMap.get(serviceName).put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void addDataToOAuthMapperPropertiesMap(Map<String, Map<String, Object>> mapperProperties, String mapperKey, String mapperActionName) {
        if (oAuthMapperPropertiesMap == null) {
            oAuthMapperPropertiesMap = new HashMap<>();
        }

        if (!oAuthMapperPropertiesMap.containsKey(mapperKey)) {
            oAuthMapperPropertiesMap.put(mapperKey, new HashMap<String, Object>());
        }

        oAuthMapperPropertiesMap.get(mapperKey).put(Constants.PROPERTIES, mapperProperties);
        oAuthMapperPropertiesMap.get(mapperKey).put(Constants.MAPPER_ACTION_NAME, mapperActionName);
    }

    public JSONObject getConnectorProperties(String serviceName) throws JSONException {
        HashMap<String, Map<String, Object>> map = (HashMap<String, Map<String, Object>>) oAuthBase20ApiMap.get(serviceName).get(Constants.PROPERTIES);
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject;
    }

    public JSONObject getMapperProperties(String mapperKey) throws JSONException {
        HashMap<String, Map<String, Object>> map = (HashMap<String, Map<String, Object>>) oAuthMapperPropertiesMap.get(mapperKey).get(Constants.PROPERTIES);
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject;
    }

    public String resolveConnectorNodeName(String serviceName) {
        return (String) oAuthBase20ApiMap.get(serviceName).get(Constants.SETTINGS_NODE_NAME);
    }

    public void setoAuthBase20ApiMap(Map<String, Map<String, Object>> oAuthBase20ApiMap) {
        this.oAuthBase20ApiMap = oAuthBase20ApiMap;
    }

    public void setTemplateManagerService(JahiaTemplateManagerService templateManagerService) {
        this.templateManagerService = templateManagerService;
    }

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }
}
