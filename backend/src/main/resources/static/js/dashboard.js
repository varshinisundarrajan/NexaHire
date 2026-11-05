/**
 * NexaHire - Role-Adaptive Dashboard Logic
 */

document.addEventListener("DOMContentLoaded", async () => {
  renderAppNavigation("dashboard");
  const user = getUser();
  if (!user) return;

  const role = user.role;
  const container = document.getElementById("dashboard-content");

  try {
    if (role === "ROLE_CANDIDATE") {
      await renderCandidateDashboard(container, user);
    } else if (role === "ROLE_ADMIN") {
      await renderAdminDashboard(container, user);
    } else if (role === "ROLE_HR_MANAGER") {
      await renderHRDashboard(container, user);
    } else {
      // Recruiter default
      await renderRecruiterDashboard(container, user);
    }
  } catch (err) {
    console.error("Dashboard render error:", err);
    container.innerHTML = `
      <div class="card text-center p-6">
        <h4 class="text-danger mb-2">Error Loading Dashboard</h4>
        <p class="text-sm text-muted">${escapeHtml(err.message)}</p>
        <button class="btn btn-primary btn-sm mt-4" onclick="location.reload()">Retry</button>
      </div>
    `;
  }
});

// 1. Candidate Dashboard
async function renderCandidateDashboard(container, user) {
  const [appsRes, interviewsRes, jobsRes, profileRes] = await Promise.all([
    apiCall("/applications/my"),
    apiCall("/interviews/my"),
    apiCall("/jobs?size=4&status=ACTIVE"),
    apiCall("/candidates/profile/me")
  ]);

  const apps = appsRes.data || [];
  const interviews = interviewsRes.data || [];
  const jobs = (jobsRes.data && jobsRes.data.content) ? jobsRes.data.content : [];
  const profile = profileRes.data || {};

  const totalApplied = apps.length;
  const inReview = apps.filter(a => ["APPLIED", "SCREENING", "SHORTLISTED"].includes(a.status)).length;
  const activeInterviews = interviews.filter(i => i.status === "SCHEDULED").length;
  const selectedCount = apps.filter(a => a.status === "SELECTED").length;

  container.innerHTML = `
    <!-- Candidate Overview Stats -->
    <div class="grid grid-4 gap-4 mb-6">
      <div class="stat-card">
        <div>
          <div class="stat-label">Active Applications</div>
          <div class="stat-val">${totalApplied}</div>
        </div>
        <div class="stat-icon">📄</div>
      </div>
      <div class="stat-card stat-warning">
        <div>
          <div class="stat-label">In Review / Screening</div>
          <div class="stat-val">${inReview}</div>
        </div>
        <div class="stat-icon">⏳</div>
      </div>
      <div class="stat-card stat-success">
        <div>
          <div class="stat-label">Upcoming Interviews</div>
          <div class="stat-val">${activeInterviews}</div>
        </div>
        <div class="stat-icon">📅</div>
      </div>
      <div class="stat-card stat-navy">
        <div>
          <div class="stat-label">Offers / Selected</div>
          <div class="stat-val">${selectedCount}</div>
        </div>
        <div class="stat-icon">🎉</div>
      </div>
    </div>

    <!-- Candidate Main Grid -->
    <div class="grid grid-3 gap-6">
      <!-- Applications Column (2 span) -->
      <div style="grid-column: span 2;" class="flex flex-col gap-6">
        <div class="card">
          <div class="card-header">
            <div>
              <h3>Recent Application History</h3>
              <p class="text-xs text-muted">Track your live pipeline progress and recruiter updates</p>
            </div>
            <a href="applications.html" class="btn btn-outline btn-sm">View All Applications &rarr;</a>
          </div>

          ${apps.length === 0 ? `
            <div class="text-center p-6 text-muted">
              <p>You haven't submitted any job applications yet.</p>
              <a href="jobs.html" class="btn btn-primary btn-sm mt-3">Browse Job Openings</a>
            </div>
          ` : `
            <div class="table-responsive">
              <table class="table">
                <thead>
                  <tr>
                    <th>Job Title</th>
                    <th>Department</th>
                    <th>Status</th>
                    <th>Skill Match</th>
                    <th>Applied Date</th>
                  </tr>
                </thead>
                <tbody>
                  ${apps.slice(0, 5).map(app => `
                    <tr>
                      <td class="text-bold">${escapeHtml(app.job.title)}</td>
                      <td>${escapeHtml(app.job.department)}</td>
                      <td>${getStatusBadge(app.status)}</td>
                      <td>${getMatchScoreBadge(app.matchScore)}</td>
                      <td class="text-sm text-muted">${formatDate(app.appliedAt)}</td>
                    </tr>
                  `).join("")}
                </tbody>
              </table>
            </div>
          `}
        </div>

        <!-- Recommended Jobs for Candidate -->
        <div class="card">
          <div class="card-header">
            <div>
              <h3>Recommended Positions For You</h3>
              <p class="text-xs text-muted">Curated based on your recorded skills</p>
            </div>
            <a href="jobs.html" class="btn btn-outline btn-sm">Explore All Jobs &rarr;</a>
          </div>

          <div class="grid grid-2 gap-4">
            ${jobs.slice(0, 4).map(job => `
              <div class="card" style="padding:1rem;background:var(--slate-50);">
                <div class="flex justify-between items-center mb-1">
                  <span class="badge badge-primary">${escapeHtml(job.jobType)}</span>
                  <span class="text-xs text-muted">${escapeHtml(job.location)}</span>
                </div>
                <h4 style="font-size:1rem;" class="mb-1">${escapeHtml(job.title)}</h4>
                <div class="text-xs text-muted mb-2">${escapeHtml(job.department)} &bull; ${formatCurrency(job.salaryMin)} - ${formatCurrency(job.salaryMax)}</div>
                <div class="mb-3">
                  ${(job.requiredSkills || "").split(",").slice(0, 3).map(s => `<span class="skill-tag">${escapeHtml(s.trim())}</span>`).join("")}
                </div>
                <a href="jobs.html" class="btn btn-primary btn-sm w-full">View &amp; Apply</a>
              </div>
            `).join("")}
          </div>
        </div>
      </div>

      <!-- Right Column: Profile & Scheduled Interviews -->
      <div class="flex flex-col gap-6">
        <div class="card">
          <div class="card-header">
            <h4>Profile Summary</h4>
            <a href="profile.html" class="text-xs text-bold">Edit &rarr;</a>
          </div>
          <div class="flex items-center gap-3 mb-4">
            <div class="avatar" style="width:48px;height:48px;font-size:1.1rem;">${escapeHtml(user.fullName[0] || "U")}</div>
            <div>
              <div class="text-bold">${escapeHtml(user.fullName)}</div>
              <div class="text-xs text-muted">${escapeHtml(profile.headline || "Job Seeker")}</div>
            </div>
          </div>
          <div class="text-sm mb-3">
            <div class="text-muted text-xs mb-1">SKILLS RECORDED:</div>
            <div>${(profile.skills || "No skills added yet").split(",").map(s => `<span class="skill-tag">${escapeHtml(s.trim())}</span>`).join("")}</div>
          </div>
          <a href="profile.html" class="btn btn-secondary btn-sm w-full">Update Resume &amp; Skills</a>
        </div>

        <div class="card">
          <div class="card-header">
            <h4>Upcoming Interviews</h4>
            <a href="interviews.html" class="text-xs text-bold">Calendar &rarr;</a>
          </div>
          ${interviews.filter(i => i.status === "SCHEDULED").length === 0 ? `
            <p class="text-sm text-muted text-center py-4">No pending interviews scheduled.</p>
          ` : `
            <div class="flex flex-col gap-3">
              ${interviews.filter(i => i.status === "SCHEDULED").slice(0, 3).map(iv => `
                <div style="border-left:3px solid var(--primary);padding-left:0.75rem;">
                  <div class="text-bold text-sm">${escapeHtml(iv.application.job.title)}</div>
                  <div class="text-xs text-muted">📅 ${formatDate(iv.interviewDate)} at ${iv.interviewTime}</div>
                  <a href="${escapeHtml(iv.meetingLink || '#')}" target="_blank" class="btn btn-primary btn-sm mt-2" style="padding:0.25rem 0.6rem;font-size:0.75rem;">Join Call</a>
                </div>
              `).join("")}
            </div>
          `}
        </div>
      </div>
    </div>
  `;
}

// 2. Recruiter Dashboard
async function renderRecruiterDashboard(container, user) {
  const [summaryRes, upcomingRes, jobsRes] = await Promise.all([
    apiCall("/analytics/summary"),
    apiCall("/interviews/upcoming"),
    apiCall("/jobs?size=5&direction=desc")
  ]);

  const summary = summaryRes.data || {};
  const upcoming = upcomingRes.data || [];
  const jobs = (jobsRes.data && jobsRes.data.content) ? jobsRes.data.content : [];

  container.innerHTML = `
    <!-- Top Action Bar -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h2>Welcome back, ${escapeHtml(user.fullName)}</h2>
        <p class="text-sm text-muted">Overview of active requisitions, applicant volume, and pipeline conversion</p>
      </div>
      <div class="flex gap-2">
        <a href="jobs.html?action=new" class="btn btn-primary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Post New Job Opening
        </a>
      </div>
    </div>

    <!-- Stat KPI Cards -->
    <div class="grid grid-4 gap-4 mb-6">
      <div class="stat-card">
        <div>
          <div class="stat-label">Active Job Openings</div>
          <div class="stat-val">${summary.activeJobs || 0}</div>
        </div>
        <div class="stat-icon">💼</div>
      </div>
      <div class="stat-card stat-navy">
        <div>
          <div class="stat-label">Total Applicants</div>
          <div class="stat-val">${summary.totalApplications || 0}</div>
        </div>
        <div class="stat-icon">👥</div>
      </div>
      <div class="stat-card stat-warning">
        <div>
          <div class="stat-label">Pending Review</div>
          <div class="stat-val">${summary.pendingApplications || 0}</div>
        </div>
        <div class="stat-icon">⏳</div>
      </div>
      <div class="stat-card stat-success">
        <div>
          <div class="stat-label">Upcoming Interviews</div>
          <div class="stat-val">${summary.upcomingInterviews || 0}</div>
        </div>
        <div class="stat-icon">📅</div>
      </div>
    </div>

    <!-- Funnel & Pipeline Overview -->
    <div class="grid grid-3 gap-6 mb-6">
      <div style="grid-column: span 2;" class="card">
        <div class="card-header">
          <div>
            <h3>Recruitment Pipeline Funnel</h3>
            <p class="text-xs text-muted">Live candidate progression across stages</p>
          </div>
          <a href="applications.html" class="btn btn-outline btn-sm">Full ATS Pipeline &rarr;</a>
        </div>

        <div class="grid grid-4 gap-3 mt-4 text-center">
          <div style="background:var(--slate-50);padding:1rem;border-radius:var(--radius-sm);">
            <div class="text-xs text-muted text-bold">1. APPLIED</div>
            <div class="text-bold" style="font-size:1.5rem;color:var(--primary);">${summary.funnelData?.APPLIED || 0}</div>
          </div>
          <div style="background:var(--slate-50);padding:1rem;border-radius:var(--radius-sm);">
            <div class="text-xs text-muted text-bold">2. SCREENING</div>
            <div class="text-bold" style="font-size:1.5rem;color:var(--warning);">${summary.funnelData?.SCREENING || 0}</div>
          </div>
          <div style="background:var(--slate-50);padding:1rem;border-radius:var(--radius-sm);">
            <div class="text-xs text-muted text-bold">3. INTERVIEW</div>
            <div class="text-bold" style="font-size:1.5rem;color:var(--info);">${summary.funnelData?.INTERVIEW || 0}</div>
          </div>
          <div style="background:var(--slate-50);padding:1rem;border-radius:var(--radius-sm);">
            <div class="text-xs text-muted text-bold">4. HIRED</div>
            <div class="text-bold" style="font-size:1.5rem;color:var(--success);">${summary.funnelData?.SELECTED || 0}</div>
          </div>
        </div>
      </div>

      <!-- Upcoming Interviews Mini List -->
      <div class="card">
        <div class="card-header">
          <h4>Upcoming Interviews</h4>
          <a href="interviews.html" class="text-xs text-bold">Schedule &rarr;</a>
        </div>
        ${upcoming.length === 0 ? `
          <p class="text-sm text-muted text-center py-6">No interviews on your calendar.</p>
        ` : `
          <div class="flex flex-col gap-3">
            ${upcoming.slice(0, 3).map(iv => `
              <div style="border-left:3px solid var(--info);padding-left:0.75rem;">
                <div class="text-bold text-sm">${escapeHtml(iv.application.candidate.user.fullName)}</div>
                <div class="text-xs text-muted">${escapeHtml(iv.application.job.title)}</div>
                <div class="text-xs" style="color:var(--primary);margin-top:0.25rem;">
                  📅 ${formatDate(iv.interviewDate)} &bull; ${iv.interviewTime}
                </div>
              </div>
            `).join("")}
          </div>
        `}
      </div>
    </div>

    <!-- Active Requisitions Table -->
    <div class="card">
      <div class="card-header">
        <div>
          <h3>Recent Requisitions</h3>
          <p class="text-xs text-muted">Manage active listings and review applicants</p>
        </div>
        <a href="jobs.html" class="btn btn-outline btn-sm">All Job Openings &rarr;</a>
      </div>
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>Job Title</th>
              <th>Department</th>
              <th>Type</th>
              <th>Status</th>
              <th>Deadline</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            ${jobs.map(job => `
              <tr>
                <td class="text-bold">${escapeHtml(job.title)}</td>
                <td>${escapeHtml(job.department)}</td>
                <td><span class="badge badge-neutral">${escapeHtml(job.jobType)}</span></td>
                <td>${getStatusBadge(job.status)}</td>
                <td class="text-sm text-muted">${formatDate(job.deadline)}</td>
                <td>
                  <a href="applications.html?jobId=${job.id}" class="btn btn-primary btn-sm">View Pipeline</a>
                </td>
              </tr>
            `).join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// 3. HR Manager Dashboard
async function renderHRDashboard(container, user) {
  // Leverage common analytics & team metrics
  await renderRecruiterDashboard(container, user);
}

// 4. Admin Dashboard
async function renderAdminDashboard(container, user) {
  const [summaryRes, usersRes, auditRes] = await Promise.all([
    apiCall("/analytics/summary"),
    apiCall("/users?size=5"),
    apiCall("/users/audit-logs/recent")
  ]);

  const summary = summaryRes.data || {};
  const users = (usersRes.data && usersRes.data.content) ? usersRes.data.content : [];
  const auditLogs = auditRes.data || [];

  container.innerHTML = `
    <div class="flex justify-between items-center mb-6">
      <div>
        <h2>System Administration & Governance</h2>
        <p class="text-sm text-muted">Platform telemetry, role administration, security events, and audit logs</p>
      </div>
      <a href="users.html" class="btn btn-primary">
        Manage Users &amp; Permissions
      </a>
    </div>

    <!-- Admin KPI Cards -->
    <div class="grid grid-4 gap-4 mb-6">
      <div class="stat-card stat-navy">
        <div>
          <div class="stat-label">Total System Users</div>
          <div class="stat-val">${(usersRes.data && usersRes.data.totalElements) || users.length}</div>
        </div>
        <div class="stat-icon">🛡️</div>
      </div>
      <div class="stat-card">
        <div>
          <div class="stat-label">Active Job Openings</div>
          <div class="stat-val">${summary.activeJobs || 0}</div>
        </div>
        <div class="stat-icon">💼</div>
      </div>
      <div class="stat-card stat-success">
        <div>
          <div class="stat-label">Candidates Registered</div>
          <div class="stat-val">${summary.totalCandidates || 0}</div>
        </div>
        <div class="stat-icon">👤</div>
      </div>
      <div class="stat-card stat-warning">
        <div>
          <div class="stat-label">Hired Candidates</div>
          <div class="stat-val">${summary.hiredCandidates || 0}</div>
        </div>
        <div class="stat-icon">🎉</div>
      </div>
    </div>

    <div class="grid grid-2 gap-6 mb-6">
      <!-- User List Quick View -->
      <div class="card">
        <div class="card-header">
          <h3>Recent Accounts</h3>
          <a href="users.html" class="btn btn-outline btn-sm">View All &rarr;</a>
        </div>
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>User</th>
                <th>Role</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${users.map(u => `
                <tr>
                  <td>
                    <div class="text-bold">${escapeHtml(u.fullName)}</div>
                    <div class="text-xs text-muted">${escapeHtml(u.email)}</div>
                  </td>
                  <td><span class="badge badge-primary">${escapeHtml(u.role?.name?.replace("ROLE_", "") || "")}</span></td>
                  <td>${getStatusBadge(u.status)}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
      </div>

      <!-- Audit Log Quick Stream -->
      <div class="card">
        <div class="card-header">
          <h3>System Audit Trail</h3>
          <span class="badge badge-navy">Immutable Log</span>
        </div>
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>Action</th>
                <th>Entity</th>
                <th>Details</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              ${auditLogs.slice(0, 6).map(log => `
                <tr>
                  <td class="text-bold text-xs"><span class="badge badge-neutral">${escapeHtml(log.action)}</span></td>
                  <td class="text-xs">${escapeHtml(log.entityType)} #${log.entityId || ""}</td>
                  <td class="text-xs" style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${escapeHtml(log.details)}</td>
                  <td class="text-xs text-muted">${formatDate(log.timestamp)}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;
}
