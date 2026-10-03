<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="NutriCare diet planning and health check-up system">
    <title><c:out value="${pageTitle}"/> | NutriCare</title>
    <c:url var="appCssUrl" value="/assets/app.css"><c:param name="v" value="1.3.0"/></c:url>
    <c:url var="appJsUrl" value="/assets/app.js"><c:param name="v" value="1.3.0"/></c:url>
    <link rel="stylesheet" href="${appCssUrl}">
    <script defer src="${appJsUrl}"></script>
</head>
<body>
<header class="topbar">
    <a class="brand" href="<c:url value='${empty sessionScope.userId ? "/" : "/dashboard"}'/>">
        <span class="brand-mark">N</span>
        <span>NutriCare<small>Health, planned well</small></span>
    </a>
    <c:if test="${empty sessionScope.userId}">
        <nav class="main-nav public-nav" aria-label="Main navigation">
            <a href="<c:url value='/'/>">Home</a>
        </nav>
        <div class="account">
            <a class="button button-quiet" href="<c:url value='/login'/>">Sign in</a>
            <a class="button button-light" href="<c:url value='/register'/>">Create account</a>
        </div>
    </c:if>
    <c:if test="${not empty sessionScope.userId}">
        <button class="nav-toggle" type="button" aria-label="Open menu" aria-expanded="false">Menu</button>
        <nav class="main-nav" aria-label="Main navigation">
            <a href="<c:url value='/dashboard'/>">Dashboard</a>
            <c:if test="${sessionScope.role == 'PATIENT' || sessionScope.role == 'DOCTOR' || sessionScope.role == 'NUTRITIONIST' || sessionScope.role == 'RECEPTIONIST' || sessionScope.role == 'ADMIN'}"><a href="<c:url value='/appointments'/>">Appointments</a></c:if>
            <c:if test="${sessionScope.role == 'PATIENT' || sessionScope.role == 'DOCTOR' || sessionScope.role == 'NUTRITIONIST' || sessionScope.role == 'ADMIN'}"><a href="<c:url value='/checkups'/>">Check-ups</a></c:if>
            <c:if test="${sessionScope.role == 'PATIENT' || sessionScope.role == 'NUTRITIONIST' || sessionScope.role == 'ADMIN'}"><a href="<c:url value='/diets'/>">Diet plans</a></c:if>
            <c:if test="${sessionScope.role == 'PATIENT' || sessionScope.role == 'FINANCE' || sessionScope.role == 'ADMIN'}"><a href="<c:url value='/payments'/>">Payments</a></c:if>
            <c:if test="${sessionScope.role == 'ADMIN'}"><a href="<c:url value='/staff'/>">Staff</a><a href="<c:url value='/audit'/>">Audit</a></c:if>
            <a href="<c:url value='/profile'/>">${sessionScope.role == 'PATIENT' ? 'Profile' : 'Account'}</a>
        </nav>
        <div class="account">
            <span><c:out value="${sessionScope.fullName}"/><small><c:out value="${sessionScope.role}"/></small></span>
            <form method="post" action="<c:url value='/logout'/>">
                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
                <button class="button button-quiet" type="submit">Sign out</button>
            </form>
        </div>
    </c:if>
</header>
<main class="page-shell ${not empty shellClass ? shellClass : (empty sessionScope.userId ? 'public-shell' : '')}">
    <c:if test="${not empty flashSuccess}"><div class="notice notice-success" role="status"><c:out value="${flashSuccess}"/></div></c:if>
    <c:if test="${not empty flashError}"><div class="notice notice-error" role="alert"><c:out value="${flashError}"/></div></c:if>
