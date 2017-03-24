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

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaoauth.service.ConnectorService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.modules.jahiaoauth.service.MapperService;
import org.jahia.osgi.BundleUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ManageMappers extends Action {
    private static final Logger logger = LoggerFactory.getLogger(ManageMappers.class);

    private JahiaOAuthService jahiaOAuthService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        String action = parameters.get("action").get(0);
        String connectorServiceName;
        String mapperServiceName;
        JSONObject response = new JSONObject();
        if (action.equals("getConnectorProperties")) {
            connectorServiceName = parameters.get(JahiaOAuthConstants.CONNECTOR_SERVICE_NAME).get(0);
            ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class, "(" + JahiaOAuthConstants.CONNECTOR_SERVICE_NAME + "=" + connectorServiceName + ")");
            if (connectorService == null) {
                response.put("error", "internal server error");
                return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, response);
            }
            response.put("connectorProperties", new JSONArray(connectorService.getAvailableProperties()));
        } else if (action.equals("getMapperProperties")) {
            mapperServiceName = parameters.get(JahiaOAuthConstants.MAPPER_SERVICE_NAME).get(0);
            MapperService mapperService = BundleUtils.getOsgiService(MapperService.class, "(" + JahiaOAuthConstants.MAPPER_SERVICE_NAME + "=" + mapperServiceName + ")");
            if (mapperService == null) {
                response.put("error", "internal server error");
                return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, response);
            }
            response.put("mapperProperties", new JSONArray(mapperService.getProperties()));
        } else if (action.equals("getMapperMapping")) {
            if (!parameters.containsKey(JahiaOAuthConstants.MAPPER_SERVICE_NAME)
                    || !parameters.containsKey(JahiaOAuthConstants.CONNECTOR_SERVICE_NAME)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            connectorServiceName = parameters.get(JahiaOAuthConstants.CONNECTOR_SERVICE_NAME).get(0);
            mapperServiceName = parameters.get(JahiaOAuthConstants.MAPPER_SERVICE_NAME).get(0);
            JCRNodeWrapper mappersNode = renderContext.getSite().getNode(JahiaOAuthConstants.JAHIA_OAUTH_NODE_NAME).getNode(connectorServiceName).getNode(JahiaOAuthConstants.MAPPERS_NODE_NAME);

            if (!mappersNode.hasNode(mapperServiceName)) {
                return new ActionResult(HttpServletResponse.SC_OK, null, response);
            }

            JCRNodeWrapper mapperNode = mappersNode.getNode(mapperServiceName);
            JSONArray jsonArrayMapping = new JSONArray(mapperNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_MAPPING));
            response.put(JahiaOAuthConstants.PROPERTY_IS_ACTIVATE, mapperNode.getProperty(JahiaOAuthConstants.PROPERTY_IS_ACTIVATE).getBoolean());
            response.put(JahiaOAuthConstants.PROPERTY_MAPPING, jsonArrayMapping);

        } else if (action.equals("setMapperMapping")) {
            if (!parameters.containsKey(JahiaOAuthConstants.PROPERTY_IS_ACTIVATE)
                    || !parameters.containsKey(JahiaOAuthConstants.MAPPER_SERVICE_NAME)
                    || !parameters.containsKey(JahiaOAuthConstants.CONNECTOR_SERVICE_NAME)
                    || !parameters.containsKey(JahiaOAuthConstants.NODE_TYPE)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            connectorServiceName = parameters.get(JahiaOAuthConstants.CONNECTOR_SERVICE_NAME).get(0);
            mapperServiceName = parameters.get(JahiaOAuthConstants.MAPPER_SERVICE_NAME).get(0);
            boolean isActivate = Boolean.parseBoolean(parameters.get(JahiaOAuthConstants.PROPERTY_IS_ACTIVATE).get(0));
            if (isActivate && !parameters.containsKey(JahiaOAuthConstants.PROPERTY_MAPPING)) {
                response.put("error", "mapping is missing");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            List<String> mapping = (parameters.containsKey(JahiaOAuthConstants.PROPERTY_MAPPING))?parameters.get(JahiaOAuthConstants.PROPERTY_MAPPING):new ArrayList<String>();

            JCRNodeWrapper mappersNode = renderContext.getSite().getNode(JahiaOAuthConstants.JAHIA_OAUTH_NODE_NAME).getNode(connectorServiceName).getNode(JahiaOAuthConstants.MAPPERS_NODE_NAME);

            JCRNodeWrapper currentMapperNode;
            if (!mappersNode.hasNode(mapperServiceName)) {
                currentMapperNode = mappersNode.addNode(mapperServiceName, parameters.get(JahiaOAuthConstants.NODE_TYPE).get(0));
                session.save();
            } else {
                currentMapperNode = mappersNode.getNode(mapperServiceName);
            }

            currentMapperNode.setProperty(JahiaOAuthConstants.PROPERTY_IS_ACTIVATE, isActivate);
            currentMapperNode.setProperty(JahiaOAuthConstants.PROPERTY_MAPPING, mapping.toString());

            session.save();
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    public void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }
}
