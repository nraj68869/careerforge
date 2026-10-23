import { useEffect, useState } from "react";
import { usersApi } from "../api/user";
import { initials } from "../utils/format";
import ConfirmDialog from "./ConfirmDialog";

export default function PendingRecruiters({ reloadKey, onChanged }) {
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [confirmState, setConfirmState] = useState(null);

  useEffect(() => {
    let cancelled = false;
    usersApi.pending()
      .then((list) => { if (!cancelled) { setItems(list); setError(""); } })
      .catch((err) => { if (!cancelled) setError(err.message || "Pending recruiters could not be loaded."); });
    return () => { cancelled = true; };
  }, [reloadKey]);

  async function review(user, status) {
    setError("");
    setBusyId(user.id);
    try {
      await usersApi.review(user.id, status);
      onChanged();
    } catch (err) {
      setError(err.message || "The decision could not be saved.");
    } finally {
      setBusyId(null);
    }
  }

  function requestReject(user) {
    setConfirmState({
      title: `Reject ${user.name}?`,
      message: "They will not be able to post jobs, and any open jobs they posted will be closed.",
      confirmLabel: "Reject recruiter",
      danger: true,
      onConfirm: () => review(user, "REJECTED"),
    });
  }

  if (items.length === 0 && !error) return null;

  return (
    <section className="pending-panel" aria-label="Recruiters awaiting approval">
      <h2>Recruiters awaiting approval ({items.length})</h2>
      <p>Check that the person and company are genuine before approving. Approved recruiters can post jobs students will see.</p>

      {error && <div className="alert alert-error" role="alert"><span>{error}</span></div>}

      <ul className="pending-list">
        {items.map((u) => (
          <li className="pending-item" key={u.id}>
            <div className="avatar avatar-recruiter" aria-hidden="true">{initials(u.name)}</div>
            <div className="info">
              <strong>{u.name}</strong>
              <span>{u.email}</span>
            </div>
            <div className="pending-actions">
              <button className="button button-primary small" onClick={() => review(u, "APPROVED")} disabled={busyId === u.id}>
                Approve
              </button>
              <button className="delete-button" onClick={() => requestReject(u)} disabled={busyId === u.id}>
                Reject
              </button>
            </div>
          </li>
        ))}
      </ul>

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
    </section>
  );
}