<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="page-heading"><div><p class="eyebrow">Care workspace</p><h1>Good day, <c:out value="${sessionScope.fullName}"/></h1><p class="lead">Your <c:out value="${sessionScope.role}"/> dashboard brings the work that needs attention into one place.</p></div><span class="badge badge-ACTIVE">SYSTEM ONLINE</span></section>
<div class="stat-grid">
    <c:forEach var="item" items="${counts}"><div class="stat"><strong><c:out value="${item.value}"/></strong><span><c:out value="${item.key}"/></span></div></c:forEach>
</div>

<c:if test="${not empty upcoming || not empty abnormalCheckups || not empty awaitingCheckup || not empty reviews}">
<section class="grid grid-2 section-gap">
    <c:if test="${not empty upcoming}">
    <div class="card"><h2>Reminders: next 24 hours</h2><ul class="alert-list">
        <c:forEach var="a" items="${upcoming}"><li><strong><fmt:formatDate value="${a.appointment_time}" pattern="EEE h:mm a"/></strong> · <c:if test="${sessionScope.role != 'PATIENT'}"><c:out value="${a.patient_name}"/> with </c:if><c:out value="${a.staff_name}"/> <span class="badge badge-${a.status}"><c:out value="${a.status}"/></span><br><span class="muted"><c:out value="${a.purpose}"/></span></li></c:forEach>
    </ul><a href="<c:url value='/appointments'/>">All appointments</a></div>
    </c:if>
    <c:if test="${not empty abnormalCheckups}">
    <div class="card"><h2>Abnormal readings (7 days)</h2><ul class="alert-list">
        <c:forEach var="c" items="${abnormalCheckups}"><li><span class="badge badge-ABNORMAL">REVIEW</span> <strong><c:out value="${c.patient_name}"/></strong> · <fmt:formatDate value="${c.checked_at}" pattern="dd MMM"/><br><span class="muted">BMI <c:out value="${c.bmi}"/> (<c:out value="${c.bmi_label}"/>)<c:if test="${not empty c.systolic || not empty c.diastolic}"> · BP <c:out value="${empty c.systolic ? '–' : c.systolic}"/>/<c:out value="${empty c.diastolic ? '–' : c.diastolic}"/></c:if><c:if test="${not empty c.blood_sugar}"> · Sugar <c:out value="${c.blood_sugar}"/></c:if></span></li></c:forEach>
    </ul><a href="<c:url value='/checkups'/>">All check-ups</a></div>
    </c:if>
    <c:if test="${not empty awaitingCheckup}">
    <div class="card"><h2>Completed visits awaiting a check-up</h2><ul class="alert-list">
        <c:forEach var="a" items="${awaitingCheckup}"><li><strong><c:out value="${a.patient_name}"/></strong> · <fmt:formatDate value="${a.appointment_time}" pattern="dd MMM, h:mm a"/> <a class="button button-secondary" href="<c:url value='/checkups'><c:param name='appointmentId' value='${a.id}'/></c:url>">Record check-up</a></li></c:forEach>
    </ul></div>
    </c:if>
    <c:if test="${not empty reviews}">
    <div class="card"><h2>Diet plans needing review</h2><ul class="alert-list">
        <c:forEach var="d" items="${reviews}"><li><span class="badge badge-ABNORMAL">REVIEW</span> <strong><c:out value="${d.title}"/></strong> · <c:out value="${d.patient_name}"/> <span class="badge badge-${d.status}"><c:out value="${d.status}"/></span></li></c:forEach>
    </ul><a href="<c:url value='/diets'/>">Open diet plans</a></div>
    </c:if>
</section>
</c:if>

<section class="grid grid-3 section-gap">
    <c:if test="${sessionScope.role == 'PATIENT'}">
        <a class="quick-link" href="<c:url value='/profile'/>"><h3>Health profile</h3><p>Maintain allergies, conditions, medications, and body measurements.</p></a>
        <a class="quick-link" href="<c:url value='/appointments'/>"><h3>Book care</h3><p>Request a doctor or nutritionist appointment without schedule conflicts.</p></a>
        <a class="quick-link" href="<c:url value='/diets'/>"><h3>My diet plan</h3><p>See the plan your nutritionist has approved for you.</p></a>
        <a class="quick-link" href="<c:url value='/checkups'/>"><h3>Check-up history</h3><p>Follow BMI, vital readings, and clinician notes over time.</p></a>
        <a class="quick-link" href="<c:url value='/payments'/>"><h3>Payments</h3><p>Submit a payment reference and follow finance verification.</p></a>
    </c:if>
    <c:if test="${sessionScope.role == 'DOCTOR'}">
        <a class="quick-link" href="<c:url value='/appointments'/>"><h3>Clinical schedule</h3><p>Review patient bookings and update appointment outcomes.</p></a>
        <a class="quick-link" href="<c:url value='/checkups'/>"><h3>Record check-up</h3><p>Capture vitals; BMI and abnormal reading flags are automatic.</p></a>
    </c:if>
    <c:if test="${sessionScope.role == 'NUTRITIONIST'}">
        <a class="quick-link" href="<c:url value='/appointments'/>"><h3>Nutrition schedule</h3><p>Manage consultation bookings and completion status.</p></a>
        <a class="quick-link" href="<c:url value='/diets'/>"><h3>Diet planning</h3><p>Create, approve, review, and archive allergy-checked plans.</p></a>
        <a class="quick-link" href="<c:url value='/checkups'/>"><h3>Patient measurements</h3><p>Check BMI and vitals for your patients before planning meals.</p></a>
    </c:if>
    <c:if test="${sessionScope.role == 'FINANCE'}">
        <a class="quick-link" href="<c:url value='/payments'/>"><h3>Payment queue</h3><p>Verify submissions, reject invalid references, and process refunds.</p></a>
    </c:if>
    <c:if test="${sessionScope.role == 'RECEPTIONIST'}">
        <a class="quick-link" href="<c:url value='/appointments'/>"><h3>Appointment desk</h3><p>Book for patients, confirm requests, reschedule, and cancel.</p></a>
    </c:if>
    <c:if test="${sessionScope.role == 'ADMIN'}">
        <a class="quick-link" href="<c:url value='/staff'/>"><h3>Staff &amp; patient access</h3><p>Create role-based accounts, control active access, and restore patients.</p></a>
        <a class="quick-link" href="<c:url value='/audit'/>"><h3>Audit trail</h3><p>Review important actions across all six modules.</p></a>
        <a class="quick-link" href="<c:url value='/appointments'/>"><h3>Operations</h3><p>See and manage the clinic-wide appointment queue.</p></a>
        <a class="quick-link" href="<c:url value='/diets'/>"><h3>Diet oversight</h3><p>Inspect plans and patient safety review flags.</p></a>
        <a class="quick-link" href="<c:url value='/payments'/>"><h3>Finance oversight</h3><p>Track unverified submissions and payment outcomes.</p></a>
    </c:if>
</section>
<%@ include file="includes/footer.jsp" %>
