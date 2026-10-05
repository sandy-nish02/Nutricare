<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="page-heading"><div><p class="eyebrow">Diet plan management</p><h1>Personal diet plans</h1><p class="lead">Plans move from draft to active to archived, and only one plan is active per patient. The server blocks meal text containing a recorded allergy when a plan is created and again when it is activated.</p></div></section>
<c:if test="${sessionScope.role == 'NUTRITIONIST' || sessionScope.role == 'ADMIN'}">
<section class="card"><h2>Create diet plan</h2><form method="post" action="<c:url value='/diets/create'/>">
    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
    <div class="field"><label for="patientId">Patient</label><select id="patientId" name="patientId" required><option value="">Select patient</option><c:forEach var="p" items="${patients}"><option value="${p.id}"><c:out value="${p.full_name}"/><c:if test="${not empty p.bmi}"> — BMI <fmt:formatNumber value="${p.bmi}" maxFractionDigits="1"/></c:if><c:if test="${not empty p.allergies}"> — Allergies: <c:out value="${p.allergies}"/></c:if></option></c:forEach></select></div>
    <div class="field-row">
    <div class="field"><label for="title">Plan title</label><input id="title" name="title" required maxlength="160"></div>
    <div class="field"><label for="calories">Daily calories</label><input id="calories" name="calories" type="number" min="500" max="10000" required></div>
    <div class="field"><label for="startDate">Start date</label><input id="startDate" name="startDate" type="date" required></div>
    <div class="field"><label for="endDate">End date</label><input id="endDate" name="endDate" type="date" required></div></div>
    <div class="field"><label for="meals">Meals, food items, and portions</label><textarea id="meals" name="meals" maxlength="3000" required placeholder="Breakfast: oats 60 g...&#10;Lunch: rice 150 g..."></textarea><small>Allergy checks happen again on the server; browser text can never bypass them.</small></div>
    <button class="button" type="submit">Create draft plan</button>
</form></section>
</c:if>
<section class="grid grid-3 section-gap">
<c:forEach var="d" items="${diets}"><article class="card"><div class="button-row"><span class="badge badge-${d.status}"><c:out value="${d.status}"/></span><c:if test="${d.review_needed}"><span class="badge badge-ABNORMAL">REVIEW NEEDED</span></c:if></div><h2 class="section-gap"><c:out value="${d.title}"/></h2><p class="muted"><c:out value="${d.patient_name}"/> · <c:out value="${d.daily_calories}"/> kcal/day<br><fmt:formatDate value="${d.start_date}" pattern="dd MMM yyyy"/> to <fmt:formatDate value="${d.end_date}" pattern="dd MMM yyyy"/><br>Prepared by <c:out value="${d.nutritionist_name}"/></p><div class="meal-text"><c:out value="${d.meals}"/></div>
<c:if test="${sessionScope.role == 'NUTRITIONIST' || sessionScope.role == 'ADMIN'}"><form class="button-row section-gap" method="post" action="<c:url value='/diets/status'/>"><input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><input type="hidden" name="id" value="${d.id}"><select name="status" aria-label="New diet plan status"><option value="${d.status}" selected><c:out value="${d.status}"/> (current)</option><option>DRAFT</option><option>ACTIVE</option><option>ARCHIVED</option></select><button class="button" type="submit">${d.review_needed && d.status == 'ACTIVE' ? 'Re-approve' : 'Update'}</button></form></c:if></article></c:forEach>
<c:if test="${empty diets}"><div class="card empty">${sessionScope.role == 'PATIENT' ? 'Your nutritionist has not approved a diet plan for you yet.' : 'No diet plans yet.'}</div></c:if>
</section>
<%@ include file="includes/footer.jsp" %>
