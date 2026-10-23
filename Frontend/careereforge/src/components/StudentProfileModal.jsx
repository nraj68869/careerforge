import { useEffect, useState } from "react";
import { profileApi } from "../api/profile";
import { safeUrl } from "../utils/format";

export default function StudentProfileModal({ studentId, onClose }) {
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    profileApi.forStudent(studentId)
      .then((p) => { if (!cancelled) setProfile(p); })
      .catch((err) => { if (!cancelled) setError(err.message || "The profile could not be loaded."); });
    return () => { cancelled = true; };
  }, [studentId]);

  useEffect(() => {
    const onKey = (e) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const resume = safeUrl(profile?.resumeUrl);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card wide job-detail" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <div className="job-detail-head">
          <span className="company-logo">{(profile?.studentName ?? "?").charAt(0).toUpperCase()}</span>
          <div>
            <h3>{profile?.studentName ?? "Student profile"}</h3>
            <p className="job-company">{profile?.studentEmail ?? ""}</p>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Close">×</button>
        </div>

        {error && <div className="alert alert-error" role="alert"><span>{error}</span></div>}
        {!profile && !error && <div className="table-state"><span className="spinner" /><span>Loading profile…</span></div>}

        {profile && profile.completeness === 0 && (
          <p>This student has not filled in their profile yet.</p>
        )}

        {profile && profile.completeness > 0 && (
          <>
            <dl className="job-facts">
              <div><dt>COLLEGE</dt><dd>{profile.college ?? "—"}</dd></div>
              <div><dt>DEGREE</dt><dd>{profile.degree ?? "—"}</dd></div>
              <div><dt>GRADUATION YEAR</dt><dd>{profile.graduationYear ?? "—"}</dd></div>
              <div><dt>CGPA</dt><dd>{profile.cgpa != null ? Number(profile.cgpa).toFixed(2) : "—"}</dd></div>
            </dl>

            {profile.about && (
              <>
                <h4>About</h4>
                <p className="job-description">{profile.about}</p>
              </>
            )}

            {profile.skills.length > 0 && (
              <>
                <h4>Skills</h4>
                <div className="profile-skills">
                  {profile.skills.map((s) => <span className="skill-chip static" key={s}>{s}</span>)}
                </div>
              </>
            )}
          </>
        )}

        <div className="modal-actions">
          {resume && (
            <a className="button button-primary" href={resume} target="_blank" rel="noopener noreferrer">
              View resume
            </a>
          )}
          <button className="button button-secondary" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}