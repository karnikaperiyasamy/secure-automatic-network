/* =====================================================
   SANTMS — Shared Utilities & API Service
   ===================================================== */

const API_BASE = 'http://localhost:8080';

/* -------- Theme (applies saved dark/light preference on EVERY page) -------- */
// Previously only login.html applied localStorage('theme'); every other page
// ignored it, so the setting appeared to "reset" the moment you navigated.
(function applySavedTheme() {
  const saved = localStorage.getItem('theme') || 'dark';
  const apply = () => { if (saved === 'light') document.body.classList.add('light-theme'); };
  if (document.body) apply();
  else document.addEventListener('DOMContentLoaded', apply);
})();

/* -------- Auth helpers -------- */
const Auth = {
  getToken: () => localStorage.getItem('santms_token'),
  getUser:  () => { try { return JSON.parse(localStorage.getItem('santms_user')||'{}'); } catch { return {}; } },
  getOrgId: () => localStorage.getItem('santms_org') || 1,
  isLoggedIn: () => !!localStorage.getItem('santms_token'),
  hasRole: (role) => {
    const u = Auth.getUser();
    return u.roles && u.roles.includes(role);
  },
  isSuperAdmin:    () => Auth.hasRole('ROLE_SUPER_ADMIN'),
  isNetworkAdmin:  () => Auth.hasRole('ROLE_NETWORK_ADMIN') || Auth.isSuperAdmin(),
  isSecAnalyst:    () => Auth.hasRole('ROLE_SECURITY_ANALYST') || Auth.isNetworkAdmin(),
  logout: () => {
    localStorage.clear();
    window.location.href = '/login.html';
  },
  requireAuth: () => {
    if (!Auth.isLoggedIn()) { window.location.href = '/login.html'; return false; }
    return true;
  }
};

/* -------- API client -------- */
const API = {
  headers: () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${Auth.getToken()}`
  }),

  async request(method, path, body = null) {
    const opts = { method, headers: API.headers() };
    if (body) opts.body = JSON.stringify(body);
    try {
      const res = await fetch(`${API_BASE}${path}`, opts);
      if (res.status === 401) { Auth.logout(); return null; }
      if (res.status === 204) return null;
      const ct = res.headers.get('content-type') || '';
      const data = ct.includes('application/json') ? await res.json() : await res.text();
      if (!res.ok) {
        // Surface field-level validation errors (e.g. password rules) instead
        // of the generic "Validation failed" message the backend sends.
        if (data && data.errors && typeof data.errors === 'object') {
          const detail = Object.values(data.errors).join(', ');
          throw new Error(detail || data?.message || `HTTP ${res.status}`);
        }
        throw new Error(data?.message || data || `HTTP ${res.status}`);
      }
      return data;
    } catch (e) {
      console.error(`API ${method} ${path}:`, e);
      throw e;
    }
  },

  get:    (path)        => API.request('GET',    path),
  post:   (path, body)  => API.request('POST',   path, body),
  put:    (path, body)  => API.request('PUT',    path, body),
  patch:  (path, body)  => API.request('PATCH',  path, body),
  delete: (path)        => API.request('DELETE', path),

  // Convenience endpoints
  dashboard: (orgId) => API.get(`/api/dashboard?orgId=${orgId}`),
  devices: {
    list:   (orgId,p=0,s=20) => API.get(`/api/devices?orgId=${orgId}&page=${p}&size=${s}`),
    search: (orgId,q,p=0)    => API.get(`/api/devices/search?orgId=${orgId}&q=${encodeURIComponent(q)}&page=${p}`),
    get:    (id)              => API.get(`/api/devices/${id}`),
    create: (orgId,data)      => API.post(`/api/devices?orgId=${orgId}`, data),
    update: (id,data)         => API.put(`/api/devices/${id}`, data),
    delete: (id)              => API.delete(`/api/devices/${id}`),
    stats:  (orgId)           => API.get(`/api/devices/stats?orgId=${orgId}`),
    maintenance: (id)         => API.patch(`/api/devices/${id}/maintenance`),
  },
  alerts: {
    list:  (orgId,p=0,s=20)  => API.get(`/api/alerts?orgId=${orgId}&page=${p}&size=${s}`),
    open:  (orgId)            => API.get(`/api/alerts/open?orgId=${orgId}`),
    count: (orgId)            => API.get(`/api/alerts/count?orgId=${orgId}`),
    ack:   (id)               => API.patch(`/api/alerts/${id}/acknowledge`),
    resolve:(id,note='')      => API.patch(`/api/alerts/${id}/resolve?note=${encodeURIComponent(note)}`),
  },
  topology: {
    get:        (orgId)    => API.get(`/api/topology?orgId=${orgId}`),
    regenerate: (orgId)    => API.post(`/api/topology/regenerate?orgId=${orgId}`),
    updatePos:  (id,x,y)   => API.patch(`/api/topology/nodes/${id}/position?x=${x}&y=${y}`),
  },
  scans: {
    start:  (orgId,net)  => API.post(`/api/network/scans/start?orgId=${orgId}&network=${encodeURIComponent(net)}`),
    status: (id)         => API.get(`/api/network/scans/${id}/status`),
    list:   (orgId)      => API.get(`/api/network/scans?orgId=${orgId}`),
  },
  users: {
    list:   (orgId)      => API.get(`/api/users?orgId=${orgId}`),
    me:     ()           => API.get('/api/users/me'),
    create: (orgId,data) => API.post(`/api/users?orgId=${orgId}`, data),
    delete: (id)         => API.delete(`/api/users/${id}`),
  },
  auditLogs: (orgId,p=0) => API.get(`/api/audit-logs?orgId=${orgId}&page=${p}&size=50`),
  ip: {
    list:      (orgId)        => API.get(`/api/ip?orgId=${orgId}`),
    add:       (orgId,data)   => API.post(`/api/ip?orgId=${orgId}`, data),
    delete:    (id)           => API.delete(`/api/ip/${id}`),
    reserve:   (id)           => API.patch(`/api/ip/${id}/reserve`),
    conflicts: (orgId)        => API.get(`/api/ip/conflicts?orgId=${orgId}`),
  },
  security: {
    runScan: (orgId) => API.post(`/api/security/scan?orgId=${orgId}`),
    scans:   (orgId) => API.get(`/api/security/scans?orgId=${orgId}`),
  },
  reports: {
    list: (orgId) => API.get(`/api/reports?orgId=${orgId}`),
    // Binary download — bypasses API.request() since that always parses
    // the body as JSON/text. Returns {blob, filename}.
    async generate(orgId, type, period, format) {
      const url = `${API_BASE}/api/reports/generate?orgId=${orgId}&type=${type}&period=${period}&format=${format}`;
      const res = await fetch(url, { method: 'POST', headers: API.headers() });
      if (res.status === 401) { Auth.logout(); throw new Error('Session expired'); }
      if (!res.ok) {
        let msg = `HTTP ${res.status}`;
        try { const err = await res.json(); msg = err?.message || msg; } catch {}
        throw new Error(msg);
      }
      const disposition = res.headers.get('content-disposition') || '';
      const match = disposition.match(/filename="?([^"]+)"?/);
      const filename = match ? match[1] : `report.${format.toLowerCase()}`;
      const blob = await res.blob();
      return { blob, filename };
    }
  },
};

/* -------- File download helper (used by Reports / Security export) -------- */
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

function downloadCSV(filename, headers, rows) {
  const csv = [headers, ...rows]
    .map(r => r.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\n');
  downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8;' }), filename);
}

/* -------- Toast -------- */
const Toast = {
  container: null,
  init() {
    if (!this.container) {
      this.container = document.createElement('div');
      this.container.className = 'toast-container';
      document.body.appendChild(this.container);
    }
  },
  show(type, title, msg = '') {
    this.init();
    const icons = { success:'fa-check-circle', error:'fa-times-circle', warning:'fa-exclamation-triangle', info:'fa-info-circle' };
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.innerHTML = `
      <i class="fas ${icons[type]||icons.info} toast-icon"></i>
      <div class="toast-body"><strong>${title}</strong>${msg?`<span>${msg}</span>`:''}  </div>
      <i class="fas fa-times" style="margin-left:auto;cursor:pointer;color:var(--text-muted);font-size:0.75rem;" onclick="this.parentElement.remove()"></i>
    `;
    this.container.appendChild(el);
    setTimeout(() => el.remove(), 4200);
  },
  success: (t,m) => Toast.show('success',t,m),
  error:   (t,m) => Toast.show('error',t,m),
  warning: (t,m) => Toast.show('warning',t,m),
  info:    (t,m) => Toast.show('info',t,m),
};

/* -------- Sidebar builder -------- */
function buildSidebar(activePage) {
  const user = Auth.getUser();
  const orgId = Auth.getOrgId();

  const navItems = [
    { section: 'Main' },
    { id:'dashboard',  icon:'fa-tachometer-alt',  label:'Dashboard',       href:'dashboard.html' },
    { id:'topology',   icon:'fa-project-diagram', label:'Network Topology', href:'topology.html' },
    { id:'devices',    icon:'fa-server',           label:'Devices',          href:'devices.html' },
    { section: 'Monitoring' },
    { id:'alerts',     icon:'fa-bell',             label:'Alerts',           href:'alerts.html', badge:'alerts' },
    { id:'monitoring', icon:'fa-chart-line',       label:'Live Monitoring',  href:'monitoring.html' },
    { id:'security',   icon:'fa-shield-alt',       label:'Security',         href:'security.html' },
    { section: 'Network' },
    { id:'scans',      icon:'fa-search-location',  label:'Network Scans',    href:'scans.html' },
    { id:'ipmanager',  icon:'fa-network-wired',    label:'IP Manager',       href:'ipmanager.html' },
    { section: 'Management' },
    { id:'reports',    icon:'fa-file-chart-bar',   label:'Reports',          href:'reports.html' },
    { id:'users',      icon:'fa-users',            label:'Users',            href:'users.html', adminOnly: true },
    { id:'auditlogs',  icon:'fa-history',          label:'Audit Logs',       href:'auditlogs.html' },
    { id:'settings',   icon:'fa-cog',              label:'Settings',         href:'settings.html' },
  ];

  let navHtml = '';
  for (const item of navItems) {
    if (item.section) {
      navHtml += `<div class="nav-section-title">${item.section}</div>`;
      continue;
    }
    if (item.adminOnly && !Auth.isNetworkAdmin()) continue;
    const active = item.id === activePage ? 'active' : '';
    navHtml += `
      <a class="nav-item ${active}" href="${item.href}" title="${item.label}">
        <span class="nav-icon"><i class="fas ${item.icon}"></i></span>
        <span class="nav-label">${item.label}</span>
        ${item.badge === 'alerts' ? '<span class="nav-badge" id="sidebarAlertBadge">0</span>' : ''}
      </a>`;
  }

  const initials = ((user.firstName||'?')[0]+(user.lastName||'?')[0]).toUpperCase();
  const roleLabel = (user.roles&&user.roles[0]||'USER').replace('ROLE_','').replace('_',' ');

  const html = `
    <div class="sidebar" id="sidebar">
      <div class="sidebar-logo">
        <div class="logo-mark"><i class="fas fa-network-wired"></i></div>
        <div class="logo-text"><h2>SANT<span>MS</span></h2><small>Network Management</small></div>
      </div>
      <div class="sidebar-toggle" onclick="toggleSidebar()" id="sidebarToggle"><i class="fas fa-chevron-left" id="toggleIcon"></i></div>
      <nav class="sidebar-nav">${navHtml}</nav>
      <div class="sidebar-user">
        <div class="user-avatar">${initials}</div>
        <div class="user-info"><strong>${user.firstName||user.username||'User'} ${user.lastName||''}</strong><span>${roleLabel}</span></div>
      </div>
    </div>`;

  const container = document.getElementById('sidebar-container');
  if (container) { container.innerHTML = html; loadAlertBadge(orgId); }
}

/* -------- Header builder -------- */
function buildHeader(title) {
  const user = Auth.getUser();
  const initials = ((user.firstName||'U')[0]+(user.lastName||'S')[0]).toUpperCase();
  return `
    <header class="main-header" id="mainHeader">
      <div class="header-search">
        <i class="fas fa-search"></i>
        <input type="text" placeholder="Search devices, IPs, alerts…" id="globalSearch" oninput="globalSearchHandler(this.value)"/>
      </div>
      <div class="header-actions">
        <div class="header-btn" title="Refresh" onclick="location.reload()"><i class="fas fa-sync-alt"></i></div>
        <div class="header-btn" title="Alerts" onclick="window.location='alerts.html'">
          <i class="fas fa-bell"></i>
          <span class="header-badge" id="headerAlertBadge">0</span>
        </div>
        <div class="header-btn" title="Dark Mode" onclick="toggleDark()"><i class="fas fa-moon"></i></div>
        <div class="header-user" onclick="profileMenu()">
          <div class="h-avatar">${initials}</div>
          <span>${user.firstName||user.username||'User'}</span>
          <i class="fas fa-chevron-down" style="font-size:0.65rem;color:var(--text-muted);"></i>
        </div>
        <div class="header-btn" title="Logout" onclick="confirmLogout()"><i class="fas fa-sign-out-alt"></i></div>
      </div>
    </header>`;
}

async function loadAlertBadge(orgId) {
  try {
    const data = await API.alerts.count(orgId);
    const n = data?.open || 0;
    const sb = document.getElementById('sidebarAlertBadge');
    const hb = document.getElementById('headerAlertBadge');
    if (sb) { sb.textContent = n > 99 ? '99+' : n; sb.style.display = n > 0 ? '' : 'none'; }
    if (hb) { hb.textContent = n > 99 ? '99+' : n; hb.style.display = n > 0 ? '' : 'none'; }
  } catch {}
}

function toggleSidebar() {
  const sb = document.getElementById('sidebar');
  const icon = document.getElementById('toggleIcon');
  sb.classList.toggle('collapsed');
  if (sb.classList.contains('collapsed')) {
    icon.className = 'fas fa-chevron-right';
  } else {
    icon.className = 'fas fa-chevron-left';
  }
}

function toggleDark() {
    const body = document.body;

    body.classList.toggle("light-theme");

    const mode = body.classList.contains("light-theme")
        ? "light"
        : "dark";

    localStorage.setItem("theme", mode);

    const icon = document.querySelector(".header-btn .fa-moon, .header-btn .fa-sun");

    if(icon){
        if(mode==="light"){
            icon.classList.remove("fa-moon");
            icon.classList.add("fa-sun");
        }else{
            icon.classList.remove("fa-sun");
            icon.classList.add("fa-moon");
        }
    }

    Toast.success(
        "Theme Changed",
        mode==="light" ? "Light Mode Enabled" : "Dark Mode Enabled"
    );
}

function confirmLogout() {
  if (confirm('Are you sure you want to log out?')) {
    Auth.logout();
  }
}

function profileMenu() {
  const u = Auth.getUser();
  alert(`User: ${u.username}\nEmail: ${u.email}\nOrg: ${u.organizationName||'—'}\nRoles: ${(u.roles||[]).join(', ')}`);
}

function globalSearchHandler(q) {
  if (q.length < 2) return;
  // Debounce search
  clearTimeout(window._searchTimer);
  window._searchTimer = setTimeout(() => {
    window.location.href = `devices.html?q=${encodeURIComponent(q)}`;
  }, 600);
}

/* -------- Helpers -------- */
function initPage(pageId, title) {
  if (!Auth.requireAuth()) return false;
  const hc = document.getElementById('header-container');
  if (hc) hc.innerHTML = buildHeader(title);
  buildSidebar(pageId);
  return true;
}

function fmt(n, dec=1) {
  if (n == null) return '—';
  return Number(n).toFixed(dec);
}

function fmtBytes(bytes) {
  if (!bytes) return '0 B';
  const k=1024, sizes=['B','KB','MB','GB','TB'];
  const i=Math.floor(Math.log(bytes)/Math.log(k));
  return `${(bytes/Math.pow(k,i)).toFixed(1)} ${sizes[i]}`;
}

function timeAgo(dt) {
  if (!dt) return '—';
  const d = new Date(dt), now = new Date();
  const s = Math.floor((now-d)/1000);
  if (s < 60) return `${s}s ago`;
  if (s < 3600) return `${Math.floor(s/60)}m ago`;
  if (s < 86400) return `${Math.floor(s/3600)}h ago`;
  return `${Math.floor(s/86400)}d ago`;
}

function statusBadge(status) {
  const map = {
    ONLINE:'badge-online', OFFLINE:'badge-offline',
    WARNING:'badge-warning', CRITICAL:'badge-critical',
    MAINTENANCE:'badge-maintenance', UNKNOWN:'badge-unknown'
  };
  const dot = {
    ONLINE:'dot-online',OFFLINE:'dot-offline',WARNING:'dot-warning',
    CRITICAL:'dot-critical',MAINTENANCE:'',UNKNOWN:''
  };
  const cls = map[status]||'badge-unknown';
  const dotCls = dot[status]||'';
  return `<span class="badge-status ${cls}"><span class="status-dot-sm ${dotCls}"></span>${status||'UNKNOWN'}</span>`;
}

function deviceTypeIcon(type) {
  const icons = {
    ROUTER:'fa-route',SWITCH:'fa-sitemap',FIREWALL:'fa-fire-alt',
    SERVER:'fa-server',COMPUTER:'fa-desktop',PRINTER:'fa-print',
    ACCESS_POINT:'fa-wifi',IOT_DEVICE:'fa-microchip',
    VIRTUAL_MACHINE:'fa-cloud',SMARTPHONE:'fa-mobile-alt',
    CAMERA:'fa-video',NAS:'fa-hdd',UNKNOWN:'fa-question-circle'
  };
  return icons[type]||'fa-network-wired';
}

function severityBadge(sev) {
  const cls = { CRITICAL:'sev-critical',HIGH:'sev-high',MEDIUM:'sev-medium',LOW:'sev-low',INFO:'sev-info' };
  return `<span class="${cls[sev]||'sev-info'}">${sev||'INFO'}</span>`;
}

function metricColor(val) {
  if (val >= 90) return 'var(--danger)';
  if (val >= 75) return 'var(--warning)';
  if (val >= 50) return 'var(--info)';
  return 'var(--success)';
}

function cpuBar(val) {
  const color = metricColor(val);
  return `<div style="display:flex;align-items:center;gap:8px;">
    <div class="progress-bar-custom" style="flex:1;height:5px;">
      <div class="progress-fill" style="width:${val||0}%;background:${color};"></div>
    </div>
    <span style="font-size:0.75rem;color:${color};width:36px;text-align:right;">${fmt(val)}%</span>
  </div>`;
}

function showModal(id)  { document.getElementById(id)?.classList.add('open'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('open'); }
