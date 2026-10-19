import { useEffect } from "react";

export default function ConfirmDialog({ title, message, confirmLabel = "Confirm", danger = false, onConfirm, onCancel }) {
  useEffect(() => {
    const onKey = (e) => { if (e.key === "Escape") onCancel(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onCancel]);

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal-card" role="dialog" aria-modal="true" aria-labelledby="confirm-title"
           onClick={(e) => e.stopPropagation()}>
        <h3 id="confirm-title">{title}</h3>
        <p>{message}</p>
        <div className="modal-actions">
          <button className="button button-secondary" onClick={onCancel}>Cancel</button>
          <button className={`button ${danger ? "button-danger" : "button-primary"}`} onClick={onConfirm} autoFocus>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}