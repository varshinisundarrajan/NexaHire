/**
 * NexaHire - Recruitment Funnel & Analytics Logic
 */

document.addEventListener("DOMContentLoaded", async () => {
  renderAppNavigation("analytics");
  loadAnalytics();
});

async function loadAnalytics() {
  try {
    const res = await apiCall("/analytics/summary");
    const summary = res.data;

    // Populate KPIs
    document.getElementById("kpi-jobs").textContent = summary.activeJobs || 0;
    document.getElementById("kpi-apps").textContent = summary.totalApplications || 0;
    document.getElementById("kpi-shortlisted").textContent = summary.shortlistedCandidates || 0;
    document.getElementById("kpi-hired").textContent = summary.hiredCandidates || 0;

    renderFunnel(summary);
    renderDepartmentChart(summary.departmentBreakdown || {});
  } catch (err) {
    showToast(err.message, "error");
  }
}

function renderFunnel(summary) {
  const container = document.getElementById("funnel-container");
  const funnel = summary.funnelData || {};

  const stages = [
    { key: "APPLIED", name: "1. Total Applications Received", color: "#2563eb" },
    { key: "SCREENING", name: "2. Under Resume Screening", color: "#f59e0b" },
    { key: "SHORTLISTED", name: "3. Shortlisted for Assessment", color: "#eab308" },
    { key: "INTERVIEW", name: "4. Technical & Culture Interviews", color: "#0ea5e9" },
    { key: "SELECTED", name: "5. Extended Job Offers / Hired", color: "#10b981" }
  ];

  const totalApps = Math.max(summary.totalApplications || 1, 1);

  container.innerHTML = stages.map(s => {
    const count = funnel[s.key] || 0;
    const percentage = Math.round((count / totalApps) * 100);

    return `
      <div class="funnel-step">
        <div class="funnel-bar-bg" style="width: ${Math.max(percentage, 8)}%; background-color: ${s.color}20;"></div>
        <div class="funnel-content">
          <div>
            <div class="text-bold text-sm" style="color:var(--navy-900);">${s.name}</div>
            <div class="text-xs text-muted">Conversion rate: ${percentage}% of applicants</div>
          </div>
          <div class="text-right">
            <div class="text-bold" style="font-size:1.3rem;color:${s.color};">${count}</div>
          </div>
        </div>
      </div>
    `;
  }).join("");

  // Update rates in table
  const screeningCount = (funnel["SCREENING"] || 0) + (funnel["SHORTLISTED"] || 0) + (funnel["INTERVIEW"] || 0) + (funnel["SELECTED"] || 0);
  const screeningRate = Math.round((screeningCount / totalApps) * 100);
  document.getElementById("metric-screening-rate").textContent = `${screeningRate}% (${screeningCount} candidates passed initial review)`;

  const interviewCount = (funnel["INTERVIEW"] || 0) + (funnel["SELECTED"] || 0);
  const hiredCount = funnel["SELECTED"] || 0;
  const interviewRate = interviewCount > 0 ? Math.round((hiredCount / interviewCount) * 100) : 0;
  document.getElementById("metric-interview-rate").textContent = `${interviewRate}% (${hiredCount} hired of ${interviewCount} interviewed)`;
}

function renderDepartmentChart(departments) {
  const container = document.getElementById("department-chart");
  const entries = Object.entries(departments);

  if (entries.length === 0) {
    container.innerHTML = `<p class="text-sm text-muted text-center py-6">No departmental requisition data yet.</p>`;
    return;
  }

  const maxVal = Math.max(...entries.map(e => e[1]), 1);

  container.innerHTML = entries.map(([dept, count]) => {
    const widthPct = Math.round((count / maxVal) * 100);

    return `
      <div class="chart-bar-row">
        <div class="chart-bar-label" title="${escapeHtml(dept)}">${escapeHtml(dept)}</div>
        <div class="chart-bar-track">
          <div class="chart-bar-fill" style="width: ${Math.max(widthPct, 12)}%;">
            ${count}
          </div>
        </div>
      </div>
    `;
  }).join("");
}
