/**
 * NexaHire - Job Management & Application Logic
 */

let currentPage = 0;
const pageSize = 8;
let totalPages = 1;

document.addEventListener("DOMContentLoaded", () => {
  renderAppNavigation("jobs");
  const user = getUser();
  const isStaff = hasAnyRole("ADMIN", "HR_MANAGER", "RECRUITER");

  if (isStaff) {
    const postBtn = document.getElementById("btn-post-job");
    if (postBtn) postBtn.style.display = "inline-flex";
  }

  // Check URL params for action=new
  const params = new URLSearchParams(window.location.search);
  if (params.get("action") === "new" && isStaff) {
    openCreateJobModal();
  }

  loadJobs(0);

  // Bind Form Submissions
  document.getElementById("job-form").addEventListener("submit", handleSaveJob);
  document.getElementById("apply-form").addEventListener("submit", handleApplyJob);
});

async function loadJobs(page = 0) {
  currentPage = page;
  const container = document.getElementById("jobs-container");
  const search = document.getElementById("search-input").value.trim();
  const dept = document.getElementById("dept-filter").value;
  const type = document.getElementById("type-filter").value;

  container.innerHTML = `<div class="text-center p-6 text-muted">Searching job requisitions...</div>`;

  try {
    let url = `/jobs?page=${page}&size=${pageSize}&direction=desc`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (dept) url += `&department=${encodeURIComponent(dept)}`;
    if (type) url += `&jobType=${encodeURIComponent(type)}`;

    const res = await apiCall(url);
    const data = res.data;
    const jobs = data.content || [];
    totalPages = data.totalPages || 1;

    renderJobsList(jobs);
    updatePagination(data);
  } catch (err) {
    container.innerHTML = `
      <div class="card text-center p-6 text-danger">
        Failed to load job listings: ${escapeHtml(err.message)}
      </div>
    `;
  }
}

function renderJobsList(jobs) {
  const container = document.getElementById("jobs-container");
  const isCandidate = hasRole("CANDIDATE");
  const isStaff = hasAnyRole("ADMIN", "HR_MANAGER", "RECRUITER");

  if (jobs.length === 0) {
    container.innerHTML = `
      <div class="card text-center p-6 text-muted">
        <h4>No job openings found matching your criteria</h4>
        <p class="text-sm mt-1">Try broadening your search keywords or resetting filters.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = jobs.map(job => {
    const skills = (job.requiredSkills || "").split(",").map(s => s.trim()).filter(Boolean);
    const isClosed = job.status === "CLOSED";

    return `
      <div class="card">
        <div class="flex justify-between items-center mb-2 flex-wrap gap-2">
          <div class="flex items-center gap-2">
            <span class="badge badge-primary">${escapeHtml(job.jobType)}</span>
            <span class="badge badge-neutral">${escapeHtml(job.department)}</span>
            ${getStatusBadge(job.status)}
          </div>
          <div class="text-xs text-muted">
            Posted ${formatDate(job.createdAt)} ${job.deadline ? `&bull; Deadline: ${formatDate(job.deadline)}` : ""}
          </div>
        </div>

        <div class="flex justify-between items-start gap-4 flex-wrap">
          <div style="flex:1;">
            <h3 class="mb-1">${escapeHtml(job.title)}</h3>
            <div class="text-sm text-muted mb-3">
              📍 ${escapeHtml(job.location)} &bull; Experience: ${job.experienceMin}-${job.experienceMax} Yrs &bull; Salary: ${formatCurrency(job.salaryMin)} - ${formatCurrency(job.salaryMax)}
            </div>
            <p class="text-sm mb-3" style="max-width:850px;color:var(--slate-700);">
              ${escapeHtml(job.description)}
            </p>
            <div class="mb-3">
              <span class="text-xs text-bold text-muted mr-1">REQUIRED SKILLS:</span>
              ${skills.map(s => `<span class="skill-tag">${escapeHtml(s)}</span>`).join("")}
            </div>
          </div>

          <!-- Actions -->
          <div class="flex items-center gap-2" style="align-self:center;">
            ${isCandidate ? `
              <button class="btn btn-primary btn-sm" ${isClosed ? 'disabled' : ''} onclick="openApplyModal(${job.id}, '${escapeHtml(job.title)}', '${escapeHtml(job.requiredSkills)}')">
                ${isClosed ? 'Position Closed' : 'Apply Now'}
              </button>
            ` : ""}

            ${isStaff ? `
              <a href="applications.html?jobId=${job.id}" class="btn btn-primary btn-sm">ATS Pipeline</a>
              <button class="btn btn-secondary btn-sm" onclick="editJob(${job.id})">Edit</button>
              <button class="btn btn-outline btn-sm" onclick="toggleJobStatus(${job.id}, '${isClosed ? 'ACTIVE' : 'CLOSED'}')">
                ${isClosed ? 'Reopen' : 'Close'}
              </button>
            ` : ""}
          </div>
        </div>
      </div>
    `;
  }).join("");
}

function updatePagination(data) {
  const controls = document.getElementById("pagination-controls");
  const indicator = document.getElementById("page-indicator");
  const btnPrev = document.getElementById("btn-prev");
  const btnNext = document.getElementById("btn-next");

  if (data.totalPages > 1) {
    controls.style.display = "flex";
    indicator.textContent = `Page ${data.number + 1} of ${data.totalPages}`;
    btnPrev.disabled = data.first;
    btnNext.disabled = data.last;
  } else {
    controls.style.display = "none";
  }
}

function changePage(delta) {
  const target = currentPage + delta;
  if (target >= 0 && target < totalPages) {
    loadJobs(target);
  }
}

function resetFilters() {
  document.getElementById("search-input").value = "";
  document.getElementById("dept-filter").value = "";
  document.getElementById("type-filter").value = "";
  loadJobs(0);
}

// Job Creation / Editing Modal
function openCreateJobModal() {
  document.getElementById("job-form").reset();
  document.getElementById("job-id").value = "";
  document.getElementById("job-modal-title").textContent = "Post New Job Opening";
  openModal("job-modal");
}

async function editJob(jobId) {
  try {
    const res = await apiCall(`/jobs/${jobId}`);
    const job = res.data;
    document.getElementById("job-id").value = job.id;
    document.getElementById("job-title").value = job.title;
    document.getElementById("job-department").value = job.department;
    document.getElementById("job-type").value = job.jobType;
    document.getElementById("job-location").value = job.location;
    document.getElementById("job-exp-min").value = job.experienceMin || 0;
    document.getElementById("job-exp-max").value = job.experienceMax || 10;
    document.getElementById("job-salary-min").value = job.salaryMin || "";
    document.getElementById("job-salary-max").value = job.salaryMax || "";
    document.getElementById("job-required-skills").value = job.requiredSkills || "";
    document.getElementById("job-description").value = job.description || "";
    document.getElementById("job-status").value = job.status || "ACTIVE";
    document.getElementById("job-deadline").value = job.deadline || "";

    document.getElementById("job-modal-title").textContent = "Edit Job Opening";
    openModal("job-modal");
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function handleSaveJob(e) {
  e.preventDefault();
  const btn = document.getElementById("btn-save-job");
  btn.disabled = true;
  btn.textContent = "Saving...";

  const jobId = document.getElementById("job-id").value;
  const payload = {
    title: document.getElementById("job-title").value.trim(),
    department: document.getElementById("job-department").value.trim(),
    jobType: document.getElementById("job-type").value,
    location: document.getElementById("job-location").value.trim(),
    experienceMin: parseInt(document.getElementById("job-exp-min").value) || 0,
    experienceMax: parseInt(document.getElementById("job-exp-max").value) || 10,
    salaryMin: parseFloat(document.getElementById("job-salary-min").value) || null,
    salaryMax: parseFloat(document.getElementById("job-salary-max").value) || null,
    requiredSkills: document.getElementById("job-required-skills").value.trim(),
    description: document.getElementById("job-description").value.trim(),
    status: document.getElementById("job-status").value,
    deadline: document.getElementById("job-deadline").value || null
  };

  try {
    if (jobId) {
      await apiCall(`/jobs/${jobId}`, {
        method: "PUT",
        body: JSON.stringify(payload)
      });
      showToast("Job opening updated successfully!", "success");
    } else {
      await apiCall("/jobs", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      showToast("New job requisition created successfully!", "success");
    }

    closeModal("job-modal");
    loadJobs(currentPage);
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.textContent = "Save Opening";
  }
}

async function toggleJobStatus(jobId, newStatus) {
  try {
    await apiCall(`/jobs/${jobId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: newStatus })
    });
    showToast(`Job status updated to ${newStatus}`, "info");
    loadJobs(currentPage);
  } catch (err) {
    showToast(err.message, "error");
  }
}

// Application Modal
function openApplyModal(jobId, title, requiredSkills) {
  document.getElementById("apply-job-id").value = jobId;
  document.getElementById("apply-modal-title").textContent = `Apply for: ${title}`;
  document.getElementById("apply-job-info").innerHTML = `
    <div class="text-bold mb-1">${escapeHtml(title)}</div>
    <div class="text-xs text-muted mb-2">Our transparent keyword matching algorithm will evaluate your recorded skills and parsed resume against:</div>
    <div>${requiredSkills.split(",").map(s => `<span class="skill-tag">${escapeHtml(s.trim())}</span>`).join("")}</div>
  `;
  openModal("apply-modal");
}

async function handleApplyJob(e) {
  e.preventDefault();
  const btn = document.getElementById("btn-submit-apply");
  btn.disabled = true;
  btn.textContent = "Analyzing & Submitting...";

  const jobId = document.getElementById("apply-job-id").value;
  const coverLetter = document.getElementById("apply-cover-letter").value.trim();

  try {
    const res = await apiCall("/applications/apply", {
      method: "POST",
      body: JSON.stringify({ jobId: parseInt(jobId), coverLetter })
    });

    closeModal("apply-modal");
    showToast(res.message || "Application successfully submitted!", "success");
    setTimeout(() => {
      window.location.href = "applications.html";
    }, 1200);
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.textContent = "Submit Application";
  }
}
