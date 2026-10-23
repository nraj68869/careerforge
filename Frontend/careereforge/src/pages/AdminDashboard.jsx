import { useEffect, useState } from "react";
import { useRecoilState } from "recoil";
import { usersApi } from "../api/user";
import ConfirmDialog from "../components/ConfirmDialog";
import Icon from "../components/Icon";
import PasswordInput from "../components/PasswordInput";
import Sidebar from "../components/Sidebar";
import { currentUserState } from "../state/authState";
import PendingRecruiters from "../components/PendingRecruiters";
import {
  noticeState,
  pageState,
  reloadKeyState,
  searchInputState,
  searchState,
} from "../state/userState";
import { useUsers } from "../state/useUsers";
import "../App.css";

// ADMIN is deliberately missing: the platform has exactly one administrator
const ASSIGNABLE_ROLES = ["STUDENT", "RECRUITER", "PLACEMENT_OFFICER"];
const initialForm = { name: "", email: "", password: "", role: "STUDENT" };

const DIST = [
  { key: "students", label: "Students", cls: "student" },
  { key: "recruiters", label: "Recruiters", cls: "recruiter" },
  { key: "placementOfficers", label: "Placement officers", cls: "officer" },
  { key: "admins", label: "Admins", cls: "admin" },
];

function initials(name = "") {
  return name.trim().split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? "").join("");
}

function formatRole(role = "STUDENT") {
  return role.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (c) => c.toUpperCase());
}

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

function AdminDashboard({ onOpenAccount }) {
  const [currentUser, setCurrentUser] = useRecoilState(currentUserState);
  const [searchInput, setSearchInput] = useRecoilState(searchInputState);
  const [search, setSearch] = useRecoilState(searchState);
  const [page, setPage] = useRecoilState(pageState);
  const [notice, setNotice] = useRecoilState(noticeState);
  const [reloadKey, setReloadKey] = useRecoilState(reloadKeyState);
  const { users, pageInfo, loading, error: loadError } = useUsers();

  const [stats, setStats] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [workingId, setWorkingId] = useState(null);
  const [actionError, setActionError] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [form, setForm] = useState(initialForm);
  const [confirmState, setConfirmState] = useState(null);

  const error = actionError || loadError;
  const reload = () => setReloadKey((key) => key + 1);

  // Debounce: copy searchInput -> search 300ms after the user stops typing
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(0);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [searchInput, setSearch, setPage]);

  // Statistics: reload whenever the reload counter changes
  useEffect(() => {
    let cancelled = false;
    usersApi.stats()
      .then((data) => { if (!cancelled) setStats(data); })
      .catch(() => { /* the cards just show "—" */ });
    return () => { cancelled = true; };
  }, [reloadKey]);

  // ---------- form ----------
  function openCreate() {
    setEditingUser(null);
    setForm(initialForm);
    setActionError("");
    setNotice("");
    setShowForm(true);
  }

  function openEdit(user) {
    setEditingUser(user);
    setForm({ ...initialForm, name: user.name ?? "", email: user.email ?? "" });
    setActionError("");
    setNotice("");
    setShowForm(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function closeForm() {
    setShowForm(false);
    setEditingUser(null);
    setForm(initialForm);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setActionError("");
    setNotice("");
    setSubmitting(true);
    try {
      if (editingUser) {
        const updated = await usersApi.update(editingUser.id, {
          name: form.name.trim(),
          email: form.email.trim(),
        });
        if (updated.id === currentUser?.id) setCurrentUser(updated);   // keep the sidebar name in sync
        setNotice(`${updated.name} was updated.`);
      } else {
        const created = await usersApi.create({
          name: form.name.trim(),
          email: form.email.trim(),
          password: form.password,
          role: form.role,
        });
        setNotice(`${created.name} was created as ${formatRole(created.role)}.`);
        setSearchInput("");
        setSearch("");
        setPage(0);
      }
      closeForm();
      reload();
    } catch (err) {
      setActionError(err.message || "The user could not be saved.");
    } finally {
      setSubmitting(false);
    }
  }

  // ---------- row actions (all go through a confirmation pop-up) ----------
  async function runAction(userId, action, successText, failText) {
    setActionError("");
    setNotice("");
    setWorkingId(userId);
    try {
      await action();
      setNotice(successText);
      return true;
    } catch (err) {
      setActionError(err.message || failText);
      return false;
    } finally {
      setWorkingId(null);
    }
  }

  function requestRoleChange(user, role) {
    if (role === user.role) return;
    setConfirmState({
      title: "Change role?",
      message: `${user.name} will become ${formatRole(role)}. Their permissions change immediately.`,
      confirmLabel: "Change role",
      onConfirm: async () => {
        const ok = await runAction(user.id, () => usersApi.changeRole(user.id, role),
          `${user.name} is now ${formatRole(role)}.`, "The role could not be changed.");
        if (ok) reload();
      },
    });
  }

  function requestToggle(user) {
    const enable = user.enabled === false;
    setConfirmState({
      title: enable ? "Enable this account?" : "Disable this account?",
      message: enable
        ? `${user.name} will be able to log in again.`
        : `${user.name} will be logged out immediately and cannot log in until you enable the account.`,
      confirmLabel: enable ? "Enable" : "Disable",
      danger: !enable,
      onConfirm: async () => {
        const ok = await runAction(user.id, () => usersApi.setEnabled(user.id, enable),
          `${user.name} was ${enable ? "enabled" : "disabled"}.`, "The account status could not be changed.");
        if (ok) reload();
      },
    });
  }

  function requestDelete(user) {
    setConfirmState({
      title: `Remove ${user.name}?`,
      message: "This permanently deletes the account. This action cannot be undone.",
      confirmLabel: "Remove user",
      danger: true,
      onConfirm: async () => {
        const ok = await runAction(user.id, () => usersApi.remove(user.id),
          `${user.name} was removed.`, "The user could not be removed.");
        if (ok) {
          if (users.length === 1 && page > 0) setPage(page - 1);
          reload();
        }
      },
    });
  }

  // ---------- display values ----------
  const startRecord = pageInfo.totalElements === 0 ? 0 : page * pageInfo.size + 1;
  const endRecord = Math.min((page + 1) * pageInfo.size, pageInfo.totalElements);
  const firstName = currentUser?.name?.split(" ")[0] ?? "";
  const today = new Date().toLocaleDateString("en-IN", {
    weekday: "long", day: "numeric", month: "long", year: "numeric",
  });
  const staff = stats ? stats.recruiters + stats.placementOfficers : null;

  const cards = [
    { label: "TOTAL USERS", value: stats?.total, foot: "All accounts", icon: "user", tone: "indigo" },
    { label: "STUDENTS", value: stats?.students, foot: "Looking for placements", icon: "check", tone: "teal" },
    { label: "HIRING STAFF", value: staff, foot: "Recruiters and officers", icon: "briefcase", tone: "amber" },
    { label: "DISABLED", value: stats?.disabled, foot: "Cannot log in", icon: "lock", tone: "rose" },
  ];

  return (
    <div className="app-shell">
      <Sidebar />

      <main className="main-content" id="overview">
        <header className="topbar">
          <div className="breadcrumbs"><span>Workspace</span><span className="crumb-slash">/</span><strong>Overview</strong></div>
          <button className="user-chip" onClick={onOpenAccount} title="My account">
            <span className="chip-avatar">{currentUser?.name?.charAt(0).toUpperCase()}</span>
            <span className="chip-copy"><strong>{currentUser?.name}</strong><span>{formatRole(currentUser?.role)}</span></span>
          </button>
        </header>

        <div className="page-content">
          <section className="welcome-row">
            <div>
              <div className="eyebrow">{today.toUpperCase()}</div>
              <h1>{greeting()}, {firstName}.</h1>
              <p className="page-subtitle">Here is what is happening across your placement platform.</p>
            </div>
            <button className="button button-primary" onClick={openCreate}>
              <span className="button-plus">+</span> Add user
            </button>
          </section>

          {notice && (
            <div className="alert alert-success" role="status">
              <span className="alert-symbol">✓</span><span>{notice}</span>
              <button onClick={() => setNotice("")} aria-label="Dismiss notification">×</button>
            </div>
          )}
          {error && (
            <div className="alert alert-error" role="alert">
              <span className="alert-symbol">!</span><span>{error}</span>
              <button onClick={() => setActionError("")} aria-label="Dismiss error">×</button>
            </div>
          )}
          <PendingRecruiters reloadKey={reloadKey} onChanged={reload} />

          {showForm && (
            <section className="form-panel">
              <div className="panel-heading">
                <div>
                  <h2>{editingUser ? "Edit user" : "Add a user"}</h2>
                  <p>{editingUser ? "Update this person's name and email." : "Create an account with a role. Share the temporary password securely."}</p>
                </div>
                <button className="icon-button" onClick={closeForm} aria-label="Close form">×</button>
              </div>
              <form className="create-form wide" onSubmit={handleSubmit}>
                <label>Full name
                  <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} maxLength={100} required />
                </label>
                <label>Email address
                  <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} maxLength={254} required />
                </label>
                {!editingUser && (
                  <>
                    <label>Temporary password
                      <PasswordInput value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })}
                                     minLength={8} autoComplete="new-password" />
                    </label>
                    <label>Role
                      <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                        {ASSIGNABLE_ROLES.map((r) => <option key={r} value={r}>{formatRole(r)}</option>)}
                      </select>
                    </label>
                  </>
                )}
                <div className="form-actions">
                  <button type="button" className="button button-secondary" onClick={closeForm}>Cancel</button>
                  <button className="button button-primary" disabled={submitting}>
                    {submitting ? <><span className="spinner small" /> Saving…</> : editingUser ? "Save changes" : "Create user"}
                  </button>
                </div>
              </form>
            </section>
          )}

          <section className="stats-grid four" aria-label="Statistics">
            {cards.map((c) => (
              <article className="stat-card" key={c.label}>
                <div className="stat-top">
                  <span className={`stat-icon ${c.tone}`}><Icon name={c.icon} size={17} /></span>
                  <span className="stat-label">{c.label}</span>
                </div>
                <div className="stat-value">{c.value ?? "—"}</div>
                <div className="stat-foot">{c.foot}</div>
              </article>
            ))}
          </section>

          <section className="panel-card">
            <div className="panel-card-head">
              <h2>Role distribution</h2>
              <span>{stats ? `${stats.total} users` : ""}</span>
            </div>
            <div className="dist-bar" aria-hidden="true">
              {stats && stats.total > 0 && DIST.map((d) =>
                stats[d.key] > 0 && (
                  <span key={d.key} className={`dist-seg ${d.cls}`}
                        style={{ width: `${(stats[d.key] / stats.total) * 100}%` }} />
                ))}
            </div>
            <ul className="dist-legend">
              {DIST.map((d) => (
                <li key={d.key}><i className={`dist-dot ${d.cls}`} /> {d.label} <strong>{stats ? stats[d.key] : "—"}</strong></li>
              ))}
            </ul>
          </section>

          <section className="directory-panel" id="users">
            <div className="directory-heading">
              <div>
                <div className="heading-with-count"><h2>User directory</h2><span className="count-pill">{pageInfo.totalElements}</span></div>
                <p>Search, edit, change roles and manage access.</p>
              </div>
              <div className="directory-tools">
                <label className="search-box">
                  <span>⌕</span>
                  <input aria-label="Search users by name" value={searchInput}
                         onChange={(e) => setSearchInput(e.target.value)} placeholder="Search by name…" />
                </label>
                <button className="button button-secondary refresh-button" onClick={reload} disabled={loading}>
                  <span className={loading ? "refresh-symbol spinning" : "refresh-symbol"}>↻</span> Refresh
                </button>
              </div>
            </div>

            <div className="table-wrap">
              <table className="student-table">
                <thead>
                  <tr><th>USER</th><th>ROLE</th><th>STATUS</th><th className="actions-heading">ACTIONS</th></tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr><td colSpan="4"><div className="table-state"><span className="spinner" /><span>Loading users…</span></div></td></tr>
                  ) : users.length === 0 ? (
                    <tr><td colSpan="4">
                      <div className="empty-state">
                        <div className="empty-icon">♙</div>
                        <strong>{search ? "No matching users" : "No users yet"}</strong>
                        <p>{search ? "Try another name or clear your search." : "Add the first user to start building the directory."}</p>
                        {search
                          ? <button className="text-button" onClick={() => setSearchInput("")}>Clear search</button>
                          : <button className="button button-primary" onClick={openCreate}>Add user</button>}
                      </div>
                    </td></tr>
                  ) : users.map((user) => {
                    const isMe = user.id === currentUser?.id;
                    const busy = workingId === user.id;
                    const disabled = user.enabled === false;
                    return (
                      <tr key={user.id} className={disabled ? "row-disabled" : ""}>
                        <td>
                          <div className="student-cell">
                            <div className={`avatar avatar-${(user.role || "student").toLowerCase()}`} aria-hidden="true">{initials(user.name)}</div>
                            <div>
                              <strong>{user.name}{isMe ? " (you)" : ""}</strong>
                              <span>{user.email}</span>
                            </div>
                          </div>
                        </td>
                        <td>
                          <select className="role-select" value={user.role} disabled={isMe || busy || user.role === "ADMIN"}
                                  onChange={(e) => requestRoleChange(user, e.target.value)}
                                  aria-label={`Role for ${user.name}`}>
                            {(user.role === "ADMIN" ? [...ASSIGNABLE_ROLES, "ADMIN"] : ASSIGNABLE_ROLES).map((r) => (
                              <option key={r} value={r}>{formatRole(r)}</option>
                            ))}
                          </select>
                        </td>
                        <td><span className={`status-pill ${disabled ? "off" : "on"}`}>{disabled ? "Disabled" : "Active"}</span></td>
                        <td>
                          <div className="status-cell">
                            <span className={`status-pill ${disabled ? "off" : "on"}`}>{disabled ? "Disabled" : "Active"}</span>
                            {user.role === "RECRUITER" && user.approvalStatus && user.approvalStatus !== "APPROVED" && (
                              <span className="status-pill warn">{user.approvalStatus === "PENDING" ? "Pending approval" : "Rejected"}</span>
                            )}
                          </div>
                        </td>
                        <td className="actions-cell">
                          <div className="row-actions">
                            <button className="text-button" onClick={() => openEdit(user)}>Edit</button>
                            <button className="text-button" onClick={() => requestToggle(user)} disabled={isMe || busy}>
                              {disabled ? "Enable" : "Disable"}
                            </button>
                            <button className="delete-button" onClick={() => requestDelete(user)} disabled={isMe || busy}
                                    aria-label={`Remove ${user.name}`}>
                              {busy ? "…" : "Remove"}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            <footer className="table-footer">
              <span>Showing <strong>{startRecord}–{endRecord}</strong> of <strong>{pageInfo.totalElements}</strong> users</span>
              <div className="pagination">
                <button className="page-button" onClick={() => setPage((c) => Math.max(0, c - 1))} disabled={page === 0 || loading}>← <span>Previous</span></button>
                <span className="page-number">{pageInfo.totalPages === 0 ? 0 : page + 1} <span>of</span> {pageInfo.totalPages}</span>
                <button className="page-button" onClick={() => setPage((c) => c + 1)} disabled={loading || page >= pageInfo.totalPages - 1}><span>Next</span> →</button>
              </div>
            </footer>
          </section>

          <footer className="app-footer">
            <span>© {new Date().getFullYear()} CareerForge</span>
            <span>Secured with signed, revocable sessions</span>
          </footer>
        </div>
      </main>

      {confirmState && (
        <ConfirmDialog
          title={confirmState.title}
          message={confirmState.message}
          confirmLabel={confirmState.confirmLabel}
          danger={confirmState.danger}
          onCancel={() => setConfirmState(null)}
          onConfirm={() => {
            const run = confirmState.onConfirm;
            setConfirmState(null);
            run();
          }}
        />
      )}
    </div>
  );
}

export default AdminDashboard;