import { useEffect, useState } from "react";
import { formatType } from "../utils/format";

export default function ApplyModal({ job, onSubmit, onClose }) {
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const onKey = (e) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSaving(true);
    try {
      await onSubmit(note.trim());
    } catch (err) {
      setError(err.message || "Your application could not be submitted.");
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop">
      <div className="modal-card" role="dialog" aria-modal="true">
        <h3>Apply to {job.title}</h3>
        <p>{job.company} · {job.location} · {formatType(job.type)}</p>

        {error && <div className="alert alert-error" role="alert"><span>{error}</span></div>}

        <form className="job-form single" onSubmit={handleSubmit}>
          <label className="full">Cover note (optional)
            <textarea value={note} onChange={(e) => setNote(e.target.value)} maxLength={1000}
                      placeholder="Why are you a good fit for this role?" />
            <span className="char-count">{note.length} / 1000</span>
          </label>
          <p className="share-note">Your name and email address will be shared with the recruiter.</p>
          <div className="modal-actions">
            <button type="button" className="button button-secondary" onClick={onClose}>Cancel</button>
            <button className="button button-primary" disabled={saving}>
              {saving ? "Submitting…" : "Submit application"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}