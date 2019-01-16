/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
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
