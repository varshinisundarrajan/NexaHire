/**
 * NexaHire - Candidate Profile & Resume Upload Logic
 */

let currentCandidateId = null;

document.addEventListener("DOMContentLoaded", () => {
  renderAppNavigation("profile");
  loadProfile();

  document.getElementById("profile-form").addEventListener("submit", handleProfileSave);

  // Setup drag and drop for dropzone
  const dropzone = document.getElementById("dropzone");
  dropzone.addEventListener("dragover", (e) => {
    e.preventDefault();
    dropzone.classList.add("dragover");
  });
  dropzone.addEventListener("dragleave", () => {
    dropzone.classList.remove("dragover");
  });
  dropzone.addEventListener("drop", (e) => {
    e.preventDefault();
    dropzone.classList.remove("dragover");
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      uploadFile(e.dataTransfer.files[0]);
    }
  });
});

async function loadProfile() {
  try {
    const res = await apiCall("/candidates/profile/me");
    const profile = res.data;
    const user = profile.user || {};
    currentCandidateId = profile.id;

    document.getElementById("profile-name").value = user.fullName || "";
    document.getElementById("profile-phone").value = user.phone || "";
    document.getElementById("profile-headline").value = profile.headline || "";
    document.getElementById("profile-summary").value = profile.summary || "";
    document.getElementById("profile-skills").value = profile.skills || "";
    document.getElementById("profile-exp").value = profile.experienceYears || "";
    document.getElementById("profile-education").value = profile.education || "";
    document.getElementById("profile-location").value = profile.location || "";
    document.getElementById("profile-current-sal").value = profile.currentSalary || "";
    document.getElementById("profile-expected-sal").value = profile.expectedSalary || "";

    loadResumes(profile.id);
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function handleProfileSave(e) {
  e.preventDefault();
  const btn = document.getElementById("btn-save-profile");
  btn.disabled = true;
  btn.textContent = "Saving Changes...";

  const payload = {
    fullName: document.getElementById("profile-name").value.trim(),
    phone: document.getElementById("profile-phone").value.trim(),
    headline: document.getElementById("profile-headline").value.trim(),
    summary: document.getElementById("profile-summary").value.trim(),
    skills: document.getElementById("profile-skills").value.trim(),
    experienceYears: parseFloat(document.getElementById("profile-exp").value) || 0,
    education: document.getElementById("profile-education").value.trim(),
    location: document.getElementById("profile-location").value.trim(),
    currentSalary: parseFloat(document.getElementById("profile-current-sal").value) || null,
    expectedSalary: parseFloat(document.getElementById("profile-expected-sal").value) || null
  };

  try {
    await apiCall("/candidates/profile/me", {
      method: "PUT",
      body: JSON.stringify(payload)
    });
    showToast("Profile details updated successfully!", "success");
    // Update cached user full name
    const u = getUser();
    if (u) {
      u.fullName = payload.fullName;
      localStorage.setItem("nexahire_user", JSON.stringify(u));
      renderAppNavigation("profile");
    }
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.textContent = "Save Profile Changes";
  }
}

function handleFileSelect(e) {
  if (e.target.files && e.target.files[0]) {
    uploadFile(e.target.files[0]);
  }
}

async function uploadFile(file) {
  // Validate client side
  const validExtensions = [".pdf", ".docx", ".doc", ".txt"];
  const fileName = file.name.toLowerCase();
  const isValid = validExtensions.some(ext => fileName.endsWith(ext));

  if (!isValid) {
    showToast("Invalid file format. Please select a PDF, DOCX, or TXT document.", "error");
    return;
  }

  if (file.size > 10 * 1024 * 1024) {
    showToast("File size exceeds 10MB limit.", "error");
    return;
  }

  const progress = document.getElementById("upload-progress");
  progress.style.display = "block";
  progress.textContent = "Uploading resume and extracting keywords...";

  const formData = new FormData();
  formData.append("file", file);

  try {
    const res = await apiCall("/candidates/resume", {
      method: "POST",
      body: formData
    });

    showToast("Resume uploaded and parsed successfully!", "success");
    if (currentCandidateId) {
      loadResumes(currentCandidateId);
    }
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    progress.style.display = "none";
    document.getElementById("resume-file-input").value = "";
  }
}

async function loadResumes(candidateId) {
  try {
    const res = await apiCall(`/candidates/${candidateId}/resumes`);
    const resumes = res.data || [];

    document.getElementById("resume-count-badge").textContent = resumes.length;
    const container = document.getElementById("resumes-list");

    if (resumes.length === 0) {
      container.innerHTML = `<p class="text-xs text-muted text-center py-4">No resumes on file.</p>`;
      return;
    }

    container.innerHTML = resumes.map(r => `
      <div class="flex justify-between items-center p-2" style="background:var(--slate-50);border:1px solid var(--slate-200);border-radius:var(--radius-sm);">
        <div style="flex:1;overflow:hidden;margin-right:0.5rem;">
          <div class="text-xs text-bold" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">📄 ${escapeHtml(r.fileName)}</div>
          <div class="text-xs text-muted">${Math.round(r.fileSize / 1024)} KB &bull; Uploaded ${formatDate(r.uploadedAt)}</div>
        </div>
        <a href="${API_BASE.replace('/api', '')}/uploads/${escapeHtml(r.storagePath)}" target="_blank" class="btn btn-outline btn-sm" style="padding:0.2rem 0.5rem;font-size:0.75rem;">Download</a>
      </div>
    `).join("");
  } catch (err) {
    console.error("Failed to load resumes:", err);
  }
}
