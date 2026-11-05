/**
 * NexaHire - Candidate Management Logic
 */

let currentCandidatePage = 0;
const candidatePageSize = 8;
let candidateTotalPages = 1;

document.addEventListener("DOMContentLoaded", () => {
  renderAppNavigation("candidates");
  loadCandidates(0);
});

async function loadCandidates(page = 0) {
  currentCandidatePage = page;
  const container = document.getElementById("candidates-container");
  const search = document.getElementById("candidate-search-input").value.trim();

  container.innerHTML = `<div class="text-center p-6 text-muted" style="grid-column: span 2;">Scanning candidate directory...</div>`;

  try {
    let url = `/candidates?page=${page}&size=${candidatePageSize}&direction=desc`;
    if (search) url += `&search=${encodeURIComponent(search)}`;

    const res = await apiCall(url);
    const data = res.data;
    const candidates = data.content || [];
    candidateTotalPages = data.totalPages || 1;

    renderCandidates(candidates);
    updateCandidatePagination(data);
  } catch (err) {
    container.innerHTML = `<div class="card text-danger text-center p-6" style="grid-column: span 2;">Failed to load candidates: ${escapeHtml(err.message)}</div>`;
  }
}

function renderCandidates(candidates) {
  const container = document.getElementById("candidates-container");

  if (candidates.length === 0) {
    container.innerHTML = `
      <div class="card text-center p-6 text-muted" style="grid-column: span 2;">
        <h4>No candidates found</h4>
        <p class="text-sm mt-1">Try searching by other skill terms or clear your search input.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = candidates.map(c => {
    const user = c.user || {};
    const skills = (c.skills || "").split(",").map(s => s.trim()).filter(Boolean);

    return `
      <div class="card">
        <div class="flex items-start gap-3 mb-3">
          <div class="avatar" style="width:44px;height:44px;font-size:1.1rem;">${escapeHtml(user.fullName ? user.fullName[0] : "C")}</div>
          <div style="flex:1;">
            <div class="flex justify-between items-center">
              <h4 style="font-size:1.05rem;">${escapeHtml(user.fullName || "Candidate")}</h4>
              <span class="badge badge-neutral text-xs">⭐ ${c.experienceYears || 0} Yrs Exp</span>
            </div>
            <div class="text-xs text-muted">${escapeHtml(c.headline || "Job Seeker")}</div>
            <div class="text-xs text-muted mt-0.5">📍 ${escapeHtml(c.location || "Remote / Unspecified")}</div>
          </div>
        </div>

        <p class="text-sm mb-3" style="color:var(--slate-600);display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
          ${escapeHtml(c.summary || "No professional summary provided.")}
        </p>

        <div class="mb-4">
          <div class="text-xs text-bold text-muted mb-1">SKILLS:</div>
          <div>
            ${skills.slice(0, 5).map(s => `<span class="skill-tag">${escapeHtml(s)}</span>`).join("")}
            ${skills.length > 5 ? `<span class="text-xs text-muted">+${skills.length - 5} more</span>` : ""}
          </div>
        </div>

        <div class="flex justify-between items-center pt-3" style="border-top:1px solid var(--slate-100);">
          <span class="text-xs text-muted">Joined ${formatDate(c.createdAt)}</span>
          <button class="btn btn-outline btn-sm" onclick="viewCandidateDossier(${c.id})">View Profile Dossier</button>
        </div>
      </div>
    `;
  }).join("");
}

function updateCandidatePagination(data) {
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
  const target = currentCandidatePage + delta;
  if (target >= 0 && target < candidateTotalPages) {
    loadCandidates(target);
  }
}

function resetSearch() {
  document.getElementById("candidate-search-input").value = "";
  loadCandidates(0);
}

async function viewCandidateDossier(candidateId) {
  try {
    const [candRes, resumeRes] = await Promise.all([
      apiCall(`/candidates/${candidateId}`),
      apiCall(`/candidates/${candidateId}/resumes`)
    ]);

    const c = candRes.data;
    const user = c.user || {};
    const resumes = resumeRes.data || [];
    const skills = (c.skills || "").split(",").map(s => s.trim()).filter(Boolean);

    const body = document.getElementById("candidate-modal-body");
    body.innerHTML = `
      <div class="flex items-start gap-4 mb-4">
        <div class="avatar" style="width:56px;height:56px;font-size:1.5rem;">${escapeHtml(user.fullName ? user.fullName[0] : "C")}</div>
        <div>
          <h2>${escapeHtml(user.fullName)}</h2>
          <div class="text-sm text-bold" style="color:var(--primary);">${escapeHtml(c.headline || "Job Seeker")}</div>
          <div class="text-xs text-muted mt-1">
            📧 ${escapeHtml(user.email)} &bull; 📞 ${escapeHtml(user.phone || "N/A")} &bull; 📍 ${escapeHtml(c.location || "N/A")}
          </div>
        </div>
      </div>

      <div class="card mb-4" style="background:var(--slate-50);padding:1rem;">
        <h4 class="mb-1 text-sm">Professional Summary</h4>
        <p class="text-sm" style="color:var(--slate-700);">${escapeHtml(c.summary || "No detailed summary provided.")}</p>
      </div>

      <div class="grid grid-2 gap-4 mb-4">
        <div>
          <h4 class="mb-1 text-sm">Experience &amp; Education</h4>
          <div class="text-sm text-muted"><strong>Experience:</strong> ${c.experienceYears || 0} years</div>
          <div class="text-sm text-muted mt-1"><strong>Education:</strong> ${escapeHtml(c.education || "N/A")}</div>
        </div>
        <div>
          <h4 class="mb-1 text-sm">Compensation</h4>
          <div class="text-sm text-muted"><strong>Current:</strong> ${formatCurrency(c.currentSalary)}</div>
          <div class="text-sm text-muted mt-1"><strong>Expected:</strong> ${formatCurrency(c.expectedSalary)}</div>
        </div>
      </div>

      <div class="mb-4">
        <h4 class="mb-2 text-sm">Technical &amp; Functional Skills</h4>
        <div>${skills.map(s => `<span class="skill-tag">${escapeHtml(s)}</span>`).join("")}</div>
      </div>

      <div class="mb-2">
        <h4 class="mb-2 text-sm">Uploaded Resumes (${resumes.length})</h4>
        ${resumes.length === 0 ? `
          <p class="text-xs text-muted">No resume uploaded to date.</p>
        ` : `
          <div class="flex flex-col gap-2">
            ${resumes.map(r => `
              <div class="flex justify-between items-center p-2" style="background:var(--slate-100);border-radius:var(--radius-sm);">
                <div class="text-xs text-bold">📄 ${escapeHtml(r.fileName)} (${Math.round(r.fileSize / 1024)} KB)</div>
                <a href="${API_BASE.replace('/api', '')}/uploads/${escapeHtml(r.storagePath)}" target="_blank" class="btn btn-outline btn-sm" style="padding:0.2rem 0.6rem;font-size:0.75rem;">Download</a>
              </div>
            `).join("")}
          </div>
        `}
      </div>
    `;

    openModal("candidate-modal");
  } catch (err) {
    showToast(err.message, "error");
  }
}
