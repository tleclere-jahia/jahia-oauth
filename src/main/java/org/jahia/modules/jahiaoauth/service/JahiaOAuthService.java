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
 * Service to be used by connectors and mappers
 *
 * @author dgaillard
 */
public interface JahiaOAuthService {
    /**
     * This method will get the authorization URL so a connector can display the authentication popup to the user
     * @param jahiaOAuthNode JCRNodeWrapper main node of Jahia OAuth that contains the connectors node
     * @param connectorServiceName String the service name of the connector
     * @param sessionId String user session ID to be able to identify the token on the callback the session ID of the user is added to the request
     * @return String authorization URL
     * @throws RepositoryException
     */
    String getAuthorizationUrl(JCRNodeWrapper jahiaOAuthNode, String connectorServiceName, String sessionId) throws RepositoryException;

    /**
     * This method will extract the token and execute the mappers action
     * @param jahiaOAuthNode JCRNodeWrapper main node of Jahia OAuth that contains the connectors node
     * @param connectorServiceName String the service name of the connector
     * @param token String token send by the OAuth API
     * @param state String state send back by OAuth API in this context it's the user session ID
     * @throws Exception
     */
    void extractAccessTokenAndExecuteMappers(JCRNodeWrapper jahiaOAuthNode, String connectorServiceName, String token, String state) throws Exception;

    /**
     * This method will get the mapper results in the cache
     * @param mapperServiceName String mapper service name
     * @param sessionId String user session ID
     * @return HashMap of the results
     */
    HashMap<String, Object> getMapperResults(String mapperServiceName, String sessionId);

    /**
     * This method will return the URL of the result page so the user can be inform of the succes or not of his authentication
     * @param siteUrl String current site URL
     * @param isAuthenticate Boolean will be added to the URL as parameter
     * @return String URL of the result page
     */
    String getResultUrl(String siteUrl, Boolean isAuthenticate);

    /**
     * This method will refresh the access token of the user
     * @param jahiaOAuthNode JCRNodeWrapper main node of Jahia OAuth that contains the connectors node
     * @param connectorServiceName String the service name of the connector
     * @param refreshToken String the refresh token
     * @return Map containing the data of the access token
     * @throws Exception
     */
    Map<String, Object> refreshAccessToken(JCRNodeWrapper jahiaOAuthNode, String connectorServiceName, String refreshToken) throws Exception;

    /**
     * This method request the user data for a given mapper and a given connector
     * @param jahiaOAuthNode JCRNodeWrapper main node of Jahia OAuth that contains the connectors node
     * @param connectorServiceName String the service name of the connector
     * @param mapperServiceName String the service name of the mapper
     * @param refreshToken String the refresh token
     * @return Map containing the result of the mapper
     * @throws Exception
     */
    Map<String, Object> requestUserData(JCRNodeWrapper jahiaOAuthNode, String connectorServiceName, String mapperServiceName, String refreshToken) throws Exception;
}
