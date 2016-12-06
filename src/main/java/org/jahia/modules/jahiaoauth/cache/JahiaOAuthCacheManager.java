package org.jahia.modules.jahiaoauth.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.cache.ModuleClassLoaderAwareCacheEntry;
import org.jahia.services.cache.ehcache.EhCacheProvider;

import java.util.HashMap;

/**
 * @author dgaillard
 */
public class JahiaOAuthCacheManager {
    private static final String JAHIA_OAUTH_USER_CACHE = "JahiaOAuthUserCache";

    private EhCacheProvider ehCacheProvider;
    private CacheManager cacheManager;
    private Ehcache userCache;

    public JahiaOAuthCacheManager() {}

    public void start() {
        cacheManager = ehCacheProvider.getCacheManager();
        userCache = cacheManager.getCache(JAHIA_OAUTH_USER_CACHE );

        if (userCache == null) {
            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setName(JAHIA_OAUTH_USER_CACHE);
            cacheConfiguration.setTimeToLiveSeconds(180);
            // Create a new cache with the configuration
            Ehcache ehcache = new Cache(cacheConfiguration);
            ehcache.setName(JAHIA_OAUTH_USER_CACHE);
            // Cache name has been set now we can initialize it by putting it in the manager.
            // Only Cache manager is initializing caches.
            userCache = cacheManager.addCacheIfAbsent(ehcache);
        }
    }

    public void stop() {
        // flush
        if (userCache != null) {
            userCache.removeAll();
        }

        cacheManager.removeCache(JAHIA_OAUTH_USER_CACHE);
    }

    public void cacheMapperResults(String userSessionId, HashMap<String, Object> mapperResult) {
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(mapperResult, "jahia-oauth");
        userCache.put(new Element(userSessionId, cacheEntry));
    }

    public HashMap<String, Object> getMapperResultsCacheEntry(String cacheKey) {
        return (HashMap<String, Object>) CacheHelper.getObjectValue(userCache, cacheKey);
    }

    public void setEhCacheProvider(EhCacheProvider ehCacheProvider) {
        this.ehCacheProvider = ehCacheProvider;
    }
}
