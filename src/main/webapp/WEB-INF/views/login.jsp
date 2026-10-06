<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="auth-layout">
    <div class="auth-story">
        <span class="project-label">SE2030 Software Engineering Project</span>
        <h1>NutriCare</h1>
        <p class="lead">Web-Based Diet Planning &amp; Health Check-up System</p>
        <div class="auth-feature">
            <div><strong>01</strong><span>Patient records and profiles</span></div>
            <div><strong>02</strong><span>Appointments and clinical check-ups</span></div>
            <div><strong>03</strong><span>Diet planning, payments, and staff access</span></div>
        </div>
    </div>
    <div class="auth-form">
        <p class="eyebrow">Account access</p><h2>Sign in</h2>
        <form method="post" action="<c:url value='/login'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
            <div class="field"><label for="email">Email</label><input id="email" name="email" type="email" autocomplete="username" required maxlength="160"></div>
            <div class="field"><label for="password">Password</label><input id="password" name="password" type="password" autocomplete="current-password" required maxlength="100"></div>
            <button class="button" type="submit">Sign in</button>
        </form>
        <p class="muted"><a href="<c:url value='/forgot-password'/>">Forgot your password?</a><br>New patient? <a href="<c:url value='/register'/>">Create an account</a></p>
        <div class="demo-box"><strong>Demonstration accounts</strong><br>Patient: patient@nutricare.lk<br>Admin: admin@nutricare.lk<br>Doctor: doctor@nutricare.lk<br>Nutritionist: nutritionist@nutricare.lk<br>Finance: finance@nutricare.lk<br>Receptionist: reception@nutricare.lk<br>Password: <strong>NutriCare123!</strong></div>
    </div>
</section>
<%@ include file="includes/footer.jsp" %>
