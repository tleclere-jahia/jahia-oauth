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
