/**
 * NexaHire - Interview Management & Feedback Scorecard Logic
 */

let allInterviews = [];

document.addEventListener("DOMContentLoaded", async () => {
  renderAppNavigation("interviews");
  const isCandidate = hasRole("CANDIDATE");
  const isStaff = hasAnyRole("ADMIN", "HR_MANAGER", "RECRUITER");

  if (isStaff) {
    document.getElementById("btn-schedule").style.display = "inline-flex";
    await populateApplicantOptions();
  }

  // Pre-fill application in schedule modal if appId in query
  const params = new URLSearchParams(window.location.search);
  const preAppId = params.get("appId");
  if (preAppId && isStaff) {
    openScheduleModal(preAppId);
  }

  loadInterviews();

  document.getElementById("schedule-form").addEventListener("submit", handleScheduleSubmit);
  document.getElementById("feedback-form").addEventListener("submit", handleFeedbackSubmit);
});

async function loadInterviews() {
  const isCandidate = hasRole("CANDIDATE");
  const isStaff = hasAnyRole("ADMIN", "HR_MANAGER", "RECRUITER");
  const tbody = document.getElementById("interviews-table-body");
  tbody.innerHTML = `<tr><td colspan="7" class="text-center p-6 text-muted">Refreshing interview schedule...</td></tr>`;

  try {
    const endpoint = isCandidate ? "/interviews/my" : "/interviews";
    const res = await apiCall(endpoint);
    allInterviews = res.data || [];

    if (allInterviews.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center p-6 text-muted">No interview rounds currently recorded.</td></tr>`;
      return;
    }

    tbody.innerHTML = allInterviews.map(iv => {
      const app = iv.application || {};
      const candidateUser = app.candidate?.user || {};
      const job = app.job || {};
      const isCompleted = iv.status === "COMPLETED";

      return `
        <tr>
          <td>
            <div class="text-bold">${escapeHtml(candidateUser.fullName || "Candidate")}</div>
            <div class="text-xs text-muted">${escapeHtml(candidateUser.email || "")}</div>
          </td>
          <td>
            <div class="text-bold text-sm">${escapeHtml(job.title || "Position")}</div>
            <div class="text-xs text-muted">${escapeHtml(job.department || "")}</div>
          </td>
          <td>
            <div class="text-bold text-sm">📅 ${formatDate(iv.interviewDate)}</div>
            <div class="text-xs text-muted">⏰ ${iv.interviewTime}</div>
          </td>
          <td>
            <span class="badge badge-neutral">${escapeHtml(iv.interviewType)}</span>
            ${iv.meetingLink ? `
              <div class="mt-1">
                <a href="${escapeHtml(iv.meetingLink)}" target="_blank" class="text-xs text-bold">Join Link &rarr;</a>
              </div>
            ` : ""}
          </td>
          <td>${getStatusBadge(iv.status)}</td>
          <td>
            ${iv.rating ? `
              <div class="text-xs">
                ${"⭐".repeat(iv.rating)} <span class="badge badge-${iv.recommendation === 'HIRE' ? 'success' : iv.recommendation === 'REJECT' ? 'danger' : 'warning'}">${escapeHtml(iv.recommendation)}</span>
              </div>
              <div class="text-xs text-muted mt-1" style="max-width:180px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(iv.feedback || "")}</div>
            ` : `<span class="text-xs text-muted">Pending Evaluation</span>`}
          </td>
          <td>
            <div class="flex gap-2">
              ${isStaff ? `
                <button class="btn btn-secondary btn-sm" onclick="openFeedbackModal(${iv.id}, '${escapeHtml(candidateUser.fullName)}', '${escapeHtml(job.title)}')">
                  ${isCompleted ? 'Edit Scorecard' : 'Submit Feedback'}
                </button>
              ` : `
                ${iv.meetingLink ? `<a href="${escapeHtml(iv.meetingLink)}" target="_blank" class="btn btn-primary btn-sm">Enter Room</a>` : `<span class="text-xs text-muted">Awaiting Link</span>`}
              `}
            </div>
          </td>
        </tr>
      `;
    }).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center p-6 text-danger">${escapeHtml(err.message)}</td></tr>`;
  }
}

async function populateApplicantOptions() {
  try {
    const res = await apiCall("/applications?size=100");
    const apps = (res.data && res.data.content) ? res.data.content : [];
    const select = document.getElementById("schedule-app-id");

    apps.forEach(app => {
      const opt = document.createElement("option");
      opt.value = app.id;
      opt.textContent = `${app.candidate?.user?.fullName || "Candidate"} - ${app.job?.title} (Stage: ${app.status})`;
      select.appendChild(opt);
    });
  } catch (err) {
    console.error("Failed to populate applicant select:", err);
  }
}

function openScheduleModal(preselectedAppId = null) {
  const select = document.getElementById("schedule-app-id");
  if (preselectedAppId) {
    select.value = preselectedAppId;
  }
  // Default date to tomorrow
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  document.getElementById("schedule-date").value = tomorrow.toISOString().split("T")[0];
  document.getElementById("schedule-time").value = "10:00";
  document.getElementById("schedule-link").value = "https://meet.google.com/nex-hire-room";
  openModal("schedule-modal");
}

async function handleScheduleSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById("btn-save-schedule");
  btn.disabled = true;
  btn.textContent = "Scheduling & Alerting...";

  const payload = {
    applicationId: parseInt(document.getElementById("schedule-app-id").value),
    interviewDate: document.getElementById("schedule-date").value,
    interviewTime: document.getElementById("schedule-time").value + ":00",
    interviewType: document.getElementById("schedule-type").value,
    meetingLink: document.getElementById("schedule-link").value.trim()
  };

  try {
    await apiCall("/interviews/schedule", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    showToast("Interview scheduled and email alert dispatched to candidate!", "success");
    closeModal("schedule-modal");
    loadInterviews();
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.textContent = "Confirm & Notify Candidate";
  }
}

function openFeedbackModal(interviewId, candidateName, jobTitle) {
  document.getElementById("feedback-interview-id").value = interviewId;
  const iv = allInterviews.find(i => i.id === interviewId);

  document.getElementById("feedback-candidate-summary").innerHTML = `
    <div class="text-bold">${escapeHtml(candidateName)}</div>
    <div class="text-xs text-muted">Role: ${escapeHtml(jobTitle)} &bull; Date: ${iv ? formatDate(iv.interviewDate) : ""}</div>
  `;

  if (iv && iv.rating) {
    document.getElementById("feedback-rating").value = String(iv.rating);
    document.getElementById("feedback-recommendation").value = iv.recommendation || "HIRE";
    document.getElementById("feedback-text").value = iv.feedback || "";
  } else {
    document.getElementById("feedback-rating").value = "4";
    document.getElementById("feedback-recommendation").value = "HIRE";
    document.getElementById("feedback-text").value = "";
  }

  openModal("feedback-modal");
}

async function handleFeedbackSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById("btn-save-feedback");
  btn.disabled = true;
  btn.textContent = "Saving...";

  const ivId = document.getElementById("feedback-interview-id").value;
  const payload = {
    rating: parseInt(document.getElementById("feedback-rating").value),
    recommendation: document.getElementById("feedback-recommendation").value,
    feedback: document.getElementById("feedback-text").value.trim(),
    status: "COMPLETED"
  };

  try {
    await apiCall(`/interviews/${ivId}/feedback`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
    showToast("Interview scorecard and feedback submitted!", "success");
    closeModal("feedback-modal");
    loadInterviews();
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.textContent = "Submit Scorecard";
  }
}
