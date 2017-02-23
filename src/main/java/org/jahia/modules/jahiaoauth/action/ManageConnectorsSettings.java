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
package org.jahia.modules.jahiaoauth.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaoauth.service.Constants;
import org.jahia.services.content.*;
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

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper jahiaOAuthNode = getOrCreateNode(renderContext.getSite(), Constants.JAHIA_OAUTH_NODE_NAME, Constants.JAHIA_OAUTH_NODE_TYPE);
        JSONObject response = new JSONObject();
        // Get registered data
        if (req.getMethod().equals(Constants.METHOD_GET)) {
            if (!parameters.containsKey(Constants.SERVICE_NAME)
                    || !parameters.containsKey(Constants.PROPERTIES)
                    || parameters.get(Constants.PROPERTIES).isEmpty()) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            String nodeName = parameters.get(Constants.SERVICE_NAME).get(0);
            if (!jahiaOAuthNode.hasNode(nodeName)) {
                return new ActionResult(HttpServletResponse.SC_OK);
            }

            JCRNodeWrapper connectorSettingsNode = jahiaOAuthNode.getNode(nodeName);
            for (String property : parameters.get(Constants.PROPERTIES)) {
                if (connectorSettingsNode.hasProperty(property)) {
                    if (!property.equals(Constants.PROPERTY_IS_ACTIVATE)) {
                        response.put(property, connectorSettingsNode.getPropertyAsString(property));
                    } else {
                        response.put(property, connectorSettingsNode.getProperty(property).getBoolean());
                    }
                }
            }
        }
        // Register or update data
        else {
            if (!parameters.containsKey(Constants.SERVICE_NAME)
                    || !parameters.containsKey(Constants.NODE_TYPE)
                    || !parameters.containsKey(Constants.PROPERTIES)
                    || parameters.get(Constants.PROPERTIES).isEmpty()) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            HashMap<String, Object> properties = new ObjectMapper().readValue(parameters.get(Constants.PROPERTIES).get(0), HashMap.class);
            if (!properties.containsKey(Constants.PROPERTY_API_KEY)
                    || !properties.containsKey(Constants.PROPERTY_API_SECRET)
                    || !properties.containsKey(Constants.PROPERTY_CALLBACK_URL)
                    || !properties.containsKey(Constants.PROPERTY_IS_ACTIVATE)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            String nodeName = parameters.get(Constants.SERVICE_NAME).get(0);
            String nodeType = parameters.get(Constants.NODE_TYPE).get(0);
            JCRNodeWrapper connectorSettingsNode = getOrCreateNode(jahiaOAuthNode, nodeName, nodeType);

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (entry.getValue() instanceof String) {
                    connectorSettingsNode.setProperty(entry.getKey(), (String) entry.getValue());
                }
                if (entry.getValue() instanceof Boolean) {
                    connectorSettingsNode.setProperty(entry.getKey(), (Boolean) entry.getValue());
                }
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
            if (node.isNodeType(Constants.OAUTH_CONNECTOR_SETTINGS_NODE_TYPE)) {
                node.addNode(Constants.MAPPERS_NODE_NAME, Constants.MAPPERS_NODE_TYPE);
            }
            parentNode.getSession().save();
        }
        return node;
    }
}
