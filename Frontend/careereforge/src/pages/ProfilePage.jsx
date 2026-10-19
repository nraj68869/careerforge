import { useState } from "react";
import { useRecoilState } from "recoil";
import { usersApi } from "../api/user";
import AppShell from "../components/AppShell";
import ChangePasswordForm from "../components/ChangePasswordForm";
import { currentUserState } from "../state/authState";
import { formatRole } from "../utils/format";

export default function ProfilePage({ onPasswordChanged }) {
  const [user, setUser] = useRecoilState(currentUserState);
  const [form, setForm] = useState({ name: user.name, email: user.email });
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState({ type: "", text: "" });

  async function handleSave(event) {
    event.preventDefault();
    setSaving(true);
    setMessage({ type: "", text: "" });
    try {
      const updated = await usersApi.update(user.id, { name: form.name.trim(), email: form.email.trim() });
      setUser(updated);
      setMessage({ type: "success", text: "Profile updated." });
    } catch (err) {
      setMessage({ type: "error", text: err.message });
    } finally {
      setSaving(false);
    }
  }

  return (
    <AppShell title="My account">
      <section className="welcome-row">
        <div>
          <div className="eyebrow">ACCOUNT</div>
          <h1>My account</h1>
          <p className="page-subtitle">Signed in as {user.email} · {formatRole(user.role)}</p>
        </div>
      </section>

      <div className="account-grid">
        <section className="panel-card">
          <h2>Profile</h2>
          {message.text && (
            <div className={`alert alert-${message.type}`} role="status"><span>{message.text}</span></div>
          )}
          <form className="auth-form" onSubmit={handleSave}>
            <label>Full name
              <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} maxLength={100} required />
            </label>
            <label>Email address
              <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} maxLength={254} required />
            </label>
            <button className="button button-primary" disabled={saving}>{saving ? "Saving…" : "Save changes"}</button>
          </form>
        </section>

        <section className="panel-card">
          <ChangePasswordForm onChanged={onPasswordChanged} />
        </section>
      </div>
    </AppShell>
  );
}