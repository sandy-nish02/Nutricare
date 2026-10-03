<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="card error-card"><p class="eyebrow">Request error</p><h1><c:out value="${pageTitle}"/></h1><p class="lead"><c:out value="${errorMessage}"/></p><c:choose><c:when test="${empty sessionScope.userId}"><a class="button section-gap" href="<c:url value='/login'/>">Return to sign in</a></c:when><c:otherwise><a class="button section-gap" href="<c:url value='/dashboard'/>">Return to dashboard</a></c:otherwise></c:choose></section>
<%@ include file="includes/footer.jsp" %>
