import { useEffect, useState } from "react";
import { applicationsApi } from "../api/applications";
import { jobsApi } from "../api/jobs";
import { useApp } from "../context/AppContext";
import { JOB_TYPES, daysLeft, deadlineText, formatSalary, formatType } from "../utils/format";
import ApplicantsModal from "./ApplicantsModal";
import ApplyModal from "./ApplyModal";
import ConfirmDialog from "./ConfirmDialog";
import JobDetailModal from "./JobDetailModal";
import JobFormModal from "./JobFormModal";
import StatusBadge from "./StatusBadge";
import ProfileNudge from "./ProfileNudge";

const PAGE_SIZE = 9;

function statusInfo(job) {
  if (job.status === "CLOSED") return { label: "Closed", cls: "off" };
  if (!job.acceptingApplications) return { label: "Expired", cls: "warn" };
  return { label: "Open", cls: "on" };
}

export default function JobBoard({ mode }) {
  const { user } = useApp();
  const isMine = mode === "mine";
  const isStudent = user.role === "STUDENT";

  const [q, setQ] = useState("");
  const [debouncedQ, setDebouncedQ] = useState("");
  const [type, setType] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [formJob, setFormJob] = useState(undefined);   // undefined = closed, null = new job, object = editing
  const [viewJob, setViewJob] = useState(null);
  const [applyJob, setApplyJob] = useState(null);
  const [applicantsJob, setApplicantsJob] = useState(null);
  const [confirmState, setConfirmState] = useState(null);
  const [workingId, setWorkingId] = useState(null);

  // wait 300 ms after the user stops typing before searching
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedQ(q.trim());
      setPage(0);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [q]);

  // load jobs whenever a filter, the page, or the reload counter changes
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    const load = isMine ? jobsApi.mine : jobsApi.browse;
    load({ q: debouncedQ, type, status, page, size: PAGE_SIZE })
      .then((result) => { if (!cancelled) { setData(result); setError(""); } })
      .catch((err) => { if (!cancelled) setError(err.message || "Jobs could not be loaded."); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [isMine, debouncedQ, type, status, page, reloadKey]);

  const jobs = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const filtersActive = Boolean(q || type || status);
  const reload = () => setReloadKey((k) => k + 1);

  const canManage = (job) =>
    user.role === "ADMIN" ||
    user.role === "PLACEMENT_OFFICER" ||
    (user.role === "RECRUITER" && job.postedById === user.id);

  const canApply = (job) =>
    isStudent && job.acceptingApplications &&
    (!job.myApplicationStatus || job.myApplicationStatus === "WITHDRAWN");

  function clearFilters() {
    setQ(""); setDebouncedQ(""); setType(""); setStatus(""); setPage(0);
  }

  async function saveJob(payload) {
    if (formJob) {
      await jobsApi.update(formJob.id, payload);
      setNotice("Job updated.");
    } else {
      await jobsApi.create(payload);
      setNotice("Job posted.");
      setPage(0);
    }
    setFormJob(undefined);
    setViewJob(null);
    reload();
  }

  async function submitApplication(coverNote) {
    await applicationsApi.apply(applyJob.id, coverNote);
    setApplyJob(null);
    setNotice("Application submitted. Track it under My applications.");
    reload();
  }

  async function runAction(id, action, successText) {
    setError("");
    setNotice("");
    setWorkingId(id);
    try {
      await action();
      setNotice(successText);
      setViewJob(null);
      reload();
    } catch (err) {
      setError(err.message || "That action failed.");
    } finally {
      setWorkingId(null);
    }
  }

  function requestToggle(job) {
    const reopen = job.status === "CLOSED";
    setConfirmState({
      title: reopen ? "Reopen this job?" : "Close this job?",
      message: reopen
        ? "Students will be able to see it again, if its deadline has not passed."
        : "Students will no longer see this job or be able to apply. You can reopen it later.",
      confirmLabel: reopen ? "Reopen" : "Close job",
      danger: !reopen,
      onConfirm: () => runAction(job.id, () => jobsApi.setStatus(job.id, reopen ? "OPEN" : "CLOSED"),
        reopen ? "Job reopened." : "Job closed."),
    });
  }

  function requestDelete(job) {
    setConfirmState({
      title: `Delete "${job.title}"?`,
      message: "This permanently deletes the job. Jobs that already have applications cannot be deleted. Close them instead.",
      confirmLabel: "Delete job",
      danger: true,
      onConfirm: async () => {
        await runAction(job.id, () => jobsApi.remove(job.id), "Job deleted.");
        if (jobs.length === 1 && page > 0) setPage(page - 1);
      },
    });
  }

  const title = isMine ? "My jobs" : "Browse jobs";
  const subtitle = isMine
    ? "Jobs you have posted."
    : isStudent ? "Open positions you can apply for." : "All jobs on the platform.";
  const start = total === 0 ? 0 : page * PAGE_SIZE + 1;
  const end = Math.min((page + 1) * PAGE_SIZE, total);

  return (
    <>
      <section className="welcome-row">
        <div>
          <div className="eyebrow">{isMine ? "RECRUITING" : "OPPORTUNITIES"}</div>
          <h1>{title}</h1>
          <p className="page-subtitle">{subtitle}</p>
        </div>
        {!isStudent && (
          <button className="button button-primary" onClick={() => setFormJob(null)}>
            <span className="button-plus">+</span> Post a job
          </button>
        )}
      </section>
      {isStudent && !isMine && <ProfileNudge />}

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
        <label className="search-box">
          <span>⌕</span>
          <input aria-label="Search jobs" placeholder="Search title or company…" value={q}
                 onChange={(e) => setQ(e.target.value)} />
        </label>
        <select aria-label="Job type" value={type} onChange={(e) => { setType(e.target.value); setPage(0); }}>
          <option value="">All types</option>
          {JOB_TYPES.map((t) => <option key={t} value={t}>{formatType(t)}</option>)}
        </select>
        {!isStudent && (
          <select aria-label="Status" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="">All statuses</option>
            <option value="OPEN">Open</option>
            <option value="CLOSED">Closed</option>
          </select>
        )}
        {filtersActive && <button className="text-button" onClick={clearFilters}>Clear filters</button>}
      </div>

      {loading && !data ? (
        <div className="table-state"><span className="spinner" /><span>Loading jobs…</span></div>
      ) : jobs.length === 0 ? (
        <div className="empty-state panel-card">
          <div className="empty-icon">▤</div>
          <strong>{filtersActive ? "No matching jobs" : isMine ? "You haven't posted any jobs yet" : "No jobs to show yet"}</strong>
          <p>
            {filtersActive
              ? "Try a different search or clear the filters."
              : isStudent ? "New opportunities appear here as soon as recruiters post them."
              : "Post the first job to get started."}
          </p>
          {filtersActive
            ? <button className="text-button" onClick={clearFilters}>Clear filters</button>
            : !isStudent && <button className="button button-primary" onClick={() => setFormJob(null)}>Post a job</button>}
        </div>
      ) : (
        <div className={`job-grid${loading ? " is-loading" : ""}`}>
          {jobs.map((job) => {
            const manage = canManage(job);
            const left = daysLeft(job.deadline);
            const pill = statusInfo(job);
            return (
              <article className="job-card" key={job.id}>
                <button type="button" className="job-card-main" onClick={() => setViewJob(job)}>
                  <div className="job-card-top">
                    <span className="company-logo">{job.company.charAt(0).toUpperCase()}</span>
                    <div>
                      <h3 className="job-title">{job.title}</h3>
                      <p className="job-company">{job.company}</p>
                    </div>
                  </div>
                  <div className="job-chips">
                    <span className="chip">{job.location}</span>
                    <span className="chip type">{formatType(job.type)}</span>
                  </div>
                  <div className="job-salary">{formatSalary(job.salaryMin, job.salaryMax)}</div>
                </button>

                <div className="job-card-foot">
                  <span className={`deadline${left >= 0 && left <= 3 ? " soon" : ""}`}>{deadlineText(job.deadline)}</span>
                  {isStudent
                    ? job.myApplicationStatus && <StatusBadge status={job.myApplicationStatus} />
                    : <span className={`status-pill ${pill.cls}`}>{pill.label}</span>}
                </div>

                {isStudent && canApply(job) && (
                  <div className="job-card-actions">
                    <button className="button button-primary small" onClick={() => setApplyJob(job)}>
                      {job.myApplicationStatus === "WITHDRAWN" ? "Apply again" : "Apply now"}
                    </button>
                  </div>
                )}

                {manage && (
                  <div className="job-card-actions">
                    <button className="button button-primary small" onClick={() => setApplicantsJob(job)}>Applicants</button>
                    <button className="text-button" onClick={() => setFormJob(job)}>Edit</button>
                    <button className="text-button" onClick={() => requestToggle(job)} disabled={workingId === job.id}>
                      {job.status === "CLOSED" ? "Reopen" : "Close"}
                    </button>
                    <button className="delete-button" onClick={() => requestDelete(job)} disabled={workingId === job.id}>Delete</button>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}

      {total > 0 && (
        <footer className="job-pager">
          <span>Showing <strong>{start}–{end}</strong> of <strong>{total}</strong> jobs</span>
          <div className="pagination">
            <button className="page-button" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0 || loading}>← <span>Previous</span></button>
            <span className="page-number">{page + 1} <span>of</span> {totalPages}</span>
            <button className="page-button" onClick={() => setPage((p) => p + 1)} disabled={loading || page >= totalPages - 1}><span>Next</span> →</button>
          </div>
        </footer>
      )}

      {viewJob && (
        <JobDetailModal job={viewJob} onClose={() => setViewJob(null)}>
          {canManage(viewJob) ? (
            <>
              <button className="button button-primary" onClick={() => { const j = viewJob; setViewJob(null); setApplicantsJob(j); }}>Applicants</button>
              <button className="button button-secondary" onClick={() => { const j = viewJob; setViewJob(null); setFormJob(j); }}>Edit</button>
              <button className="button button-secondary" onClick={() => requestToggle(viewJob)}>
                {viewJob.status === "CLOSED" ? "Reopen" : "Close job"}
              </button>
              <button className="button button-danger" onClick={() => requestDelete(viewJob)}>Delete</button>
            </>
          ) : canApply(viewJob) ? (
            <button className="button button-primary" onClick={() => { const j = viewJob; setViewJob(null); setApplyJob(j); }}>
              {viewJob.myApplicationStatus === "WITHDRAWN" ? "Apply again" : "Apply now"}
            </button>
          ) : viewJob.myApplicationStatus && viewJob.myApplicationStatus !== "WITHDRAWN" ? (
            <>
              <StatusBadge status={viewJob.myApplicationStatus} />
              <a className="button button-secondary" href="#/my-applications">View my applications</a>
            </>
          ) : (
            <button className="button button-secondary" disabled>Not accepting applications</button>
          )}
        </JobDetailModal>
      )}

      {applyJob && (
        <ApplyModal job={applyJob} onSubmit={submitApplication} onClose={() => setApplyJob(null)} />
      )}

      {applicantsJob && (
        <ApplicantsModal job={applicantsJob} onClose={() => setApplicantsJob(null)} />
      )}

      {formJob !== undefined && (
        <JobFormModal job={formJob} onSave={saveJob} onClose={() => setFormJob(undefined)} />
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
    </>
  );
}