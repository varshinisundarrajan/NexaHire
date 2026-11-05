/**
 * NexaHire Enterprise - Core API Client & Utilities
 */

const API_BASE = window.location.port === "8080" 
  ? `${window.location.origin}/api` 
  : "http://localhost:8080/api";

const TOKEN_KEY = "nexahire_jwt_token";
const USER_KEY = "nexahire_user";

// Authentication helpers
function setAuth(authData) {
  localStorage.setItem(TOKEN_KEY, authData.token);
  localStorage.setItem(USER_KEY, JSON.stringify({
    id: authData.id,
    username: authData.username,
    email: authData.email,
    fullName: authData.fullName,
    role: authData.role
  }));
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function getUser() {
  const userStr = localStorage.getItem(USER_KEY);
  return userStr ? JSON.parse(userStr) : null;
}

function isAuthenticated() {
  return !!getToken();
}

function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

function hasRole(role) {
  const user = getUser();
  if (!user || !user.role) return false;
  return user.role === role || user.role === `ROLE_${role}`;
}

function hasAnyRole(...roles) {
  return roles.some(role => hasRole(role));
}

// Unified API Caller
async function apiCall(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint.startsWith("/") ? endpoint : "/" + endpoint}`;
  const headers = options.headers || {};

  const token = getToken();
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  if (!(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers
    });

    const data = await response.json().catch(() => null);

    if (response.status === 401) {
      clearAuth();
      showToast("Session expired. Please log in again.", "warning");
      setTimeout(() => {
        if (!window.location.pathname.endsWith("login.html")) {
          window.location.href = "login.html";
        }
      }, 1200);
      throw new Error("Unauthorized");
    }

    if (!response.ok) {
      const errMsg = (data && data.message) ? data.message : `Request failed with status ${response.status}`;
      throw new Error(errMsg);
    }

    return data;
  } catch (err) {
    console.error(`API Error [${endpoint}]:`, err);
    throw err;
  }
}

// Toast System
function showToast(message, type = "success") {
  let container = document.getElementById("toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "toast-container";
    container.className = "toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span>${escapeHtml(message)}</span>
    <button onclick="this.parentElement.remove()" style="background:none;border:none;cursor:pointer;color:#94a3b8;font-size:16px;">&times;</button>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.remove();
  }, 4000);
}

// Modal Helpers
function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    modal.classList.add("show");
  }
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    modal.classList.remove("show");
  }
}

// Formatting Utilities
function formatCurrency(amount) {
  if (amount == null) return "Negotiable";
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(amount);
}

function formatDate(dateStr) {
  if (!dateStr) return "N/A";
  const d = new Date(dateStr);
  return d.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
}

function getStatusBadge(status) {
  const s = (status || "").toUpperCase();
  switch (s) {
    case "ACTIVE":
    case "SELECTED":
    case "COMPLETED":
      return `<span class="badge badge-success">${s}</span>`;
    case "APPLIED":
    case "SCHEDULED":
      return `<span class="badge badge-primary">${s}</span>`;
    case "SCREENING":
    case "SHORTLISTED":
      return `<span class="badge badge-warning">${s}</span>`;
    case "INTERVIEW":
      return `<span class="badge badge-info">${s}</span>`;
    case "REJECTED":
    case "CLOSED":
    case "SUSPENDED":
      return `<span class="badge badge-danger">${s}</span>`;
    default:
      return `<span class="badge badge-neutral">${s}</span>`;
  }
}

function getMatchScoreBadge(score) {
  if (score == null) return "";
  let cls = "score-low";
  if (score >= 75) cls = "score-high";
  else if (score >= 50) cls = "score-medium";

  return `<span class="match-score-badge ${cls}">
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
    ${score}% Match
  </span>`;
}

function escapeHtml(str) {
  if (!str) return "";
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

// Dynamic Navigation & Role-Based Sidebar renderer
function renderAppNavigation(activePage) {
  const user = getUser();
  if (!user && !["index.html", "login.html", "register.html"].some(p => window.location.pathname.endsWith(p))) {
    window.location.href = "login.html";
    return;
  }

  const role = user ? user.role : "";
  const isCandidate = role === "ROLE_CANDIDATE";
  const isRecruiter = role === "ROLE_RECRUITER";
  const isHR = role === "ROLE_HR_MANAGER";
  const isAdmin = role === "ROLE_ADMIN";
  const isStaff = isRecruiter || isHR || isAdmin;

  const sidebarNav = document.getElementById("sidebar-nav");
  if (sidebarNav) {
    let navHtml = `
      <div class="nav-section-title">Overview</div>
      <a href="dashboard.html" class="nav-item ${activePage === 'dashboard' ? 'active' : ''}">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
        Dashboard
      </a>
      <a href="jobs.html" class="nav-item ${activePage === 'jobs' ? 'active' : ''}">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
        ${isCandidate ? 'Browse Jobs' : 'Job Openings'}
      </a>
    `;

    if (isCandidate) {
      navHtml += `
        <div class="nav-section-title">My Career</div>
        <a href="applications.html" class="nav-item ${activePage === 'applications' ? 'active' : ''}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          My Applications
        </a>
        <a href="interviews.html" class="nav-item ${activePage === 'interviews' ? 'active' : ''}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          My Interviews
        </a>
        <a href="profile.html" class="nav-item ${activePage === 'profile' ? 'active' : ''}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          Profile & Resume
        </a>
      `;
    }

    if (isStaff) {
      navHtml += `
        <div class="nav-section-title">Talent Pipeline</div>
        <a href="applications.html" class="nav-item ${activePage === 'applications' ? 'active' : ''}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
          ATS Pipeline
        </a>
        <a href="candidates.html" class="nav-item ${activePage === 'candidates' ? 'active' : ''}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          Candidate Sourcing
        </a>
        <a href="interviews.html" class="nav-item ${activePage === 'interviews' ? 'active' : ''}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          Interviews & Feedback
        </a>
        <a href="analytics.html" class="nav-item ${activePage === 'analytics' ? 'active' : ''}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
          Funnel Analytics
        </a>
      `;
    }

    if (isAdmin || isHR) {
      navHtml += `
        <div class="nav-section-title">Administration</div>
        <a href="users.html" class="nav-item ${activePage === 'users' ? 'active' : ''}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          User Management & Audit
        </a>
      `;
    }

    sidebarNav.innerHTML = navHtml;
  }

  // Populate user profile info in sidebar footer & topbar
  const userInitials = document.getElementById("user-initials");
  const userFullName = document.getElementById("user-full-name");
  const userRoleBadge = document.getElementById("user-role-badge");

  if (user) {
    const initials = user.fullName
      ? user.fullName.split(" ").map(n => n[0]).join("").toUpperCase().substring(0, 2)
      : user.username.substring(0, 2).toUpperCase();

    if (userInitials) userInitials.textContent = initials;
    if (userFullName) userFullName.textContent = user.fullName || user.username;
    if (userRoleBadge) {
      userRoleBadge.textContent = user.role.replace("ROLE_", "");
    }
  }
}

function handleLogout() {
  clearAuth();
  showToast("Logged out successfully", "info");
  setTimeout(() => {
    window.location.href = "login.html";
  }, 500);
}
