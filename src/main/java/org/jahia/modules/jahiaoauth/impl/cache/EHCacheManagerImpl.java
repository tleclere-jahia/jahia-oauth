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
package org.jahia.modules.jahiaoauth.impl.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthCacheService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.cache.ModuleClassLoaderAwareCacheEntry;
import org.jahia.services.cache.ehcache.EhCacheProvider;


import java.util.HashMap;
import java.util.List;

/**
 * @author dgaillard
 */
public class EHCacheManagerImpl implements JahiaOAuthCacheService {
    private EhCacheProvider ehCacheProvider;
    private CacheManager cacheManager;
    private Ehcache userCache;

    protected void init() {
        cacheManager = ehCacheProvider.getCacheManager();
        userCache = cacheManager.getCache(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE);

        if (userCache == null) {
            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setName(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE);
            cacheConfiguration.setTimeToLiveSeconds(180);
            // Create a new cache with the configuration
            Ehcache ehcache = new Cache(cacheConfiguration);
            ehcache.setName(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE);
            // Cache name has been set now we can initialize it by putting it in the manager.
            // Only Cache manager is initializing caches.
            userCache = cacheManager.addCacheIfAbsent(ehcache);
        }
    }

    public void destroy() {
        // flush
        if (userCache != null) {
            userCache.removeAll();
        }

        cacheManager.removeCache(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE);
    }

    @Override
    public void cacheMapperResults(String userSessionId, HashMap<String, Object> mapperResult) {
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(mapperResult, "jahia-oauth");
        userCache.put(new Element(userSessionId, cacheEntry));
    }

    @Override
    public HashMap<String, Object> getMapperResultsCacheEntry(String cacheKey) {
        return (HashMap<String, Object>) CacheHelper.getObjectValue(userCache, cacheKey);
    }

    @Override
    public void updateCacheEntry(String originalSessionId, String newSessionId) {
        for (String key : (List<String>) userCache.getKeys()) {
            if (StringUtils.endsWith(key, originalSessionId)) {
                String newKey = StringUtils.substringBefore(key, originalSessionId) + newSessionId;
                HashMap<String, Object> mapperResults = getMapperResultsCacheEntry(key);
                if (mapperResults != null) {
                    cacheMapperResults(newKey, mapperResults);
                }
            }
        }
    }

    public void setEhCacheProvider(EhCacheProvider ehCacheProvider) {
        this.ehCacheProvider = ehCacheProvider;
    }
}
