<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="page-heading">
    <div>
        <p class="eyebrow">User & staff management</p>
        <h1>Staff administration</h1>
        <p class="lead">Create the right role for each staff member and deactivate access without deleting historical records.</p>
    </div>
</section>
<div class="grid grid-2">
    <section class="card">
        <h2>Create staff account</h2>
        <form method="post" action="<c:url value='/staff/create'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
            <div class="field">
                <label for="fullName">Full name</label>
                <input id="fullName" name="fullName" required maxlength="120">
            </div>
            <div class="field-row">
                <div class="field">
                    <label for="email">Work email</label>
                    <input id="email" name="email" type="email" required maxlength="160">
                </div>
                <div class="field">
                    <label for="phone">Phone</label>
                    <input id="phone" name="phone" type="tel" maxlength="30">
                </div>
            </div>
            <div class="field">
                <label for="role">Access role</label>
                <select id="role" name="role" required>
                    <option value="">Select role</option>
                    <option>ADMIN</option>
                    <option>DOCTOR</option>
                    <option>NUTRITIONIST</option>
                    <option>RECEPTIONIST</option>
                    <option>FINANCE</option>
                </select>
            </div>
            <div class="field">
                <label for="password">Temporary password</label>
                <input id="password" name="password" type="password" minlength="8" maxlength="100" required>
                <small>At least 8 characters with a letter and a number.</small>
            </div>
            <button class="button" type="submit">Create account</button>
        </form>
    </section>
    <section class="card card-muted">
        <h2>Role boundaries</h2>
        <p class="lead">Doctors record check-ups, nutritionists manage diet plans, reception handles appointments, finance verifies payments, and admins control staff and audit data.</p>
        <p class="danger-text">Deactivation blocks login immediately, even for someone already signed in, but keeps linked clinical and finance history intact.</p>
    </section>
</div>
<section class="section-gap table-wrap">
    <table>
        <thead>
            <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Role</th>
                <th>Created</th>
                <th>Access</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="s" items="${staff}">
                <tr>
                    <td>
                        <strong><c:out value="${s.full_name}"/></strong>
                    </td>
                    <td>
                        <c:out value="${s.email}"/>
                    </td>
                    <td>
                        <c:out value="${s.phone}"/>
                    </td>
                    <td>
                        <span class="badge"><c:out value="${s.role}"/></span>
                        <form class="section-gap" method="post" action="<c:url value='/staff/access'/>">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
                            <input type="hidden" name="id" value="${s.id}">
                                <select name="role" aria-label="Staff role">
                                    <option value="${s.role}" selected><c:out value="${s.role}"/> (current)</option>
                                    <option>ADMIN</option>
                                    <option>DOCTOR</option>
                                    <option>NUTRITIONIST</option>
                                    <option>RECEPTIONIST</option>
                                    <option>FINANCE</option>
                                </select>
                            <input name="password" type="password" minlength="8" maxlength="100" placeholder="New password (optional)" aria-label="Optional new password">
                            <button class="button" type="submit">Update access</button>
                        </form></td><td><fmt:formatDate value="${s.created_at}" pattern="dd MMM yyyy"/>
                    </td>
                    <td>
                        <span class="badge badge-${s.active ? 'ACTIVE' : 'ARCHIVED'}">${s.active ? 'ACTIVE' : 'DEACTIVATED'}</span>
                        <form class="section-gap" method="post" action="<c:url value='/staff/status'/>" data-confirm="Change access for this account?">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
                            <input type="hidden" name="id" value="${s.id}">
                            <input type="hidden" name="active" value="${!s.active}">
                            <button class="button ${s.active ? 'button-danger' : 'button-secondary'}" type="submit">${s.active ? 'Deactivate' : 'Reactivate'}</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</section>
<section class="section-gap">
    <h2>Patient accounts</h2>
    <p class="muted">Patients who deactivated their own account can be restored here. Accounts that never completed phone verification must finish it themselves by signing in.</p>
</section>
<section class="table-wrap">
    <table>
        <thead>
            <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Registered</th>
                <th>Status</th>
                <th>Access</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="p" items="${patientAccounts}">
                <tr>
                    <td>
                        <strong>
                            <c:out value="${p.full_name}"/>
                        </strong>
                    </td>
                    <td>
                        <c:out value="${p.email}"/>
                    </td>
                    <td>
                        <c:out value="${p.phone}"/>
                    </td>
                    <td>
                        <fmt:formatDate value="${p.created_at}" pattern="dd MMM yyyy"/>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${!p.verified}">
                                <span class="badge badge-PENDING">AWAITING VERIFICATION</span>
                            </c:when>
                            <c:when test="${p.active}">
                                <span class="badge badge-ACTIVE">ACTIVE</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge-ARCHIVED">DEACTIVATED</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:if test="${p.verified}">
                            <form method="post" action="<c:url value='/staff/patient-status'/>" data-confirm="${p.active ? 'Suspend this patient account?' : 'Restore this patient account?'}">
                                <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrf}'/>">
                                <input type="hidden" name="id" value="${p.id}">
                                <input type="hidden" name="active" value="${!p.active}">
                                <button class="button ${p.active ? 'button-danger' : 'button-secondary'}" type="submit">${p.active ? 'Suspend' : 'Restore'}</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty patientAccounts}">
                <tr>
                    <td class="empty" colspan="6">No patient accounts yet.</td>
                </tr>
            </c:if>
        </tbody>
    </table>
</section>
<%@ include file="includes/footer.jsp" %>
