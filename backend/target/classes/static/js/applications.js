/**
 * NexaHire - ATS Pipeline & Application Workflow Logic
 */

let currentView = "kanban";
let applicationsData = [];
let searchTimeout = null;

const PIPELINE_STAGES = [
  { id: "APPLIED", title: "Applied", color: "var(--primary)" },
  { id: "SCREENING", title: "Screening", color: "var(--warning)" },
  { id: "SHORTLISTED", title: "Shortlisted", color: "#f59e0b" },
  { id: "INTERVIEW", title: "Interview", color: "var(--info)" },
  { id: "SELECTED", title: "Selected", color: "var(--success)" },
  { id: "REJECTED", title: "Rejected", color: "var(--danger)" }
];

document.addEventListener("DOMContentLoaded", async () => {
  renderAppNavigation("applications");
  const user = getUser();
  const isCandidate = hasRole("CANDIDATE");
  const isStaff = hasAnyRole("ADMIN", "HR_MANAGER", "RECRUITER");

  if (isCandidate) {
    document.getElementById("page-header-title").textContent = "My Job Applications";
    loadCandidateApplications();
  } else if (isStaff) {
    document.getElementById("staff-view-toggle").style.display = "inline-flex";
    document.getElementById("ats-filter-bar").style.display = "block";
    await populateJobsFilter();
    loadPipelineData();
  }

  document.getElementById("status-form").addEventListener("submit", handleStatusUpdate);
});

function switchView(view) {
  currentView = view;
  const btnKanban = document.getElementById("btn-kanban");
  const btnTable = document.getElementById("btn-table");
  const kanbanContainer = document.getElementById("kanban-view");
  const tableContainer = document.getElementById("table-view");

  if (view === "kanban") {
    btnKanban.classList.add("active");
    btnTable.classList.remove("active");
    kanbanContainer.style.display = "flex";
    tableContainer.style.display = "none";
    renderKanban(applicationsData);
  } else {
    btnTable.classList.add("active");
    btnKanban.classList.remove("active");
    kanbanContainer.style.display = "none";
    tableContainer.style.display = "block";
    renderTable(applicationsData);
  }
}

async function populateJobsFilter() {
  try {
    const res = await apiCall("/jobs?size=50");
    const select = document.getElementById("pipeline-job-select");
    const jobs = (res.data && res.data.content) ? res.data.content : [];

    const urlParams = new URLSearchParams(window.location.search);
    const preselectedJobId = urlParams.get("jobId");

    jobs.forEach(j => {
      const opt = document.createElement("option");
      opt.value = j.id;
      opt.textContent = `${j.title} (${j.department})`;
      if (preselectedJobId && String(j.id) === preselectedJobId) {
        opt.selected = true;
      }
      select.appendChild(opt);
    });
  } catch (err) {
    console.error("Failed to load jobs list for filter:", err);
  }
}

async function loadPipelineData() {
  const jobId = document.getElementById("pipeline-job-select")?.value;
  const search = document.getElementById("candidate-search")?.value.trim();

  let url = `/applications?size=100`;
  if (jobId) url += `&jobId=${jobId}`;
  if (search) url += `&search=${encodeURIComponent(search)}`;

  try {
    const res = await apiCall(url);
    applicationsData = (res.data && res.data.content) ? res.data.content : [];

    if (currentView === "kanban") {
      renderKanban(applicationsData);
    } else {
      renderTable(applicationsData);
    }
  } catch (err) {
    showToast(err.message, "error");
  }
}

function debounceSearch() {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(loadPipelineData, 350);
}

// 1. Kanban Pipeline Renderer
function renderKanban(apps) {
  const container = document.getElementById("kanban-view");
  container.innerHTML = "";

  PIPELINE_STAGES.forEach(stage => {
    const stageApps = apps.filter(a => a.status === stage.id);
    const col = document.createElement("div");
    col.className = "pipeline-col";

    col.innerHTML = `
      <div class="pipeline-header">
        <span style="color:${stage.color};">${stage.title}</span>
        <span class="badge badge-neutral">${stageApps.length}</span>
      </div>
      <div class="pipeline-list">
        ${stageApps.length === 0 ? `
          <div class="text-xs text-muted text-center py-6" style="border:1px dashed var(--slate-300);border-radius:var(--radius-sm);">
            No applicants in this stage
          </div>
        ` : stageApps.map(app => renderCandidateCardHtml(app)).join("")}
      </div>
    `;

    container.appendChild(col);
  });
}

function renderCandidateCardHtml(app) {
  const user = app.candidate?.user || {};
  const matchedList = (app.matchedSkills || "").split(",").map(s => s.trim()).filter(Boolean);

  return `
    <div class="candidate-card" onclick="openApplicationDetail(${app.id})">
      <div class="flex justify-between items-center mb-1">
        <span class="text-bold text-sm">${escapeHtml(user.fullName || "Candidate")}</span>
        ${getMatchScoreBadge(app.matchScore)}
      </div>
      <div class="text-xs text-muted mb-2">
        💼 ${escapeHtml(app.job?.title || "Role")}
      </div>
      <div class="mb-2">
        ${matchedList.slice(0, 3).map(s => `<span class="skill-tag matched">${escapeHtml(s)}</span>`).join("")}
        ${matchedList.length > 3 ? `<span class="text-xs text-muted">+${matchedList.length - 3}</span>` : ""}
      </div>
      <div class="flex justify-between items-center mt-2 pt-2" style="border-top:1px solid var(--slate-100);" onclick="event.stopPropagation()">
        <span class="text-xs text-muted">${formatDate(app.appliedAt)}</span>
        <button class="btn btn-secondary btn-sm" style="padding:0.2rem 0.5rem;font-size:0.75rem;" onclick="openStatusModal(${app.id}, '${escapeHtml(user.fullName)}', '${app.status}')">
          Move &rarr;
        </button>
      </div>
    </div>
  `;
}

// 2. Table View Renderer
function renderTable(apps) {
  const tbody = document.getElementById("table-body");
  if (apps.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center p-6 text-muted">No applications found</td></tr>`;
    return;
  }

  tbody.innerHTML = apps.map(app => `
    <tr>
      <td>
        <div class="text-bold">${escapeHtml(app.candidate?.user?.fullName || "Candidate")}</div>
        <div class="text-xs text-muted">${escapeHtml(app.candidate?.user?.email || "")}</div>
      </td>
      <td>
        <div class="text-bold text-sm">${escapeHtml(app.job?.title)}</div>
        <div class="text-xs text-muted">${escapeHtml(app.job?.department)}</div>
      </td>
      <td>${getMatchScoreBadge(app.matchScore)}</td>
      <td>${getStatusBadge(app.status)}</td>
      <td class="text-sm text-muted">${formatDate(app.appliedAt)}</td>
      <td>
        <div class="flex gap-2">
          <button class="btn btn-secondary btn-sm" onclick="openApplicationDetail(${app.id})">Dossier</button>
          <button class="btn btn-primary btn-sm" onclick="openStatusModal(${app.id}, '${escapeHtml(app.candidate?.user?.fullName)}', '${app.status}')">Advance</button>
        </div>
      </td>
    </tr>
  `).join("");
}

// 3. Candidate's own view
async function loadCandidateApplications() {
  const container = document.getElementById("kanban-view");
  container.style.display = "block";

  try {
    const res = await apiCall("/applications/my");
    const apps = res.data || [];

    if (apps.length === 0) {
      container.innerHTML = `
        <div class="card text-center p-6 text-muted">
          <h3>No applications submitted yet</h3>
          <p class="text-sm mt-1">Explore active job opportunities and apply directly with transparent skill scoring.</p>
          <a href="jobs.html" class="btn btn-primary btn-sm mt-4">Browse Jobs</a>
        </div>
      `;
      return;
    }

    container.innerHTML = `
      <div class="flex flex-col gap-4">
        ${apps.map(app => `
          <div class="card">
            <div class="flex justify-between items-center mb-2 flex-wrap gap-2">
              <div>
                <h3 class="mb-1">${escapeHtml(app.job?.title)}</h3>
                <div class="text-sm text-muted">${escapeHtml(app.job?.department)} &bull; ${escapeHtml(app.job?.location)}</div>
              </div>
              <div class="flex items-center gap-2">
                ${getMatchScoreBadge(app.matchScore)}
                ${getStatusBadge(app.status)}
              </div>
            </div>

            <div class="grid grid-2 gap-4 mt-3 pt-3" style="border-top:1px solid var(--slate-100);">
              <div>
                <div class="text-xs text-bold text-muted mb-1">SKILL BREAKDOWN:</div>
                <div class="mb-2">
                  <span class="text-xs text-success text-bold">Matched:</span>
                  ${(app.matchedSkills || "None").split(",").map(s => `<span class="skill-tag matched">${escapeHtml(s.trim())}</span>`).join("")}
                </div>
                ${app.missingSkills ? `
                  <div>
                    <span class="text-xs text-danger text-bold">Missing:</span>
                    ${app.missingSkills.split(",").map(s => `<span class="skill-tag missing">${escapeHtml(s.trim())}</span>`).join("")}
                  </div>
                ` : ""}
              </div>
              <div>
                <div class="text-xs text-bold text-muted mb-1">RECRUITER NOTES:</div>
                <p class="text-sm" style="color:var(--slate-700);">${escapeHtml(app.recruiterNotes || "Application is under active review by the talent acquisition team.")}</p>
                <div class="text-xs text-muted mt-2">Applied on ${formatDate(app.appliedAt)}</div>
              </div>
            </div>
          </div>
        `).join("")}
      </div>
    `;
  } catch (err) {
    container.innerHTML = `<div class="card text-danger text-center p-6">${escapeHtml(err.message)}</div>`;
  }
}

// Modals
function openStatusModal(appId, candidateName, currentStatus) {
  document.getElementById("status-app-id").value = appId;
  document.getElementById("new-stage-select").value = currentStatus;
  document.getElementById("status-modal-title").textContent = `Update Stage for: ${candidateName}`;
  document.getElementById("status-candidate-details").innerHTML = `
    <div class="text-bold">${escapeHtml(candidateName)}</div>
    <div class="text-xs text-muted">Current Stage: <span class="badge badge-primary">${currentStatus}</span></div>
  `;
  openModal("status-modal");
}

async function handleStatusUpdate(e) {
  e.preventDefault();
  const appId = document.getElementById("status-app-id").value;
  const newStage = document.getElementById("new-stage-select").value;
  const notes = document.getElementById("stage-notes").value.trim();

  try {
    await apiCall(`/applications/${appId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: newStage, recruiterNotes: notes })
    });
    showToast(`Application successfully transitioned to ${newStage}`, "success");
    closeModal("status-modal");
    loadPipelineData();
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function openApplicationDetail(appId) {
  try {
    const res = await apiCall(`/applications/${appId}`);
    const app = res.data;
    const user = app.candidate?.user || {};
    const candidate = app.candidate || {};

    const modalBody = document.getElementById("app-detail-body");
    modalBody.innerHTML = `
      <div class="flex justify-between items-start mb-4">
        <div>
          <h2>${escapeHtml(user.fullName || "Candidate")}</h2>
          <div class="text-sm text-muted">${escapeHtml(candidate.headline || "Applicant")} &bull; 📍 ${escapeHtml(candidate.location || "N/A")}</div>
          <div class="text-xs text-muted mt-1">📧 ${escapeHtml(user.email)} &bull; 📞 ${escapeHtml(user.phone || "N/A")}</div>
        </div>
        <div class="text-right">
          <div class="mb-1">${getMatchScoreBadge(app.matchScore)}</div>
          <div>${getStatusBadge(app.status)}</div>
        </div>
      </div>

      <div class="card mb-4" style="background:var(--slate-50);padding:1rem;">
        <h4 class="mb-1">Target Position: ${escapeHtml(app.job?.title)}</h4>
        <div class="text-xs text-muted mb-2">${escapeHtml(app.job?.department)} &bull; ${escapeHtml(app.job?.jobType)}</div>
        <div class="text-xs text-bold text-muted mb-1">JOB REQUIRED SKILLS:</div>
        <div>${(app.job?.requiredSkills || "").split(",").map(s => `<span class="skill-tag">${escapeHtml(s.trim())}</span>`).join("")}</div>
      </div>

      <div class="mb-4">
        <h4 class="mb-2">Transparent Keyword Matching Analysis</h4>
        <div class="grid grid-2 gap-3">
          <div style="background:var(--success-bg);border:1px solid #a7f3d0;padding:0.75rem;border-radius:var(--radius-sm);">
            <div class="text-xs text-bold text-success mb-1">MATCHED SKILLS:</div>
            <div>${(app.matchedSkills || "None").split(",").map(s => `<span class="skill-tag matched">${escapeHtml(s.trim())}</span>`).join("")}</div>
          </div>
          <div style="background:var(--danger-bg);border:1px solid #fecaca;padding:0.75rem;border-radius:var(--radius-sm);">
            <div class="text-xs text-bold text-danger mb-1">MISSING SKILLS:</div>
            <div>${(app.missingSkills || "None").split(",").map(s => `<span class="skill-tag missing">${escapeHtml(s.trim())}</span>`).join("")}</div>
          </div>
        </div>
      </div>

      <div class="mb-4">
        <h4 class="mb-1">Applicant Cover Letter</h4>
        <p class="text-sm" style="background:var(--slate-50);padding:1rem;border-radius:var(--radius-sm);color:var(--slate-700);white-space:pre-wrap;">${escapeHtml(app.coverLetter || "No cover letter provided.")}</p>
      </div>

      <div class="flex justify-between items-center pt-3" style="border-top:1px solid var(--slate-200);">
        <div class="text-xs text-muted">Applied on ${formatDate(app.appliedAt)}</div>
        <div class="flex gap-2">
          <a href="interviews.html?appId=${app.id}" class="btn btn-primary btn-sm">Schedule Interview</a>
          <button class="btn btn-secondary btn-sm" onclick="closeModal('app-detail-modal'); openStatusModal(${app.id}, '${escapeHtml(user.fullName)}', '${app.status}')">Update Stage</button>
        </div>
      </div>
    `;

    openModal("app-detail-modal");
  } catch (err) {
    showToast(err.message, "error");
  }
}
