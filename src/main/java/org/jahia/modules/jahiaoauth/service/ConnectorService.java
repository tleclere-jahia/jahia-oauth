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

import java.util.List;
import java.util.Map;

/**
 * Service to be implemented by a connector to allow Jahia OAuth to work
 *
 * @author dgaillard
 */
public interface ConnectorService {
    /**
     * This method get the connector service name
     * @return String connector service name
     */
    String getServiceName();

    /**
     * This method return the url that will allow Jahia OAuth to get the user data
     * @return String url to request to get the user data
     */
    String getProtectedResourceUrl();

    /**
     * This method get the list of available properties with this connector
     * @return List the list of available properties
     */
    List<Map<String, Object>> getAvailableProperties();
}
