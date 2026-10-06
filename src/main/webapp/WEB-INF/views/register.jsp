<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="auth-layout">
    <div class="auth-story"><span class="project-label">Patient registration</span><h1>Create an account</h1><p class="lead">Register first, verify the one-time code, and then complete the health profile used by the clinical modules.</p><div class="auth-feature"><div><strong>1</strong><span>Enter account details</span></div><div><strong>2</strong><span>Verify the six-digit code</span></div><div><strong>3</strong><span>Complete the patient profile</span></div></div></div>
    <div class="auth-form">
        <p class="eyebrow">New patient</p><h2>Account details</h2>
        <form method="post" action="<c:url value='/register'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
            <div class="field"><label for="fullName">Full name</label><input id="fullName" name="fullName" required maxlength="120" autocomplete="name"></div>
            <div class="field"><label for="email">Email</label><input id="email" name="email" type="email" required maxlength="160" autocomplete="email"></div>
            <div class="field"><label for="phone">Phone</label><input id="phone" name="phone" type="tel" maxlength="30" autocomplete="tel" required></div>
            <div class="field"><label for="password">Password</label><input id="password" name="password" type="password" required minlength="8" maxlength="100" autocomplete="new-password"><small>At least 8 characters with a letter and number.</small></div>
            <button class="button" type="submit">Create account</button>
        </form>
        <p class="muted">Already registered? <a href="<c:url value='/login'/>">Sign in</a></p>
    </div>
</section>
<%@ include file="includes/footer.jsp" %>
