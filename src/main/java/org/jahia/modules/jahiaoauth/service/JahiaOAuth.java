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
package org.jahia.modules.jahiaoauth.service;

import org.jahia.services.content.JCRNodeWrapper;
import org.json.JSONArray;
import org.json.JSONException;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public interface JahiaOAuth {
    String getAuthorizationUrl(JCRNodeWrapper jahiaOAuthNode, String serviceName, String sessionId) throws RepositoryException;

    void extractTokenAndExecuteMappers(JCRNodeWrapper jahiaOAuthNode, String serviceName, String token, String state) throws Exception;

    void addDataToOAuthBaseApiMap(Map<String, Map<String, Object>> dataToLoad, String serviceName);

    void addDataToOAuthMapperPropertiesMap(List<Map<String, Object>> mapperProperties, String mapperServiceName);

    JSONArray getConnectorProperties(String serviceName) throws JSONException;

    JSONArray getMapperProperties(String mapperServiceName) throws JSONException;

    HashMap<String, Object> getMapperResults(String mapperServiceName, String sessionId);

    void updateCacheEntry(String originalSessionId, String newSessionId);
}
