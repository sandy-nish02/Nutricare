<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="auth-layout">
    <div class="auth-story"><span class="project-label">Account recovery</span><h1>Forgot your password?</h1><p class="lead">Enter the email you registered with. A six-digit reset code is sent to the phone number on that account and expires after five minutes.</p></div>
    <div class="auth-form">
        <p class="eyebrow">Step 1 of 2</p><h2>Request a reset code</h2>
        <form method="post" action="<c:url value='/forgot-password'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
            <div class="field"><label for="email">Email</label><input id="email" name="email" type="email" autocomplete="username" required maxlength="160"></div>
            <button class="button" type="submit">Send reset code</button>
        </form>
        <p class="muted"><a href="<c:url value='/login'/>">Back to sign in</a></p>
    </div>
</section>
<%@ include file="includes/footer.jsp" %>
