import { useEffect } from "react";
import { deadlineText, formatDate, formatSalary, formatType } from "../utils/format";
import MatchPill from "./MatchPill";

export default function JobDetailModal({ job, onClose, children }) {
  useEffect(() => {
    const onKey = (e) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const matched = new Set((job.match?.matchedSkills ?? []).map((s) => s.toLowerCase()));
  const cgpaNote =
    job.match?.cgpaOk === true ? " · you meet this"
    : job.match?.cgpaOk === false ? " · your CGPA is below this"
    : job.match && job.minCgpa != null ? " · add your CGPA to your profile"
    : "";

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
          <MatchPill match={job.match} />
        </div>

        <dl className="job-facts">
          <div><dt>SALARY</dt><dd>{formatSalary(job.salaryMin, job.salaryMax)}</dd></div>
          <div><dt>APPLY BEFORE</dt><dd>{formatDate(job.deadline)} · {deadlineText(job.deadline)}</dd></div>
          <div><dt>POSTED BY</dt><dd>{job.postedByName}</dd></div>
          <div><dt>POSTED ON</dt><dd>{formatDate(job.createdAt.slice(0, 10))}</dd></div>
          {job.minCgpa != null && (
            <div><dt>MINIMUM CGPA</dt><dd>{Number(job.minCgpa).toFixed(2)}{cgpaNote}</dd></div>
          )}
        </dl>

        {job.requiredSkills?.length > 0 && (
          <>
            <h4>Skills needed</h4>
            <div className="profile-skills">
              {job.requiredSkills.map((skill) => {
                const state = job.match ? (matched.has(skill.toLowerCase()) ? "ok" : "miss") : "";
                return (
                  <span key={skill} className={`skill-chip static ${state}`}>
                    {state === "ok" ? "✓ " : ""}{skill}
                  </span>
                );
              })}
            </div>
          </>
        )}

        <h4>About this role</h4>
        <p className="job-description">{job.description}</p>

        <div className="modal-actions">{children}</div>
      </div>
    </div>
  );
}