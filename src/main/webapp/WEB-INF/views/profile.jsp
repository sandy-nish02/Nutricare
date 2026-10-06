<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<c:choose>
<c:when test="${sessionScope.role == 'PATIENT'}">
<section class="page-heading"><div><p class="eyebrow">Patient record</p><h1>My health profile</h1><p class="lead">Keep this accurate. Doctors and nutritionists use these values when making care decisions.</p></div></section>
<section class="card">
    <form method="post" action="<c:url value='/profile'/>">
        <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
        <div class="field-row">
            <div class="field"><label for="fullName">Full name</label><input id="fullName" name="fullName" value="<c:out value='${patient.full_name}'/>" required maxlength="120"></div>
            <div class="field"><label for="phone">Phone</label><input id="phone" name="phone" type="tel" value="<c:out value='${patient.phone}'/>" required maxlength="30"></div>
            <div class="field"><label for="dob">Date of birth</label><input id="dob" name="dob" type="date" value="<c:out value='${patient.date_of_birth}'/>"></div>
            <div class="field"><label for="gender">Gender</label><select id="gender" name="gender"><option value="">Prefer not to say</option><c:forEach var="g" items="${['Female','Male','Other']}"><option ${patient.gender == g ? 'selected' : ''}><c:out value="${g}"/></option></c:forEach></select></div>
            <div class="field"><label for="height">Height (cm)</label><input id="height" name="height" type="number" min="50" max="260" step="0.1" value="<c:out value='${patient.height_cm}'/>"></div>
            <div class="field"><label for="weight">Weight (kg)</label><input id="weight" name="weight" type="number" min="10" max="500" step="0.1" value="<c:out value='${patient.weight_kg}'/>"></div>
        </div>
        <div class="field"><label for="allergies">Allergies</label><input id="allergies" name="allergies" value="<c:out value='${patient.allergies}'/>" maxlength="500" placeholder="Peanut, shellfish, milk"><small>Separate allergy names with commas. Adding an allergy immediately re-checks your diet plans.</small></div>
        <div class="field"><label for="conditions">Medical conditions</label><textarea id="conditions" name="conditions" maxlength="800"><c:out value="${patient.conditions}"/></textarea></div>
        <div class="field"><label for="medications">Current medications</label><textarea id="medications" name="medications" maxlength="800"><c:out value="${patient.medications}"/></textarea></div>
        <div class="field"><label for="emergency">Emergency contact</label><input id="emergency" name="emergency" value="<c:out value='${patient.emergency_contact}'/>" maxlength="120"></div>
        <button class="button" type="submit">Save health profile</button>
    </form>
</section>
</c:when>
<c:otherwise>
<section class="page-heading"><div><p class="eyebrow">Staff account</p><h1>My account</h1><p class="lead">Your name, email, and role are managed by a NutriCare administrator.</p></div></section>
<section class="card"><div class="grid grid-3">
    <div><p class="muted">Full name</p><h3><c:out value="${account.full_name}"/></h3></div>
    <div><p class="muted">Work email</p><h3><c:out value="${account.email}"/></h3></div>
    <div><p class="muted">Role</p><span class="badge"><c:out value="${account.role}"/></span></div>
</div></section>
</c:otherwise>
</c:choose>
<section class="grid grid-2 section-gap">
    <div class="card"><h2>Change password</h2><form method="post" action="<c:url value='/profile/password'/>"><input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><div class="field"><label for="currentPassword">Current password</label><input id="currentPassword" name="currentPassword" type="password" required maxlength="100" autocomplete="current-password"></div><div class="field"><label for="newPassword">New password</label><input id="newPassword" name="newPassword" type="password" required minlength="8" maxlength="100" autocomplete="new-password"><small>At least 8 characters with a letter and a number.</small></div><button class="button" type="submit">Change password</button></form></div>
    <c:if test="${sessionScope.role == 'PATIENT'}">
    <div class="card card-muted"><h2>Deactivate account</h2><p class="muted">This immediately blocks sign-in while preserving clinical and payment history. An administrator can restore it later.</p><form method="post" action="<c:url value='/profile/deactivate'/>" data-confirm="Deactivate your NutriCare account?"><input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><button class="button button-danger" type="submit">Deactivate my account</button></form></div>
    </c:if>
</section>
<%@ include file="includes/footer.jsp" %>
