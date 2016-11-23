package org.jahia.modules.jahiaoauth.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaoauth.service.JahiaOAuth;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ManageConnectorsSettings extends Action {
    private static final Logger logger = LoggerFactory.getLogger(ManageConnectorsSettings.class);

    private static String METHOD_GET = "GET";
    private static String JAHIA_OAUTH_NODE_TYPE = "joant:jahiaOAuth";
    private static String PROPERTY_NODE_NAME = "nodeName";
    private static String PROPERTY_NODE_TYPE = "nodeType";
    private static String PROPERTIES = "properties";
    private JahiaOAuth jahiaOAuth;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper jahiaOAuthNode = getOrCreateNode(renderContext.getSite(), jahiaOAuth.JAHIA_OAUTH, JAHIA_OAUTH_NODE_TYPE);
        JSONObject response = new JSONObject();
        if (req.getMethod().equals(METHOD_GET)) {
            if (!parameters.containsKey(PROPERTY_NODE_NAME) || !parameters.containsKey(PROPERTIES) || parameters.get(PROPERTIES).isEmpty()) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            String nodeName = parameters.get(PROPERTY_NODE_NAME).get(0);
            if (!jahiaOAuthNode.hasNode(nodeName)) {
                return new ActionResult(HttpServletResponse.SC_OK);
            }

            JCRNodeWrapper connectorSettingsNode = jahiaOAuthNode.getNode(nodeName);
            for (String property : parameters.get(PROPERTIES)) {
                if (connectorSettingsNode.hasProperty(property)) {
                    response.put(property, connectorSettingsNode.getPropertyAsString(property));
                }
            }
        } else {
            if (!parameters.containsKey(PROPERTY_NODE_NAME) || !parameters.containsKey(PROPERTIES)
                    || parameters.get(PROPERTIES).isEmpty() || !parameters.containsKey(PROPERTY_NODE_TYPE)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            String nodeName = parameters.get(PROPERTY_NODE_NAME).get(0);
            String nodeType = parameters.get(PROPERTY_NODE_TYPE).get(0);
            JCRNodeWrapper connectorSettingsNode = getOrCreateNode(jahiaOAuthNode, nodeName, nodeType);

            HashMap<String, String> properties = new ObjectMapper().readValue(parameters.get(PROPERTIES).get(0), HashMap.class);
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                connectorSettingsNode.setProperty(entry.getKey(), entry.getValue());
            }
            connectorSettingsNode.getSession().save();
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    private JCRNodeWrapper getOrCreateNode(JCRNodeWrapper parentNode, String nodeName, String primaryNodeType) throws RepositoryException {
        JCRNodeWrapper node;
        if (parentNode.hasNode(nodeName)) {
            node = parentNode.getNode(nodeName);
        } else {
            node = parentNode.addNode(nodeName, primaryNodeType);
            if (node.isNodeType("joamix:oauthConnectorSettings")) {
                node.addNode("mappers", "joant:mappers");
            }
            parentNode.getSession().save();
        }
        return node;
    }

    public void setJahiaOAuth(JahiaOAuth jahiaOAuth) {
        this.jahiaOAuth = jahiaOAuth;
    }
}
