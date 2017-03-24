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

import org.jahia.modules.jahiaoauth.service.JahiaOAuthCacheService;
import org.jahia.osgi.BundleUtils;
import org.jahia.settings.SettingsBean;

import java.util.HashMap;

/**
 * @author dgaillard
 */
public class JahiaOAuthCacheServiceImpl implements JahiaOAuthCacheService {
    private JahiaOAuthCacheService defaultCacheService;
    private JahiaOAuthCacheService service;
    private SettingsBean settingsBean;

    public void initService() {
        service = settingsBean.isClusterActivated() ?
                BundleUtils.getOsgiService(JahiaOAuthCacheService.class, "(clustered=true)") : null;

        if (service == null) {
         service = defaultCacheService;
        }
    }

    @Override
    public void cacheMapperResults(String userSessionId, HashMap<String, Object> mapperResult) {
        service.cacheMapperResults(userSessionId, mapperResult);
    }

    @Override
    public HashMap<String, Object> getMapperResultsCacheEntry(String cacheKey) {
        return service.getMapperResultsCacheEntry(cacheKey);
    }

    @Override
    public void updateCacheEntry(String originalSessionId, String newSessionId) {
        service.updateCacheEntry(originalSessionId, newSessionId);
    }

    public void setDefaultCacheService(JahiaOAuthCacheService defaultCacheService) {
        this.defaultCacheService = defaultCacheService;
    }

    public void setSettingsBean(SettingsBean settingsBean) {
        this.settingsBean = settingsBean;
    }
}
