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
package org.jahia.modules.jahiaoauth.decorator;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.LazyPropertyIterator;
import org.jahia.services.content.decorator.JCRNodeDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ConnectorNode extends JCRNodeDecorator {
    private transient static Logger logger = LoggerFactory.getLogger(ConnectorNode.class);

    private boolean alwaysAllowSystem = true;
    private ArrayList<String> protectedProperties = new ArrayList<String>() {{
        add("apiKey");
        add("apiSecret");
        add("callbackUrl");
        add("scope");
    }};

    public ConnectorNode(JCRNodeWrapper node) {
        super(node);
    }

    private class FilteredIterator extends LazyPropertyIterator {
        public FilteredIterator() throws RepositoryException {
            super(ConnectorNode.this, ConnectorNode.this.getSession().getLocale());
        }

        public FilteredIterator(String singlePattern) throws RepositoryException {
            super(ConnectorNode.this, ConnectorNode.this.getSession().getLocale(),
                    singlePattern);
        }

        public FilteredIterator(String[] patternArray) throws RepositoryException {
            super(ConnectorNode.this, ConnectorNode.this.getSession().getLocale(),
                    patternArray);
        }

        @Override
        public boolean hasNext() {
            while (super.hasNext()) {
                try {
                    if (!canGetProperty(tempNext.getName())) {
                        tempNext = null;
                    } else {
                        return true;
                    }
                } catch (RepositoryException e) {
                    tempNext = null;
                    logger.error("Cannot read property", e);
                }
            }
            return false;
        }
    }

    private boolean canReadProperty(String propertyName) throws RepositoryException {
        return !protectedProperties.contains(propertyName);
    }

    private boolean canGetProperty(String propertyName) throws RepositoryException {
        return alwaysAllowSystem && getSession().isSystem() || canReadProperty(propertyName) || getUser().hasPermission("canSetupJahiaOAuth");
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        return (alwaysAllowSystem && getSession().isSystem()) ? super.getProperties() : new FilteredIterator();
    }

    @Override
    public PropertyIterator getProperties(String s) throws RepositoryException {
        return (alwaysAllowSystem && getSession().isSystem()) ? super.getProperties(s) : new FilteredIterator(s);
    }

    @Override
    public PropertyIterator getProperties(String[] strings) throws RepositoryException {
        return (alwaysAllowSystem && getSession().isSystem()) ? super.getProperties(strings) : new FilteredIterator(strings);
    }

    @Override
    public Map<String, String> getPropertiesAsString() throws RepositoryException {
        return (alwaysAllowSystem && getSession().isSystem()) ? super.getPropertiesAsString() : Maps.filterKeys(super.getPropertiesAsString(),
                new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        try {
                            return canGetProperty(input);
                        } catch (RepositoryException e) {
                            return false;
                        }
                    }
                });
    }

    @Override
    public JCRPropertyWrapper getProperty(String s) throws PathNotFoundException, RepositoryException {
        if (!canGetProperty(s)) {
            throw new PathNotFoundException(s);
        }

        return super.getProperty(s);
    }

    @Override
    public String getPropertyAsString(String name) {
        try {
            return canGetProperty(name) ? super.getPropertyAsString(name) : null;
        } catch (RepositoryException e) {
            logger.error("Could not get property [" + name + "] as string", e);
            return null;
        }
    }

    @Override
    public boolean hasProperty(String s) throws RepositoryException {
        return super.hasProperty(s) && canGetProperty(s);
    }
}
