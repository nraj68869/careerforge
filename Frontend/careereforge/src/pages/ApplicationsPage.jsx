import { useEffect, useState } from "react";
import { applicationsApi } from "../api/applications";
import AppShell from "../components/AppShell";
import ApplicationsTable from "../components/ApplicationsTable";
import Icon from "../components/Icon";
import { STATUS_OPTIONS, formatStatus } from "../utils/format";
import { saveBlob } from "../utils/download";

const PAGE_SIZE = 10;
const PIPELINE = [
  { key: "applied", label: "Applied" },
  { key: "shortlisted", label: "Shortlisted" },
  { key: "selected", label: "Selected" },
  { key: "rejected", label: "Rejected" },
  { key: "withdrawn", label: "Withdrawn" },
];

export default function ApplicationsPage() {
  const [q, setQ] = useState("");
  const [debouncedQ, setDebouncedQ] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [data, setData] = useState(null);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => { setDebouncedQ(q.trim()); setPage(0); }, 300);
    return () => window.clearTimeout(timer);
  }, [q]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    applicationsApi.all({ q: debouncedQ, status, page, size: PAGE_SIZE })
      .then((result) => { if (!cancelled) { setData(result); setError(""); } })
      .catch((err) => { if (!cancelled) setError(err.message || "Applications could not be loaded."); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [debouncedQ, status, page, reloadKey]);

  useEffect(() => {
    let cancelled = false;
    applicationsApi.stats()
      .then((result) => { if (!cancelled) setStats(result); })
      .catch(() => { /* cards just show a dash */ });
    return () => { cancelled = true; };
  }, [reloadKey]);

  const items = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const filtersActive = Boolean(q || status);
  const reload = () => setReloadKey((k) => k + 1);

  const cards = [
    { label: "APPLICATIONS", value: stats?.total, foot: "All time", icon: "briefcase", tone: "indigo" },
    { label: "AWAITING REVIEW", value: stats?.applied, foot: "Need a decision", icon: "user", tone: "amber" },
    { label: "SHORTLISTED", value: stats?.shortlisted, foot: "In the pipeline", icon: "check", tone: "teal" },
    { label: "STUDENTS PLACED", value: stats?.studentsPlaced, foot: "Selected for a job", icon: "shield", tone: "rose" },
  ];
  async function exportCsv() {
  setExporting(true);
  try {
    saveBlob(await applicationsApi.exportCsv(), "careerforge-applications.csv");
  } catch (err) {
    setError(err.message || "The export failed.");
  } finally {
    setExporting(false);
  }
}


  return (
    <AppShell title="Applications">
      <section className="welcome-row">
        <div>
          <div className="eyebrow">PLACEMENT OVERVIEW</div>
          <h1>Applications</h1>
          <p className="page-subtitle">
            Every application across the platform{stats ? ` · ${stats.openJobs} open job${stats.openJobs === 1 ? "" : "s"}` : ""}.
          </p>
        </div>
        <button className="button button-secondary" onClick={exportCsv} disabled={exporting}>
        {exporting ? "Preparing…" : "Export CSV"}
      </button>
      </section>

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
          <h2>Pipeline</h2>
          <span>{stats ? `${stats.total} applications` : ""}</span>
        </div>
        <div className="dist-bar" aria-hidden="true">
          {stats && stats.total > 0 && PIPELINE.map((p) =>
            stats[p.key] > 0 && (
              <span key={p.key} className={`dist-seg ${p.key}`} style={{ width: `${(stats[p.key] / stats.total) * 100}%` }} />
            ))}
        </div>
        <ul className="dist-legend">
          {PIPELINE.map((p) => (
            <li key={p.key}><i className={`dist-dot ${p.key}`} /> {p.label} <strong>{stats ? stats[p.key] : "—"}</strong></li>
          ))}
        </ul>
      </section>

      {error && (
        <div className="alert alert-error" role="alert">
          <span className="alert-symbol">!</span><span>{error}</span>
          <button onClick={() => setError("")} aria-label="Dismiss">×</button>
        </div>
      )}

      <section className="directory-panel">
        <div className="directory-heading">
          <div>
            <div className="heading-with-count"><h2>All applications</h2><span className="count-pill">{total}</span></div>
            <p>Search by student or job title, and move candidates through the pipeline.</p>
          </div>
          <div className="directory-tools">
            <label className="search-box">
              <span>⌕</span>
              <input aria-label="Search applications" placeholder="Student or job…" value={q}
                     onChange={(e) => setQ(e.target.value)} />
            </label>
            <select className="role-select" aria-label="Status" value={status}
                    onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
              <option value="">All statuses</option>
              {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{formatStatus(s)}</option>)}
            </select>
          </div>
        </div>

        {loading && !data ? (
          <div className="table-state"><span className="spinner" /><span>Loading applications…</span></div>
        ) : items.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">☑</div>
            <strong>{filtersActive ? "No matching applications" : "No applications yet"}</strong>
            <p>{filtersActive ? "Try another search or status." : "Applications appear here as students apply."}</p>
            {filtersActive && <button className="text-button" onClick={() => { setQ(""); setDebouncedQ(""); setStatus(""); }}>Clear filters</button>}
          </div>
        ) : (
          <ApplicationsTable items={items} showJob onChanged={reload} />
        )}

        {total > 0 && (
          <footer className="table-footer">
            <span>Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong></span>
            <div className="pagination">
              <button className="page-button" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0 || loading}>← <span>Previous</span></button>
              <button className="page-button" onClick={() => setPage((p) => p + 1)} disabled={loading || page >= totalPages - 1}><span>Next</span> →</button>
            </div>
          </footer>
        )}
      </section>
    </AppShell>
  );
}