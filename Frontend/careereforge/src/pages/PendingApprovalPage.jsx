import { useState } from "react";
import { useRecoilState } from "recoil";
import { authApi } from "../api/auth";
import { currentUserState } from "../state/authState";

export default function PendingApprovalPage({ onLogout }) {
  const [user, setUser] = useRecoilState(currentUserState);
  const [checking, setChecking] = useState(false);
  const [error, setError] = useState("");
  const rejected = user.approvalStatus === "REJECTED";

  async function checkStatus() {
    setChecking(true);
    setError("");
    try {
      const me = await authApi.me();
      setUser(me);   // if you were approved, the app switches to your jobs page by itself
    } catch (err) {
      setError(err.message || "Your status could not be checked.");
    } finally {
      setChecking(false);
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-card pending-card">
        <div className="brand">
          <span className="brand-mark">C</span>
          <span className="brand-name">career<span>forge</span></span>
        </div>

        <div className={`pending-icon ${rejected ? "bad" : ""}`}>{rejected ? "✕" : "⏳"}</div>
        <h1>{rejected ? "Request not approved" : "Waiting for approval"}</h1>
        <p className="page-subtitle">
          {rejected
            ? "The administrator did not approve this recruiter account, so you cannot post jobs. Contact the administrator if you think this is a mistake."
            : `Thanks for registering, ${user.name.split(" ")[0]}. The administrator reviews every recruiter account before it can post jobs. This usually takes a short while.`}
        </p>

        {error && <div className="alert alert-error" role="alert"><span>{error}</span></div>}

        {!rejected && (
          <button className="button button-primary" onClick={checkStatus} disabled={checking}>
            {checking ? "Checking…" : "Check my status"}
          </button>
        )}
        <button className="button logout-button" onClick={onLogout}>Log out</button>
      </div>
    </div>
  );
}