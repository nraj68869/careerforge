import { useState } from "react";
import { applicationsApi } from "../api/applications";
import { useApp } from "../context/AppContext";
import { formatDate, formatStatus, initials } from "../utils/format";
import ConfirmDialog from "./ConfirmDialog";
import StatusBadge from "./StatusBadge";
import StudentProfileModal from "./StudentProfileModal";

function actionLabel(current, next) {
  if (next === "SHORTLISTED") return current === "REJECTED" ? "Reconsider" : "Shortlist";
  if (next === "SELECTED") return "Select";
  if (next === "REJECTED") return "Reject";
  return formatStatus(next);
}

export default function ApplicationsTable({ items, showJob = false, onChanged }) {
  const { user } = useApp();
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");
  const [confirmState, setConfirmState] = useState(null);
  const [profileId, setProfileId] = useState(null);

  async function changeStatus(app, next) {
    setError("");
    setBusyId(app.id);
    try {
      await applicationsApi.updateStatus(app.id, next);
      onChanged();
    } catch (err) {
      setError(err.message || "The status could not be changed.");
    } finally {
      setBusyId(null);
    }
  }

  function requestChange(app, next) {
    if (next === "SHORTLISTED") {
      changeStatus(app, next);
      return;
    }
    const select = next === "SELECTED";
    setConfirmState({
      title: select ? `Select ${app.studentName}?` : `Reject ${app.studentName}?`,
      message: select
        ? "This marks the student as placed for this job. It cannot be undone."
        : "The student will see this application as rejected. You can reconsider it later.",
      confirmLabel: select ? "Select student" : "Reject",
      danger: !select,
      onConfirm: () => changeStatus(app, next),
    });
  }

  return (
    <>
      {error && (
        <div className="alert alert-error" role="alert">
          <span className="alert-symbol">!</span><span>{error}</span>
          <button onClick={() => setError("")} aria-label="Dismiss">×</button>
        </div>
      )}

      <div className="table-wrap">
        <table className="student-table">
          <thead>
            <tr>
              <th>STUDENT</th>
              {showJob && <th>JOB</th>}
              <th>APPLIED ON</th>
              <th>STATUS</th>
              <th className="actions-heading">ACTIONS</th>
            </tr>
          </thead>
          <tbody>
            {items.map((a) => {
              // recruiters cannot open the profile of a student who withdrew
              const canViewProfile = a.status !== "WITHDRAWN" || user.role !== "RECRUITER";
              return (
                <tr key={a.id}>
                  <td>
                    <div className="student-cell">
                      <div className="avatar avatar-student" aria-hidden="true">{initials(a.studentName)}</div>
                      <div>
                        <strong>{a.studentName}</strong>
                        <span>{a.studentEmail}</span>
                        {canViewProfile && (
                          <button className="text-button profile-link" onClick={() => setProfileId(a.studentId)}>
                            View profile
                          </button>
                        )}
                        {a.coverNote && (
                          <details className="note">
                            <summary>Cover note</summary>
                            <p>{a.coverNote}</p>
                          </details>
                        )}
                      </div>
                    </div>
                  </td>
                  {showJob && (
                    <td>
                      <div className="student-cell"><div><strong>{a.jobTitle}</strong><span>{a.company}</span></div></div>
                    </td>
                  )}
                  <td>{formatDate(a.appliedAt.slice(0, 10))}</td>
                  <td><StatusBadge status={a.status} /></td>
                  <td className="actions-cell">
                    <div className="row-actions">
                      {a.nextStatuses.length === 0 && <span className="muted-text">No actions</span>}
                      {a.nextStatuses.map((next) => (
                        <button key={next}
                                className={next === "REJECTED" ? "delete-button" : "button button-primary small"}
                                onClick={() => requestChange(a, next)}
                                disabled={busyId === a.id}>
                          {actionLabel(a.status, next)}
                        </button>
                      ))}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {profileId && <StudentProfileModal studentId={profileId} onClose={() => setProfileId(null)} />}

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