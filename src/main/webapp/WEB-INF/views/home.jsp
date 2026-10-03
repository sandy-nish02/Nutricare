<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="shellClass" value="home-shell" scope="request"/>
<%@ include file="includes/header.jsp" %>

<section class="hero">
    <div class="hero-copy">
        <span class="project-label">Diet planning &amp; health check-ups</span>
        <h1>Healthy choices,<br>better lives.</h1>
        <p class="lead">NutriCare brings patient registration, diet planning, health check-ups, appointments, billing, and staff administration together around one shared patient record, so every decision is made with the latest health data.</p>
        <div class="button-row hero-actions">
            <c:choose>
                <c:when test="${not empty sessionScope.userId}">
                    <a class="button button-large" href="<c:url value='/dashboard'/>">Open my dashboard</a>
                </c:when>
                <c:otherwise>
                    <a class="button button-large" href="<c:url value='/register'/>">Create a patient account</a>
                    <a class="button button-secondary button-large" href="<c:url value='/login'/>">Sign in</a>
                </c:otherwise>
            </c:choose>
        </div>
        <ul class="hero-points">
            <li>Diet plans checked against your allergies</li>
            <li>Conflict-free doctor and nutritionist booking</li>
            <li>Automatic BMI and health-risk flags</li>
        </ul>
    </div>
    <div class="hero-visual" aria-hidden="true">
        <div class="record-card">
            <div class="record-head"><span class="brand-mark">N</span><div><strong>Patient record</strong><small>One profile · six modules</small></div></div>
            <div class="record-stats">
                <div><span>BMI</span><strong>23.4</strong><em class="ok">Healthy</em></div>
                <div><span>Blood pressure</span><strong>118/76</strong><em class="ok">Normal</em></div>
            </div>
            <div class="record-row"><span class="badge badge-ABNORMAL">ALLERGY</span> Peanut · Shellfish</div>
            <div class="record-row"><span class="badge badge-ACTIVE">ACTIVE PLAN</span> Balanced 1,800 kcal</div>
            <div class="record-row"><span class="badge badge-CONFIRMED">CONFIRMED</span> Nutrition review · Tue 10:30 AM</div>
            <div class="record-row"><span class="badge badge-VERIFIED">VERIFIED</span> Payment LKR 3,500.00</div>
        </div>
        <div class="floating-note">Allergy check passed ✓</div>
    </div>
</section>

<section class="home-section">
    <p class="eyebrow">Why NutriCare</p>
    <h2 class="section-title">Diet and health care shouldn't live on paper and spreadsheets</h2>
    <div class="grid grid-4">
        <div class="problem"><strong>Data duplication</strong><p>Patients re-enter the same details at every visit and department.</p></div>
        <div class="problem"><strong>Clinical risk</strong><p>Diet plans made without the latest allergy or check-up data.</p></div>
        <div class="problem"><strong>Admin burden</strong><p>Phone bookings, manual billing, and paper records slow every visit.</p></div>
        <div class="problem"><strong>No visibility</strong><p>Management has no real-time view of patients, appointments, or revenue.</p></div>
    </div>
</section>

<section class="home-section">
    <p class="eyebrow">One connected platform</p>
    <h2 class="section-title">Six modules, one patient record</h2>
    <div class="grid grid-3">
        <article class="module-card"><span class="module-number">01</span><h3>User &amp; Patient Management</h3><p>OTP-verified registration, secure sign-in and password reset, and a medical profile with allergies, conditions, and medications.</p></article>
        <article class="module-card"><span class="module-number">02</span><h3>Diet Plan Management</h3><p>Personal calorie-targeted plans that are blocked if they contain a recorded allergy, approved by a nutritionist, one active plan at a time.</p></article>
        <article class="module-card"><span class="module-number">03</span><h3>Health Check-up Management</h3><p>Vitals recorded each visit, system-calculated BMI, abnormal-reading flags, and diet reviews on a 5% weight change.</p></article>
        <article class="module-card"><span class="module-number">04</span><h3>Appointment Management</h3><p>Book doctors or nutritionists with automatic double-booking prevention, rescheduling, cancellation reasons, and 24-hour reminders.</p></article>
        <article class="module-card"><span class="module-number">05</span><h3>Payment &amp; Billing Management</h3><p>Payment-slip upload, finance verification, duplicate-reference protection, printable receipts, and controlled refunds.</p></article>
        <article class="module-card"><span class="module-number">06</span><h3>User &amp; Staff Management</h3><p>Role-based access for every staff member, account control, and an audit trail of every critical action.</p></article>
    </div>
</section>

<section class="home-section">
    <p class="eyebrow">How it works</p>
    <h2 class="section-title">A typical patient journey</h2>
    <ol class="journey">
        <li><span>1</span><strong>Register</strong><p>Create an account, verify your phone, and complete your health profile.</p></li>
        <li><span>2</span><strong>Book</strong><p>Choose a doctor or nutritionist and a free time slot.</p></li>
        <li><span>3</span><strong>Check-up</strong><p>Your doctor records vitals; BMI and risk flags are calculated for you.</p></li>
        <li><span>4</span><strong>Diet plan</strong><p>Your nutritionist approves a plan that is safe for your allergies.</p></li>
        <li><span>5</span><strong>Pay &amp; track</strong><p>Upload a payment slip and follow verification, receipts, and reminders.</p></li>
    </ol>
</section>

<section class="home-section">
    <p class="eyebrow">Built for every role</p>
    <h2 class="section-title">Each person sees only what their role needs</h2>
    <div class="grid grid-3">
        <div class="role-card"><h3>Patient</h3><p>Own profile, diet plan, check-up history, appointments, and payments.</p></div>
        <div class="role-card"><h3>Doctor</h3><p>Clinical schedule, check-up recording, and abnormal-reading alerts.</p></div>
        <div class="role-card"><h3>Nutritionist</h3><p>Allergy-checked diet plans, approvals, and review flags.</p></div>
        <div class="role-card"><h3>Receptionist</h3><p>Bookings for walk-in patients, confirmations, and rescheduling.</p></div>
        <div class="role-card"><h3>Finance staff</h3><p>Payment verification, receipts, refunds, and monthly revenue.</p></div>
        <div class="role-card"><h3>Administrator</h3><p>Staff accounts, roles, patient access, and the system audit log.</p></div>
    </div>
</section>

<section class="home-section">
    <div class="safety-band">
        <div><p class="eyebrow">Safety first</p><h2>Rules the system enforces, not just reminds</h2><p class="muted">NutriCare supports, and never replaces, the clinical judgement of doctors and nutritionists.</p></div>
        <ul>
            <li><strong>Allergy blocking</strong> when a plan is created and again when it is approved</li>
            <li><strong>No double-booking</strong> of any staff member's or patient's time</li>
            <li><strong>Verified payments only</strong> before refunds or confirmations</li>
            <li><strong>Hashed passwords, expiring codes,</strong> and a full audit trail</li>
        </ul>
    </div>
</section>

<section class="home-section">
    <p class="eyebrow">The team</p>
    <h2 class="section-title">SE2030 · Group 2026-Y2-S1-KU-38</h2>
    <div class="team-grid">
        <div class="team-member"><strong>Hettiarachchi H. H. K. S.</strong><span>IT25103745</span><em>User &amp; Patient Management</em></div>
        <div class="team-member"><strong>Samarakoon P. T. A. N.</strong><span>IT25103748</span><em>Diet Plan Management</em></div>
        <div class="team-member"><strong>Siriwardhana R. D. T. D.</strong><span>IT25103749</span><em>Health Check-up Management</em></div>
        <div class="team-member"><strong>Driberg B. D.</strong><span>IT25103744</span><em>Appointment Management</em></div>
        <div class="team-member"><strong>Nawarathne S. N.</strong><span>IT25103747 · Team Leader</span><em>Payment &amp; Billing Management</em></div>
        <div class="team-member"><strong>Perera K. V. R.</strong><span>IT25103746</span><em>User &amp; Staff Management (Admin)</em></div>
    </div>
</section>

<section class="cta-band">
    <div><h2>Start with your health profile</h2><p>Registration takes a minute. Your nutritionist and doctor will take it from there.</p></div>
    <div class="button-row">
        <c:choose>
            <c:when test="${not empty sessionScope.userId}"><a class="button button-light button-large" href="<c:url value='/dashboard'/>">Open my dashboard</a></c:when>
            <c:otherwise>
                <a class="button button-light button-large" href="<c:url value='/register'/>">Create a patient account</a>
                <a class="button button-outline-light button-large" href="<c:url value='/login'/>">Staff sign in</a>
            </c:otherwise>
        </c:choose>
    </div>
</section>
<%@ include file="includes/footer.jsp" %>
