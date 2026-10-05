<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="page-heading"><div><p class="eyebrow">Health check-up management</p><h1>Clinical measurements</h1><p class="lead">BMI and abnormal-reading flags are calculated by the system and cannot be typed in. A 5% weight change flags the patient's active diet plan for review.</p></div></section>
<c:if test="${sessionScope.role == 'DOCTOR' || sessionScope.role == 'ADMIN'}">
<section class="card"><h2>Record a check-up</h2><form method="post" action="<c:url value='/checkups/create'/>">
    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
    <c:choose>
        <c:when test="${not empty linkedAppointment}">
            <input type="hidden" name="appointmentId" value="${linkedAppointment.id}">
            <div class="notice notice-success">Linked to appointment #<c:out value="${linkedAppointment.id}"/> — <strong><c:out value="${linkedAppointment.patient_name}"/></strong>, <fmt:formatDate value="${linkedAppointment.appointment_time}" pattern="dd MMM yyyy, h:mm a"/> (<c:out value="${linkedAppointment.purpose}"/>). <a href="<c:url value='/checkups'/>">Record without an appointment</a></div>
        </c:when>
        <c:otherwise>
            <div class="field"><label for="patientId">Patient</label><select id="patientId" name="patientId" required><option value="">Select patient</option><c:forEach var="p" items="${patients}"><option value="${p.id}"><c:out value="${p.full_name}"/></option></c:forEach></select><small>For a completed appointment, use “Record check-up” on the Appointments page so the visit is linked.</small></div>
        </c:otherwise>
    </c:choose>
    <div class="field-row">
    <div class="field"><label for="height">Height (cm)</label><input id="height" name="height" type="number" min="50" max="260" step="0.1" required></div>
    <div class="field"><label for="weight">Weight (kg)</label><input id="weight" name="weight" type="number" min="10" max="500" step="0.1" required></div>
    <div class="field"><label for="systolic">Systolic pressure (mmHg)</label><input id="systolic" name="systolic" type="number" min="50" max="260"></div>
    <div class="field"><label for="diastolic">Diastolic pressure (mmHg)</label><input id="diastolic" name="diastolic" type="number" min="30" max="180"></div>
    <div class="field"><label for="bloodSugar">Blood sugar (mg/dL)</label><input id="bloodSugar" name="bloodSugar" type="number" min="20" max="600" step="0.1"></div></div>
    <div class="field"><label for="notes">Clinical notes</label><textarea id="notes" name="notes" maxlength="1200"></textarea></div>
    <button class="button" type="submit">Save check-up</button>
</form></section>
</c:if>
<section class="section-gap table-wrap"><table><thead><tr><th>Date</th><th>Patient</th><th>Measurements</th><th>Vitals</th><th>Assessment</th><th>Doctor notes</th></tr></thead><tbody>
<c:forEach var="c" items="${checkups}"><tr>
    <td><fmt:formatDate value="${c.checked_at}" pattern="dd MMM yyyy, h:mm a"/><br><span class="muted"><c:out value="${c.doctor_name}"/></span><c:if test="${not empty c.appointment_id}"><br><span class="muted">Appointment #<c:out value="${c.appointment_id}"/></span></c:if></td>
    <td><c:out value="${c.patient_name}"/></td>
    <td><strong><c:out value="${c.weight_kg}"/> kg</strong><br><c:out value="${c.height_cm}"/> cm<br>BMI <c:out value="${c.bmi}"/> <span class="muted">(<c:out value="${c.bmi_label}"/>)</span></td>
    <td>BP: <c:choose><c:when test="${not empty c.systolic || not empty c.diastolic}"><c:out value="${empty c.systolic ? '–' : c.systolic}"/>/<c:out value="${empty c.diastolic ? '–' : c.diastolic}"/></c:when><c:otherwise>not taken</c:otherwise></c:choose><br>Sugar: <c:out value="${empty c.blood_sugar ? 'not taken' : c.blood_sugar}"/></td>
    <td><c:choose><c:when test="${c.abnormal}"><span class="badge badge-ABNORMAL">REVIEW</span></c:when><c:otherwise><span class="badge badge-ACTIVE">NORMAL RANGE</span></c:otherwise></c:choose></td>
    <td><c:out value="${c.notes}"/></td>
</tr></c:forEach>
<c:if test="${empty checkups}"><tr><td class="empty" colspan="6">No check-ups have been recorded.</td></tr></c:if>
</tbody></table></section>
<%@ include file="includes/footer.jsp" %>
