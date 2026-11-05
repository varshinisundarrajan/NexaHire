/**
 * NexaHire - User Management & System Audit Logic
 */

let cachedUsers = [];

document.addEventListener("DOMContentLoaded", () => {
  renderAppNavigation("users");
  loadUsers();

  document.getElementById("edit-user-form").addEventListener("submit", handleEditUserSubmit);
});

function switchAdminTab(tab) {
  const tabUsers = document.getElementById("tab-users");
  const tabAudit = document.getElementById("tab-audit");
  const sectionUsers = document.getElementById("users-section");
  const sectionAudit = document.getElementById("audit-section");

  if (tab === "users") {
    tabUsers.classList.add("active");
    tabAudit.classList.remove("active");
    sectionUsers.style.display = "block";
    sectionAudit.style.display = "none";
    loadUsers();
  } else {
    tabAudit.classList.add("active");
    tabUsers.classList.remove("active");
    sectionUsers.style.display = "none";
    sectionAudit.style.display = "block";
    loadAuditLogs();
  }
}

async function loadUsers() {
  const tbody = document.getElementById("users-table-body");
  tbody.innerHTML = `<tr><td colspan="6" class="text-center p-6 text-muted">Loading user accounts...</td></tr>`;

  try {
    const res = await apiCall("/users?size=50");
    cachedUsers = (res.data && res.data.content) ? res.data.content : [];

    if (cachedUsers.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center p-6 text-muted">No users found.</td></tr>`;
      return;
    }

    tbody.innerHTML = cachedUsers.map(user => `
      <tr>
        <td>
          <div class="text-bold">${escapeHtml(user.fullName)}</div>
          <div class="text-xs text-muted">@${escapeHtml(user.username)}</div>
        </td>
        <td>${escapeHtml(user.email)}</td>
        <td>
          <span class="badge badge-primary">${escapeHtml(user.role?.name?.replace("ROLE_", "") || "")}</span>
        </td>
        <td>${getStatusBadge(user.status)}</td>
        <td class="text-sm text-muted">${formatDate(user.createdAt)}</td>
        <td>
          <div class="flex gap-2">
            <button class="btn btn-secondary btn-sm" onclick="openEditUserModal(${user.id})">Edit</button>
            <button class="btn btn-outline btn-sm" onclick="toggleUserStatus(${user.id}, '${user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'}')">
              ${user.status === 'ACTIVE' ? 'Suspend' : 'Activate'}
            </button>
          </div>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center p-6 text-danger">${escapeHtml(err.message)}</td></tr>`;
  }
}

async function loadAuditLogs() {
  const tbody = document.getElementById("audit-table-body");
  tbody.innerHTML = `<tr><td colspan="6" class="text-center p-6 text-muted">Loading immutable system audit logs...</td></tr>`;

  try {
    const res = await apiCall("/users/audit-logs?size=50");
    const logs = (res.data && res.data.content) ? res.data.content : [];

    if (logs.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center p-6 text-muted">No audit events recorded yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = logs.map(l => `
      <tr>
        <td class="text-xs text-muted" style="white-space:nowrap;">${formatDate(l.timestamp)}</td>
        <td><span class="badge badge-neutral text-xs text-bold">${escapeHtml(l.action)}</span></td>
        <td class="text-xs">${escapeHtml(l.entityType)} #${l.entityId || ""}</td>
        <td class="text-sm text-bold">${escapeHtml(l.user?.fullName || "System/Anonymous")}</td>
        <td class="text-xs text-muted">${escapeHtml(l.ipAddress || "127.0.0.1")}</td>
        <td class="text-xs" style="max-width:300px;word-break:break-word;">${escapeHtml(l.details || "")}</td>
      </tr>
    `).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center p-6 text-danger">${escapeHtml(err.message)}</td></tr>`;
  }
}

function openEditUserModal(userId) {
  const user = cachedUsers.find(u => u.id === userId);
  if (!user) return;

  document.getElementById("edit-user-id").value = user.id;
  document.getElementById("edit-full-name").value = user.fullName || "";
  document.getElementById("edit-phone").value = user.phone || "";
  document.getElementById("edit-role").value = user.role?.name || "ROLE_CANDIDATE";
  document.getElementById("edit-status").value = user.status || "ACTIVE";

  openModal("edit-user-modal");
}

async function handleEditUserSubmit(e) {
  e.preventDefault();
  const userId = document.getElementById("edit-user-id").value;
  const payload = {
    fullName: document.getElementById("edit-full-name").value.trim(),
    phone: document.getElementById("edit-phone").value.trim(),
    roleName: document.getElementById("edit-role").value,
    status: document.getElementById("edit-status").value
  };

  try {
    await apiCall(`/users/${userId}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
    showToast("User role and permissions updated successfully!", "success");
    closeModal("edit-user-modal");
    loadUsers();
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function toggleUserStatus(userId, newStatus) {
  try {
    await apiCall(`/users/${userId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: newStatus })
    });
    showToast(`User status changed to ${newStatus}`, "info");
    loadUsers();
  } catch (err) {
    showToast(err.message, "error");
  }
}
