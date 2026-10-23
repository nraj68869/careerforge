import { useEffect, useState } from "react";
import { applicationsApi } from "../api/applications";
import AppShell from "../components/AppShell";
import ApplicationTracker from "../components/ApplicationTracker";
import ConfirmDialog from "../components/ConfirmDialog";
import StatusBadge from "../components/StatusBadge";
import { STATUS_OPTIONS, formatDate, formatStatus } from "../utils/format";

const PAGE_SIZE = 8;

export default function MyApplicationsPage() {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [confirmState, setConfirmState] = useState(null);
  const [workingId, setWorkingId] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    applicationsApi.mine({ status, page, size: PAGE_SIZE })
      .then((result) => { if (!cancelled) { setData(result); setError(""); } })
      .catch((err) => { if (!cancelled) setError(err.message || "Your applications could not be loaded."); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [status, page, reloadKey]);

  const items = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  function requestWithdraw(app) {
    setConfirmState({
      title: "Withdraw this application?",
      message: `Your application to "${app.jobTitle}" will be withdrawn. You can apply again later if the job is still open.`,
      confirmLabel: "Withdraw",
      danger: true,
      onConfirm: async () => {
        setError("");
        setNotice("");
        setWorkingId(app.id);
        try {
          await applicationsApi.withdraw(app.id);
          setNotice("Application withdrawn.");
          setReloadKey((k) => k + 1);
        } catch (err) {
          setError(err.message || "The application could not be withdrawn.");
        } finally {
          setWorkingId(null);
        }
      },
    });
  }

  return (
    <AppShell title="My applications">
      <section className="welcome-row">
        <div>
          <div className="eyebrow">YOUR PROGRESS</div>
          <h1>My applications</h1>
          <p className="page-subtitle">Track every job you have applied to.</p>
        </div>
        <a className="button button-primary" href="#/jobs">Browse jobs</a>
      </section>

      {notice && (
        <div className="alert alert-success" role="status">
          <span className="alert-symbol">✓</span><span>{notice}</span>
          <button onClick={() => setNotice("")} aria-label="Dismiss">×</button>
        </div>
      )}
      {error && (
        <div className="alert alert-error" role="alert">
          <span className="alert-symbol">!</span><span>{error}</span>
          <button onClick={() => setError("")} aria-label="Dismiss">×</button>
        </div>
      )}

      <div className="filter-bar">
        <select aria-label="Filter by status" value={status}
                onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
          <option value="">All statuses</option>
          {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{formatStatus(s)}</option>)}
        </select>
        {status && <button className="text-button" onClick={() => { setStatus(""); setPage(0); }}>Clear filter</button>}
      </div>

      {loading && !data ? (
        <div className="table-state"><span className="spinner" /><span>Loading your applications…</span></div>
      ) : items.length === 0 ? (
        <div className="empty-state panel-card">
          <div className="empty-icon">☑</div>
          <strong>{status ? "No applications with this status" : "You haven't applied to any jobs yet"}</strong>
          <p>{status ? "Try another status." : "Find an opening and apply. Your progress shows up here."}</p>
          {!status && <a className="button button-primary" href="#/jobs">Browse jobs</a>}
        </div>
      ) : (
        <div className={`app-list${loading ? " is-loading" : ""}`}>
          {items.map((a) => (
            <article className="app-card" key={a.id}>
              <div className="app-card-head">
                <span className="company-logo">{a.company.charAt(0).toUpperCase()}</span>
                <div className="grow">
                  <h3 className="job-title">{a.jobTitle}</h3>
                  <p className="job-company">{a.company}</p>
                </div>
                <StatusBadge status={a.status} />
              </div>

              <ApplicationTracker status={a.status} />

              <div className="app-card-foot">
                <span className="app-card-meta">
                  Applied {formatDate(a.appliedAt.slice(0, 10))} · Updated {formatDate(a.updatedAt.slice(0, 10))}
                  {a.jobStatus === "CLOSED" && <span className="chip">Job closed</span>}
                </span>
                {a.canWithdraw && (
                  <button className="delete-button" onClick={() => requestWithdraw(a)} disabled={workingId === a.id}>
                    Withdraw
                  </button>
                )}
              </div>

              {a.coverNote && (
                <details className="note">
                  <summary>My cover note</summary>
                  <p>{a.coverNote}</p>
                </details>
              )}
            </article>
          ))}
        </div>
      )}

      {total > 0 && (
        <footer className="job-pager">
          <span><strong>{total}</strong> application{total === 1 ? "" : "s"}</span>
          <div className="pagination">
            <button className="page-button" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0 || loading}>← <span>Previous</span></button>
            <span className="page-number">{page + 1} <span>of</span> {totalPages}</span>
            <button className="page-button" onClick={() => setPage((p) => p + 1)} disabled={loading || page >= totalPages - 1}><span>Next</span> →</button>
          </div>
        </footer>
      )}

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
    </AppShell>
  );
}