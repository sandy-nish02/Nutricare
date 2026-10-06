<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="auth-layout">
    <div class="auth-story"><span class="project-label">Account recovery</span><h1>Choose a new password</h1><p class="lead">Enter the six-digit code from your phone and a new password. Classroom build: the "SMS" is printed in the NutriCare server console window.</p></div>
    <div class="auth-form">
        <p class="eyebrow">Step 2 of 2</p><h2>Reset password</h2>
        <form method="post" action="<c:url value='/reset-password'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
            <div class="field"><label for="otp">Six-digit code</label><input id="otp" name="otp" inputmode="numeric" pattern="[0-9]{6}" minlength="6" maxlength="6" autocomplete="one-time-code" required></div>
            <div class="field"><label for="newPassword">New password</label><input id="newPassword" name="newPassword" type="password" required minlength="8" maxlength="100" autocomplete="new-password"><small>At least 8 characters with a letter and a number.</small></div>
            <div class="field"><label for="confirmPassword">Confirm new password</label><input id="confirmPassword" name="confirmPassword" type="password" required minlength="8" maxlength="100" autocomplete="new-password"></div>
            <button class="button" type="submit">Reset password</button>
        </form>
        <p class="muted"><a href="<c:url value='/forgot-password'/>">Request a new code</a> · <a href="<c:url value='/login'/>">Back to sign in</a></p>
    </div>
</section>
<%@ include file="includes/footer.jsp" %>
