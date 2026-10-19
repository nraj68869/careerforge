import { useEffect } from "react";
import { deadlineText, formatDate, formatSalary, formatType } from "../utils/format";

export default function JobDetailModal({ job, onClose, children }) {
  useEffect(() => {
    const onKey = (e) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card wide job-detail" role="dialog" aria-modal="true"
           onClick={(e) => e.stopPropagation()}>
        <div className="job-detail-head">
          <span className="company-logo">{job.company.charAt(0).toUpperCase()}</span>
          <div>
            <h3>{job.title}</h3>
            <p className="job-company">{job.company}</p>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Close">×</button>
        </div>

        <div className="job-chips">
          <span className="chip">{job.location}</span>
          <span className="chip type">{formatType(job.type)}</span>
        </div>

        <dl className="job-facts">
          <div><dt>SALARY</dt><dd>{formatSalary(job.salaryMin, job.salaryMax)}</dd></div>
          <div><dt>APPLY BEFORE</dt><dd>{formatDate(job.deadline)} · {deadlineText(job.deadline)}</dd></div>
          <div><dt>POSTED BY</dt><dd>{job.postedByName}</dd></div>
          <div><dt>POSTED ON</dt><dd>{formatDate(job.createdAt.slice(0, 10))}</dd></div>
        </dl>

        <h4>About this role</h4>
        <p className="job-description">{job.description}</p>

        <div className="modal-actions">{children}</div>
      </div>
    </div>
  );
}