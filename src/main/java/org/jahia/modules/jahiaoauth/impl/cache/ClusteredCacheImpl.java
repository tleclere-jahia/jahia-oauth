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

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Jahia;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthCacheService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.HashMap;

/**
 * @author dgaillard
 */
@Component( name = "org.jahia.module.jahiaoauth.impl.cache.ClusteredCacheImpl",
            service = JahiaOAuthCacheService.class,
            property = {
                Constants.SERVICE_PID + "=org.jahia.module.jahiaoauth.impl.cache.ClusteredCacheImpl",
                Constants.SERVICE_DESCRIPTION + "=Clustered cache service implementation using Hazelcast",
                Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
                "clustered=true"
            },
            immediate = true)
public class ClusteredCacheImpl implements JahiaOAuthCacheService {
    private HazelcastInstance hazelcastInstance;

    @Activate
    protected void activate() throws Exception {
        Config config = hazelcastInstance.getConfig();
        MapConfig mapConfig = new MapConfig(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).setTimeToLiveSeconds(180);
        config.addMapConfig(mapConfig);
    }

    @Reference(service = HazelcastInstance.class)
    protected void bindHazelcastService(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public void cacheMapperResults(String cacheKey, HashMap<String, Object> mapperResult) {
        if (hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).containsKey(cacheKey)) {
            hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).replace(cacheKey, mapperResult);
        } else {
            hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).set(cacheKey, mapperResult);
        }
    }

    @Override
    public HashMap<String, Object> getMapperResultsCacheEntry(String cacheKey) {
        HashMap<String, Object> mapperResult = null;
        if (hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).containsKey(cacheKey)) {
            mapperResult = (HashMap<String, Object>) hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).get(cacheKey);
        }
        return mapperResult;
    }

    @Override
    public void updateCacheEntry(String originalSessionId, String newSessionId) {
        for (Object key : hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).keySet()) {
            String keyAsString = (String) key;
            if (StringUtils.endsWith(keyAsString, originalSessionId)) {
                String newKey = StringUtils.substringBefore(keyAsString, originalSessionId) + newSessionId;
                HashMap<String, Object> mapperResult = (HashMap<String, Object>) hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).get(key);
                hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).remove(key);
                hazelcastInstance.getMap(JahiaOAuthConstants.JAHIA_OAUTH_USER_CACHE).set(newKey, mapperResult);
            }
        }
    }
}
