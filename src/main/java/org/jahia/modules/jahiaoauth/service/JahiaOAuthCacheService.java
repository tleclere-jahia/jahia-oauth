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

import java.util.HashMap;

/**
 * Service to be implemented by Mapper that need to access the data in the cache
 *
 * @author dgaillard
 */
public interface JahiaOAuthCacheService {
    /**
     * This method will register the results for the mapper in the cache during 180 seconds
     * @param cacheKey String the cache key built using the user session ID and the mapper service name
     * @param mapperResult HashMap that contains the result for the mapper
     */
    void cacheMapperResults(String cacheKey, HashMap<String, Object> mapperResult);

    /**
     * This method get the results of the mapper in the cache
     * @param cacheKey String the cache key built using the user session ID and the mapper service name
     * @return HashMap that contains the result for the mapper
     */
    HashMap<String, Object> getMapperResultsCacheEntry(String cacheKey);

    /**
     * This method will update all the cache entries that contains the original session ID with the new session ID
     * This method should be use is the session has been invalidated to ensure that other mappers will be able to find the results with the new session ID
     * @param originalSessionId String of the original session ID
     * @param newSessionId String of the new session ID
     */
    void updateCacheEntry(String originalSessionId, String newSessionId);
}
