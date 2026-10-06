<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="includes/header.jsp" %>
<section class="page-heading">
    <div>
        <p class="eyebrow">Governance</p>
        <h1>Audit trail</h1>
        <p class="lead">The latest 150 important actions across patient, clinical, diet, appointment, finance, and staff modules.</p>
    </div>
</section>
<section class="table-wrap">
    <table>
        <thead>
            <tr>
                <th>Time</th>
                <th>User</th>
                <th>Action</th>
                <th>Details</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="log" items="${logs}">
                <tr>
                    <td>
                        <fmt:formatDate value="${log.created_at}" pattern="dd MMM yyyy, h:mm:ss a"/>
                    </td>
                    <td>
                        <c:out value="${empty log.full_name ? 'Database (outside NutriCare)' : log.full_name}"/>
                    </td>
                    <td>
                        <span class="badge"><c:out value="${log.action}"/></span>
                    </td>
                    <td>
                        <c:out value="${log.details}"/>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty logs}">
                <tr>
                    <td class="empty" colspan="4">No audit entries yet.</td>
                </tr>
            </c:if>
        </tbody>
    </table>
</section>
<%@ include file="includes/footer.jsp" %>
