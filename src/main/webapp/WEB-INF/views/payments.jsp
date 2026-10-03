<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="page-heading"><div><p class="eyebrow">Payment & billing management</p><h1>Payment verification</h1><p class="lead">Record a bank transfer, cash, or card reference. Duplicate transaction references are rejected by both application and database rules. Verifying a payment confirms its linked appointment; only verified payments for a cancelled appointment can be refunded.</p></div></section>
<section class="card"><h2>Submit payment</h2><form method="post" enctype="multipart/form-data" action="<c:url value='/payments/create'/>">
    <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
    <div class="field-row"><c:if test="${sessionScope.role != 'PATIENT'}"><div class="field"><label for="patientId">Patient</label><select id="patientId" name="patientId" required><option value="">Select patient</option><c:forEach var="p" items="${patients}"><option value="${p.id}"><c:out value="${p.full_name}"/></option></c:forEach></select></div></c:if>
    <div class="field"><label for="appointmentId">For appointment (optional)</label><select id="appointmentId" name="appointmentId"><option value="">Not linked to an appointment</option><c:forEach var="a" items="${appointmentOptions}"><option value="${a.id}">#<c:out value="${a.id}"/> · <fmt:formatDate value="${a.appointment_time}" pattern="dd MMM yyyy, h:mm a"/> · <c:if test="${sessionScope.role != 'PATIENT'}"><c:out value="${a.patient_name}"/> with </c:if><c:out value="${a.staff_name}"/> (<c:out value="${a.status}"/>)</option></c:forEach></select></div>
    <div class="field"><label for="amount">Amount (LKR)</label><input id="amount" name="amount" type="number" min="0.01" max="1000000" step="0.01" required></div>
    <div class="field"><label for="reference">Transaction reference</label><input id="reference" name="reference" required maxlength="100"></div>
    <div class="field"><label for="method">Payment method</label><select id="method" name="method" required><option>BANK_TRANSFER</option><option>CARD</option><option>CASH</option></select></div>
    <div class="field"><label for="slip">Payment slip (PDF, JPG, or PNG)</label><input id="slip" name="slip" type="file" accept="application/pdf,image/jpeg,image/png" data-max-bytes="2097152"><small>Required for bank transfers; maximum 2 MB.</small></div></div>
    <button class="button" type="submit">Submit for verification</button>
</form></section>
<section class="section-gap table-wrap"><table><thead><tr><th>Submitted</th><th>Patient</th><th>Reference</th><th>Amount</th><th>Method</th><th>Status</th><th>Finance action</th></tr></thead><tbody>
<c:forEach var="p" items="${payments}"><tr>
    <td><fmt:formatDate value="${p.submitted_at}" pattern="dd MMM yyyy, h:mm a"/></td>
    <td><c:out value="${p.patient_name}"/></td>
    <td><strong><c:out value="${p.transaction_ref}"/></strong><c:if test="${not empty p.appointment_id}"><br><span class="muted">Appointment #<c:out value="${p.appointment_id}"/><c:if test="${not empty p.appointment_time}">, <fmt:formatDate value="${p.appointment_time}" pattern="dd MMM yyyy"/></c:if></span></c:if><div class="button-row section-gap"><a href="<c:url value='/payments/receipt'><c:param name='id' value='${p.id}'/></c:url>">${p.status == 'VERIFIED' ? 'Receipt' : 'Invoice'}</a><c:if test="${not empty p.payment_slip_name}"><a href="<c:url value='/payments/slip'><c:param name='id' value='${p.id}'/></c:url>" target="_blank" rel="noopener">Slip</a></c:if></div></td>
    <td>LKR <fmt:formatNumber value="${p.amount}" minFractionDigits="2" maxFractionDigits="2"/></td>
    <td><c:out value="${p.method}"/></td>
    <td><span class="badge badge-${p.status}"><c:out value="${p.status}"/></span><c:if test="${not empty p.verifier_name}"><br><span class="muted">by <c:out value="${p.verifier_name}"/></span></c:if></td>
    <td class="table-actions"><c:if test="${(sessionScope.role == 'FINANCE' || sessionScope.role == 'ADMIN') && not empty p.next_statuses}"><form method="post" action="<c:url value='/payments/status'/>"><input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>"><input type="hidden" name="id" value="${p.id}"><select name="status" aria-label="New payment status" required><c:forEach var="next" items="${p.next_statuses}"><option><c:out value="${next}"/></option></c:forEach></select><button class="button" type="submit">Update</button></form></c:if></td>
</tr></c:forEach>
<c:if test="${empty payments}"><tr><td class="empty" colspan="7">No payment records yet.</td></tr></c:if>
</tbody></table></section>
<%@ include file="includes/footer.jsp" %>
