import { useState } from "react";
import { authApi } from "../api/auth";

export default function ChangePasswordForm({ onChanged }) {
  const [form, setForm] = useState({ currentPassword: "", newPassword: "", confirm: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    if (form.newPassword !== form.confirm) {
      setError("The new passwords do not match.");
      return;
    }
    setBusy(true);
    try {
      await authApi.changePassword(form.currentPassword, form.newPassword);
      onChanged();
    } catch (err) {
      setError(err.message || "The password could not be changed.");
    } finally {
      setBusy(false);
    }
  }

  const setField = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h2>Change password</h2>
      {error && <div className="alert alert-error" role="alert"><span>{error}</span></div>}
      <label>Current password
        <input type="password" value={form.currentPassword} onChange={setField("currentPassword")}
               autoComplete="current-password" required />
      </label>
      <label>New password
        <input type="password" value={form.newPassword} onChange={setField("newPassword")}
               minLength={8} maxLength={72} autoComplete="new-password" required />
      </label>
      <label>Repeat new password
        <input type="password" value={form.confirm} onChange={setField("confirm")}
               autoComplete="new-password" required />
      </label>
      <button className="button button-primary" disabled={busy}>
        {busy ? "Saving…" : "Change password"}
      </button>
    </form>
  );
}