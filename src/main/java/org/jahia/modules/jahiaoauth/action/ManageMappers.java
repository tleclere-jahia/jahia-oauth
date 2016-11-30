package org.jahia.modules.jahiaoauth.action;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaoauth.service.Constants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuth;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ManageMappers extends Action {
    private static final Logger logger = LoggerFactory.getLogger(ManageMappers.class);

    private JahiaOAuth jahiaOAuth;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        String action = parameters.get("action").get(0);
        JSONObject response = new JSONObject();
        if (action.equals("getConnectorProperties")) {
            response = jahiaOAuth.getConnectorProperties(parameters.get(Constants.SERVICE_NAME).get(0));
        } else if (action.equals("getMapperProperties")) {
            response = jahiaOAuth.getMapperProperties(parameters.get(Constants.MAPPER_KEY).get(0));
        } else if (action.equals("getMapperMapping")) {
            if (!parameters.containsKey(Constants.MAPPER_KEY)
                    || !parameters.containsKey(Constants.SERVICE_NAME)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            String serviceName = parameters.get(Constants.SERVICE_NAME).get(0);
            String mapperKey = parameters.get(Constants.MAPPER_KEY).get(0);
            String nodeName = jahiaOAuth.resolveConnectorNodeName(serviceName);

            JCRNodeWrapper mappersNode = renderContext.getSite().getNode(Constants.JAHIA_OAUTH_NODE_NAME).getNode(nodeName).getNode(Constants.MAPPERS_NODE_NAME);

            if (!mappersNode.hasNode(mapperKey)) {
                return new ActionResult(HttpServletResponse.SC_OK, null, response);
            }

            JCRNodeWrapper mapperNode = mappersNode.getNode(mapperKey);
            JSONArray jsonArrayMapping = new JSONArray(mapperNode.getPropertyAsString(Constants.PROPERTY_MAPPING));
            response.put(Constants.PROPERTY_IS_ACTIVATE, mapperNode.getProperty(Constants.PROPERTY_IS_ACTIVATE).getBoolean());
            response.put(Constants.PROPERTY_MAPPING, jsonArrayMapping);

        } else if (action.equals("setMapperMapping")) {
            if (!parameters.containsKey(Constants.MAPPER_KEY)
                    || !parameters.containsKey(Constants.PROPERTY_MAPPING)
                    || !parameters.containsKey(Constants.PROPERTY_IS_ACTIVATE)
                    || !parameters.containsKey(Constants.SERVICE_NAME)
                    || !parameters.containsKey(Constants.PROPERTY_NODE_TYPE)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            boolean isActivate = Boolean.parseBoolean(parameters.get(Constants.PROPERTY_IS_ACTIVATE).get(0));
            List<String> mapping = parameters.get(Constants.PROPERTY_MAPPING);
            String serviceName = parameters.get(Constants.SERVICE_NAME).get(0);
            String mapperKey = parameters.get(Constants.MAPPER_KEY).get(0);
            String nodeName = jahiaOAuth.resolveConnectorNodeName(serviceName);

            JCRNodeWrapper mappersNode = renderContext.getSite().getNode(Constants.JAHIA_OAUTH_NODE_NAME).getNode(nodeName).getNode(Constants.MAPPERS_NODE_NAME);

            JCRNodeWrapper currentMapperNode;
            if (!mappersNode.hasNode(mapperKey)) {
                currentMapperNode = mappersNode.addNode(mapperKey, parameters.get(Constants.PROPERTY_NODE_TYPE).get(0));
                mappersNode.getSession().save();
            } else {
                currentMapperNode = mappersNode.getNode(mapperKey);
            }

            currentMapperNode.setProperty(Constants.PROPERTY_IS_ACTIVATE, isActivate);
            currentMapperNode.setProperty(Constants.PROPERTY_MAPPING, mapping.toString());

            currentMapperNode.getSession().save();
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    public void setJahiaOAuth(JahiaOAuth jahiaOAuth) {
        this.jahiaOAuth = jahiaOAuth;
    }
}
