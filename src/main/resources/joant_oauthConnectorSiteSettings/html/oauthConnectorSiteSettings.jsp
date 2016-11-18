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
        <md-toolbar>
            <div class="md-toolbar-tools">
                <h2>
                    <span>Jahia OAuth Settings</span>
                </h2>
            </div>
        </md-toolbar>
    </div>

    <div>
        <jcr:sql var="oauthConnectorsViews" sql="SELECT * FROM [joamix:oauthConnectorView]"/>
        <c:forEach items="${oauthConnectorsViews.nodes}" var="connectorView">
            <template:module node="${connectorView}"/>
        </c:forEach>
    </div>
</div>
