<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="auth-layout">
    <div class="auth-story"><span class="project-label">Account verification</span><h1>Verify the patient account</h1><p class="lead">The code expires after <c:out value="${validityMinutes}"/> minutes and stops working after five wrong attempts. In this classroom build it is displayed on screen instead of being sent through a live SMS provider.</p></div>
    <div class="auth-form"><p class="eyebrow">Six-digit code</p><h2>Enter verification code</h2>
        <c:if test="${not empty sessionScope.demoOtp}"><div class="demo-box"><strong>Demonstration OTP</strong><br><span class="otp-code"><c:out value="${sessionScope.demoOtp}"/></span></div></c:if>
        <form class="section-gap" method="post" action="<c:url value='/verify'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
            <div class="field"><label for="otp">Six-digit code</label><input id="otp" name="otp" inputmode="numeric" pattern="[0-9]{6}" minlength="6" maxlength="6" autocomplete="one-time-code" required></div>
            <button class="button" type="submit">Verify and activate</button>
        </form>
        <form class="section-gap" method="post" action="<c:url value='/verify/resend'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
            <button class="button button-secondary" type="submit">Send a new code</button>
        </form>
    </div>
</section>
<%@ include file="includes/footer.jsp" %>
