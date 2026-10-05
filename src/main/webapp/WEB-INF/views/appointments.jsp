<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="page-heading">
    <div>
        <p class="eyebrow">Appointment management</p>
        <h1>Appointments</h1>
        <p class="lead">Book, confirm, complete, or cancel visits. Each visit reserves a <c:out value="${slotMinutes}"/>-minute slot, and the server blocks overlapping bookings for the same care provider or patient.</p>
    </div>
</section>
<div class="grid grid-2">
    <c:if test="${sessionScope.role == 'PATIENT' || sessionScope.role == 'RECEPTIONIST' || sessionScope.role == 'ADMIN'}">
    <section class="card">
        <h2>Book appointment</h2>
        <form method="post" action="<c:url value='/appointments/create'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
            <c:if test="${sessionScope.role != 'PATIENT'}">
                <div class="field">
                    <label for="patientId">Patient</label>
                    <select id="patientId" name="patientId" required>
                        <option value="">Select patient</option>
                        <c:forEach var="p" items="${patients}">
                            <option value="${p.id}"><c:out value="${p.full_name}"/></option>
                        </c:forEach>
                    </select>
                </div>
            </c:if>
            <div class="field">
                <label for="staffId">Doctor or nutritionist</label>
                <select id="staffId" name="staffId" required>
                    <option value="">Select staff member</option>
                    <c:forEach var="s" items="${staffOptions}">
                        <option value="${s.id}"><c:out value="${s.full_name}"/> — <c:out value="${s.role}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="field"><label for="appointmentTime">Date and time</label><input id="appointmentTime" name="appointmentTime" type="datetime-local" data-future required><small>At least 30 minutes from now.</small></div>
            <div class="field"><label for="purpose">Purpose</label><textarea id="purpose" name="purpose" required maxlength="500" placeholder="Brief reason for this visit"></textarea></div>
            <button class="button" type="submit">Request appointment</button>
        </form>
    </section>
    </c:if>
    <section class="card card-muted"><h2>How it works</h2><p class="muted">New requests start as PENDING. Staff confirm them (or Finance verification of a linked payment confirms them automatically), then mark them COMPLETED or NO_SHOW. Cancelled, completed, and no-show visits are kept as history and cannot be reopened.</p><p class="muted">Staff members only see their assigned schedule. Visits within the next 24 hours are marked as due.</p></section>
</div>
<section class="section-gap table-wrap">
<table><thead><tr><th>#</th><th>When</th><th>Patient</th><th>Care provider</th><th>Purpose</th><th>Status</th><th>Action</th></tr></thead><tbody>
<c:forEach var="a" items="${appointments}"><tr>
    <td><strong>#<c:out value="${a.id}"/></strong></td>
    <td><fmt:formatDate value="${a.appointment_time}" pattern="dd MMM yyyy, h:mm a"/><c:if test="${a.reminder}"><br><span class="badge badge-PENDING">DUE WITHIN 24H</span></c:if></td>
    <td><c:out value="${a.patient_name}"/></td>
    <td><c:out value="${a.staff_name}"/><br><span class="muted"><c:out value="${a.staff_role}"/></span></td>
    <td><c:out value="${a.purpose}"/></td>
    <td><span class="badge badge-${a.status}"><c:out value="${a.status}"/></span><c:if test="${not empty a.change_reason}"><br><span class="muted">Reason: <c:out value="${a.change_reason}"/></span></c:if></td>
    <td class="table-actions">
    <c:if test="${sessionScope.role == 'PATIENT' && a.reschedulable}">
        <form method="post" action="<c:url value='/appointments/status'/>" data-confirm="Cancel this appointment?">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><input type="hidden" name="id" value="${a.id}"><input type="hidden" name="status" value="CANCELLED">
            <input name="reason" maxlength="500" placeholder="Cancellation reason" aria-label="Cancellation reason" required>
            <button class="button button-danger" type="submit">Cancel</button>
        </form>
    </c:if>
    <c:if test="${sessionScope.role != 'PATIENT' && not empty a.next_statuses}">
        <form method="post" action="<c:url value='/appointments/status'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><input type="hidden" name="id" value="${a.id}">
            <select name="status" aria-label="New appointment status" required><option value="">Change status…</option><c:forEach var="next" items="${a.next_statuses}"><option><c:out value="${next}"/></option></c:forEach></select>
            <input name="reason" maxlength="500" placeholder="Reason (required for cancel/no-show)" aria-label="Status change reason">
            <button class="button" type="submit">Update</button>
        </form>
    </c:if>
    <c:if test="${sessionScope.role != 'PATIENT' && a.reschedulable}">
        <form class="section-gap" method="post" action="<c:url value='/appointments/reschedule'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><input type="hidden" name="id" value="${a.id}">
            <input name="appointmentTime" type="datetime-local" data-future aria-label="New appointment time" required>
            <input name="reason" maxlength="500" placeholder="Reschedule reason" aria-label="Reschedule reason" required>
            <button class="button button-secondary" type="submit">Move</button>
        </form>
    </c:if>
    <c:if test="${a.status == 'COMPLETED' && a.staff_role == 'DOCTOR'}">
        <c:choose>
            <c:when test="${not empty a.checkup_id}"><span class="muted">Check-up recorded</span></c:when>
            <c:when test="${sessionScope.role == 'DOCTOR' || sessionScope.role == 'ADMIN'}"><a class="button button-secondary" href="<c:url value='/checkups'><c:param name='appointmentId' value='${a.id}'/></c:url>">Record check-up</a></c:when>
        </c:choose>
    </c:if>
    </td>
</tr></c:forEach>
<c:if test="${empty appointments}"><tr><td class="empty" colspan="7">No appointments yet.</td></tr></c:if>
</tbody></table></section>
<%@ include file="includes/footer.jsp" %>
