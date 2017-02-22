package org.jahia.modules.jahiaoauth.tag;

import org.apache.commons.lang.StringUtils;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaException;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author dgaillard
 */
public class JahiaOAuthFunctions {
    private static Logger logger = LoggerFactory.getLogger(JahiaOAuthFunctions.class);

    private static JahiaTemplateManagerService jahiaTemplateManagerService;

    public static Boolean isModuleActiveOnSite(String siteKey, String path) throws JahiaException {
        List<JahiaTemplatesPackage> jahiaTemplatesPackageList = jahiaTemplateManagerService.getInstalledModulesForSite(siteKey, false, true,false);

        for (JahiaTemplatesPackage jahiaTemplatesPackage : jahiaTemplatesPackageList) {
            if (StringUtils.startsWith(path, jahiaTemplatesPackage.getRootFolderPath() + "/" + jahiaTemplatesPackage.getVersion().toString())) {
                return true;
            }
        }

        return false;
    }

    public void setJahiaTemplateManagerService(JahiaTemplateManagerService jahiaTemplateManagerService) {
        this.jahiaTemplateManagerService = jahiaTemplateManagerService;
    }
}
