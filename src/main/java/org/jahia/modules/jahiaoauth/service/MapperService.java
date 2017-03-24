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
 * Service that can be implemented by mappers
 *
 * @author dgaillard
 */
public interface MapperService {
    /**
     * Return the list of properties of the mapper
     * @return
     */
    List<Map<String, Object>> getProperties();

    /**
     * This method is called by JahiaOAuthService once the authentication process is done and we have the results so the mapper can use the results
     * @param mapperResult
     */
    void executeMapper(Map<String, Object> mapperResult);
}
