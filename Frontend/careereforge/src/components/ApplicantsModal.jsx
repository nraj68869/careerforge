import { useEffect, useState } from "react";
import { applicationsApi } from "../api/applications";
import { STATUS_OPTIONS, formatStatus } from "../utils/format";
import ApplicationsTable from "./ApplicationsTable";

const PAGE_SIZE = 8;

export default function ApplicantsModal({ job, onClose }) {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    applicationsApi.forJob(job.id, { status, page, size: PAGE_SIZE })
      .then((result) => { if (!cancelled) { setData(result); setError(""); } })
      .catch((err) => { if (!cancelled) setError(err.message || "Applicants could not be loaded."); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [job.id, status, page, reloadKey]);

  const items = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card xl" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <div className="job-detail-head">
          <span className="company-logo">{job.company.charAt(0).toUpperCase()}</span>
          <div>
            <h3>Applicants</h3>
            <p className="job-company">{job.title} · {job.company}</p>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Close">×</button>
        </div>

        <div className="filter-bar">
          <select aria-label="Filter by status" value={status}
                  onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{formatStatus(s)}</option>)}
          </select>
          <span className="muted-text">{total} application{total === 1 ? "" : "s"}</span>
        </div>

        {error && <div className="alert alert-error" role="alert"><span>{error}</span></div>}

        {loading && !data ? (
          <div className="table-state"><span className="spinner" /><span>Loading applicants…</span></div>
        ) : items.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">♙</div>
            <strong>{status ? "No applicants with this status" : "No applications yet"}</strong>
            <p>{status ? "Try another status." : "Students' applications will appear here."}</p>
          </div>
        ) : (
          <ApplicationsTable items={items} onChanged={() => setReloadKey((k) => k + 1)} />
        )}

        {total > 0 && (
          <footer className="job-pager">
            <span>Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong></span>
            <div className="pagination">
              <button className="page-button" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0 || loading}>← <span>Previous</span></button>
              <button className="page-button" onClick={() => setPage((p) => p + 1)} disabled={loading || page >= totalPages - 1}><span>Next</span> →</button>
            </div>
          </footer>
        )}
      </div>
    </div>
  );
}