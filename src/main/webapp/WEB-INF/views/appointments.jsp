<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="page-heading">
    <div>
        <p class="eyebrow">Appointment management</p>
        <h1>Appointments</h1>
        <p class="lead">Book, confirm, complete, or cancel visits. Exact staff/time clashes are prevented by the server.</p>
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
                            <option value="${p.id}">
                                <c:out value="${p.full_name}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
            </c:if>
            <div class="field">
                <label for="staffId">Doctor or nutritionist</label>
                <select id="staffId" name="staffId" required>
                    <option value="">Select staff member</option>
                    <c:forEach var="s" items="${staffOptions}">
                        <option value="${s.id}">
                            <c:out value="${s.full_name}"/> — <c:out value="${s.role}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="field">
            <label for="appointmentTime">Date and time</label><input id="appointmentTime" name="appointmentTime" type="datetime-local" required></div>
            <div class="field"><label for="purpose">Purpose</label><textarea id="purpose" name="purpose" required maxlength="500" placeholder="Brief reason for this visit"></textarea></div>
            <button class="button" type="submit">Request appointment</button>
        </form>
    </section>
    </c:if>
    <section class="card ${sessionScope.role != 'PATIENT' && sessionScope.role != 'RECEPTIONIST' && sessionScope.role != 'ADMIN' ? '' : 'card-muted'}"><h2>Schedule</h2><p class="muted">Newest appointments appear first. Staff members only see their assigned schedule.</p></section>
</div>
<section class="section-gap table-wrap">
<table><thead><tr><th>When</th><th>Patient</th><th>Care provider</th><th>Purpose</th><th>Status</th><th>Action</th></tr></thead><tbody>
<c:forEach var="a" items="${appointments}"><tr><td><c:out value="${a.appointment_time}"/><c:if test="${a.reminder}"><br><span class="badge badge-PENDING">DUE WITHIN 24H</span></c:if></td><td><c:out value="${a.patient_name}"/></td><td><c:out value="${a.staff_name}"/><br><span class="muted"><c:out value="${a.staff_role}"/></span></td><td><c:out value="${a.purpose}"/></td><td><span class="badge badge-${a.status}"><c:out value="${a.status}"/></span><c:if test="${not empty a.change_reason}"><br><span class="muted">Reason: <c:out value="${a.change_reason}"/></span></c:if></td><td class="table-actions">
    <c:if test="${sessionScope.role == 'PATIENT' && (a.status == 'PENDING' || a.status == 'CONFIRMED')}"><form method="post" action="<c:url value='/appointments/status'/>" data-confirm="Cancel this appointment?"><input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><input type="hidden" name="id" value="${a.id}"><input type="hidden" name="status" value="CANCELLED"><input name="reason" maxlength="500" placeholder="Cancellation reason" aria-label="Cancellation reason" required><button class="button button-danger" type="submit">Cancel</button></form></c:if>
    <c:if test="${sessionScope.role != 'PATIENT'}"><form method="post" action="<c:url value='/appointments/status'/>"><input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><input type="hidden" name="id" value="${a.id}"><select name="status" aria-label="New appointment status"><option value="${a.status}" selected><c:out value="${a.status}"/> (current)</option><option>PENDING</option><option>CONFIRMED</option><option>COMPLETED</option><option>CANCELLED</option><option>NO_SHOW</option></select><input name="reason" maxlength="500" placeholder="Reason for cancel/no-show" aria-label="Status change reason"><button class="button" type="submit">Update</button></form><form class="section-gap" method="post" action="<c:url value='/appointments/reschedule'/>"><input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><input type="hidden" name="id" value="${a.id}"><input name="appointmentTime" type="datetime-local" aria-label="New appointment time" required><input name="reason" maxlength="500" placeholder="Reschedule reason" aria-label="Reschedule reason" required><button class="button button-secondary" type="submit">Move</button></form></c:if>
</td></tr></c:forEach>
<c:if test="${empty appointments}"><tr><td class="empty" colspan="6">No appointments yet.</td></tr></c:if>
</tbody></table></section>
<%@ include file="includes/footer.jsp" %>
