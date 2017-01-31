<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="css" resources="jahia-oauth/style.css"/>
<template:addResources type="css" resources="jahia-oauth/vendor/angular-material.css"/>

<template:addResources type="javascript" resources="jahia-oauth/vendor/angular.js,
                                                    jahia-oauth/vendor/angular-animate.js,
                                                    jahia-oauth/vendor/angular-aria.js,
                                                    jahia-oauth/vendor/angular-messages.js,
                                                    jahia-oauth/vendor/angular-material.js,
                                                    jahia-oauth/vendor/angular-route.js,
                                                    jahia-oauth/i18n.js,
                                                    jahia-oauth/app.js"/>

<template:addResources>
    <script>
        var jahiaOAuthContext = {
            baseEdit: '${url.context}${url.baseEdit}',
            sitePath: '${renderContext.siteInfo.sitePath}'
        }
    </script>
</template:addResources>

<div ng-app="JahiaOAuth" layout="column" layout-fill>
    <div layout="row">
        <md-toolbar ng-controller="headerController">
            <div class="md-toolbar-tools">
                <md-button class="md-icon-button" ng-show="isMapperView()" ng-click="goToConnectors()">
                    <md-icon>keyboard_arrow_left</md-icon>
                </md-button>
                <h2>
                    <span message-key="joant_oauthConnectorSiteSettings"></span>
                </h2>
            </div>
        </md-toolbar>
    </div>

    <div ng-view></div>

    <script type="text/ng-template" id="connectors.html">
        <jcr:sql var="oauthConnectorsViews" sql="SELECT * FROM [joamix:oauthConnectorView]"/>
        <c:set var="siteHasConnector" value="false"/>
        <c:forEach items="${oauthConnectorsViews.nodes}" var="connectorView">
            <c:set var="currentModuleName" value="${fn:substringAfter(connectorView.path, '/modules/')}"/>
            <c:set var="currentModuleName" value="${fn:substringBefore(currentModuleName, '/')}"/>
            <c:if test="${functions:contains(renderContext.site.installedModules, currentModuleName)}">
                <c:set var="siteHasConnector" value="true"/>
                <template:module node="${connectorView}" />
            </c:if>
        </c:forEach>
        <c:if test="${not siteHasConnector}">
            <md-card>
                <md-card-content>
                    <span message-key="joant_oauthConnectorSiteSettings.connector.notFound"></span>
                </md-card-content>
            </md-card>
        </c:if>
    </script>

    <script type="text/ng-template" id="mappers.html">
        <jcr:sql var="oauthMappersViews" sql="SELECT * FROM [joamix:oauthMapperView]"/>
        <c:set var="siteHasMapper" value="false"/>
        <c:forEach items="${oauthMappersViews.nodes}" var="mapperView">
            <c:set var="currentModuleName" value="${fn:substringAfter(mapperView.path, '/modules/')}"/>
            <c:set var="currentModuleName" value="${fn:substringBefore(currentModuleName, '/')}"/>
            <c:if test="${functions:contains(renderContext.site.installedModules, currentModuleName)}">
                <c:set var="siteHasMapper" value="true"/>
                <template:module node="${mapperView}"/>
            </c:if>
        </c:forEach>
        <c:if test="${not siteHasMapper}">
            <md-card>
                <md-card-content>
                    <span message-key="joant_oauthConnectorSiteSettings.mapper.notFound"></span>
                </md-card-content>
            </md-card>
        </c:if>
    </script>
</div>

