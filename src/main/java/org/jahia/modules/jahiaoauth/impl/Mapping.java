package org.jahia.modules.jahiaoauth.impl;

public class Mapping {
    private String mapperPropertyName;
    private boolean mapperPropertyMandatory;
    private String connectorPropertyName;
    private String connectorPropertyType;
    private String connectorPropertyFormat;

    public String getMapperPropertyName() {
        return mapperPropertyName;
    }

    public void setMapperPropertyName(String mapperPropertyName) {
        this.mapperPropertyName = mapperPropertyName;
    }

    public boolean isMapperPropertyMandatory() {
        return mapperPropertyMandatory;
    }

    public void setMapperPropertyMandatory(boolean mapperPropertyMandatory) {
        this.mapperPropertyMandatory = mapperPropertyMandatory;
    }

    public String getConnectorPropertyName() {
        return connectorPropertyName;
    }

    public void setConnectorPropertyName(String connectorPropertyName) {
        this.connectorPropertyName = connectorPropertyName;
    }

    public String getConnectorPropertyType() {
        return connectorPropertyType;
    }

    public void setConnectorPropertyType(String connectorPropertyType) {
        this.connectorPropertyType = connectorPropertyType;
    }

    public String getConnectorPropertyFormat() {
        return connectorPropertyFormat;
    }

    public void setConnectorPropertyFormat(String connectorPropertyFormat) {
        this.connectorPropertyFormat = connectorPropertyFormat;
    }
}
