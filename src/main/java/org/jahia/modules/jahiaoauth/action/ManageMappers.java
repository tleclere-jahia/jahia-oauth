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

    private JahiaOAuth jahiaOAuth;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        String action = parameters.get("action").get(0);
        JSONObject response = new JSONObject();
        if (action.equals("getConnectorProperties")) {
            response.put("connectorProperties", jahiaOAuth.getConnectorProperties(parameters.get(Constants.SERVICE_NAME).get(0)));
        } else if (action.equals("getMapperProperties")) {
            response.put("mapperProperties", jahiaOAuth.getMapperProperties(parameters.get(Constants.MAPPER_SERVICE_NAME).get(0)));
        } else if (action.equals("getMapperMapping")) {
            if (!parameters.containsKey(Constants.MAPPER_SERVICE_NAME)
                    || !parameters.containsKey(Constants.SERVICE_NAME)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            String serviceName = parameters.get(Constants.SERVICE_NAME).get(0);
            String mapperServiceName = parameters.get(Constants.MAPPER_SERVICE_NAME).get(0);

            JCRNodeWrapper mappersNode = renderContext.getSite().getNode(Constants.JAHIA_OAUTH_NODE_NAME).getNode(serviceName).getNode(Constants.MAPPERS_NODE_NAME);

            if (!mappersNode.hasNode(mapperServiceName)) {
                return new ActionResult(HttpServletResponse.SC_OK, null, response);
            }

            JCRNodeWrapper mapperNode = mappersNode.getNode(mapperServiceName);
            JSONArray jsonArrayMapping = new JSONArray(mapperNode.getPropertyAsString(Constants.PROPERTY_MAPPING));
            response.put(Constants.PROPERTY_IS_ACTIVATE, mapperNode.getProperty(Constants.PROPERTY_IS_ACTIVATE).getBoolean());
            response.put(Constants.PROPERTY_MAPPING, jsonArrayMapping);

        } else if (action.equals("setMapperMapping")) {
            if (!parameters.containsKey(Constants.PROPERTY_IS_ACTIVATE)
                    || !parameters.containsKey(Constants.MAPPER_SERVICE_NAME)
                    || !parameters.containsKey(Constants.SERVICE_NAME)
                    || !parameters.containsKey(Constants.NODE_TYPE)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            boolean isActivate = Boolean.parseBoolean(parameters.get(Constants.PROPERTY_IS_ACTIVATE).get(0));
            if (isActivate && !parameters.containsKey(Constants.PROPERTY_MAPPING)) {
                response.put("error", "mapping is missing");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            List<String> mapping = (parameters.containsKey(Constants.PROPERTY_MAPPING))?parameters.get(Constants.PROPERTY_MAPPING):new ArrayList<String>();
            String serviceName = parameters.get(Constants.SERVICE_NAME).get(0);
            String mapperServiceName = parameters.get(Constants.MAPPER_SERVICE_NAME).get(0);

            JCRNodeWrapper mappersNode = renderContext.getSite().getNode(Constants.JAHIA_OAUTH_NODE_NAME).getNode(serviceName).getNode(Constants.MAPPERS_NODE_NAME);

            JCRNodeWrapper currentMapperNode;
            if (!mappersNode.hasNode(mapperServiceName)) {
                currentMapperNode = mappersNode.addNode(mapperServiceName, parameters.get(Constants.NODE_TYPE).get(0));
                mappersNode.getSession().save();
            } else {
                currentMapperNode = mappersNode.getNode(mapperServiceName);
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
