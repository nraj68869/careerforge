import { useState } from "react";
import { authApi } from "../api/auth";
import Icon from "./Icon";
import PasswordInput from "./PasswordInput";

const FEATURES = [
  { icon: "shield", title: "Role-based access", text: "Students, recruiters, officers and admins only see what they are allowed to." },
  { icon: "lock", title: "Secure sessions", text: "Signed tokens, instant logout and lockout after repeated failed logins." },
  { icon: "briefcase", title: "Built for placement teams", text: "Manage students today, with jobs and applications on the way." },
];
const ACCOUNT_TYPES = [
  { value: "STUDENT", label: "Student", hint: "Find jobs and apply" },
  { value: "RECRUITER", label: "Recruiter", hint: "Post jobs . needs approval" },
];

function Brand() {
  return (
    <div className="brand">
      <span className="brand-mark">C</span>
      <span className="brand-name">career<span>forge</span></span>
    </div>
  );
}

export default function AuthPage({ onLoggedIn, message }) {
  const [mode, setMode] = useState("login");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ name: "", email: "", password: "", role: "STUDENT" });

  const isLogin = mode === "login";
  const setField = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const rules = [
    { label: "At least 8 characters", ok: form.password.length >= 8 },
    { label: "Contains a letter", ok: /[A-Za-z]/.test(form.password) },
    { label: "Contains a number", ok: /\d/.test(form.password) },
  ];
  const metCount = rules.filter((r) => r.ok).length;

  function switchMode(next) {
    if (next === mode) return;
    setMode(next);
    setError("");
    setInfo("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setInfo("");
    setBusy(true);
    try {
      if (isLogin) {
        const user = await authApi.login(form.email.trim(), form.password);
        onLoggedIn(user);
      } else {
        const wasRecruiter = form.role === "RECRUITER";
        await authApi.register(form.name.trim(), form.email.trim(), form.password, form.role);
        setMode("login");
        setForm({ name: "", email: form.email.trim(), password: "", role: "STUDENT" });
        setInfo(wasRecruiter
        ? "Account created. The administrator must approve your recruiter account before you can post jobs. You can log in to check."
        : "Account created. Please log in.");
      }
    } catch (err) {
      setError(err.message || "Something went wrong. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-layout">
      <aside className="auth-hero">
        <div className="auth-hero-inner">
          <Brand />

          <div className="hero-copy">
            <span className="hero-pill">PLACEMENT MANAGEMENT PLATFORM</span>
            <h2>Where careers get <em>forged.</em></h2>
            <p className="hero-lead">
              One secure workspace for students, recruiters and placement officers.
            </p>
            <ul className="hero-points">
              {FEATURES.map((f) => (
                <li key={f.title}>
                  <span className="hero-icon"><Icon name={f.icon} /></span>
                  <div>
                    <strong>{f.title}</strong>
                    <span className="hero-text">{f.text}</span>
                  </div>
                </li>
              ))}
            </ul>
          </div>

          <div className="hero-foot">© {new Date().getFullYear()} CareerForge · All rights reserved</div>
        </div>
      </aside>

      <main className="auth-panel">
        <div className="auth-box">
          <div className="auth-mobile-brand"><Brand /></div>

          <div className="auth-tabs" role="tablist" aria-label="Choose login or sign up">
            <button type="button" role="tab" aria-selected={isLogin} onClick={() => switchMode("login")}>Log in</button>
            <button type="button" role="tab" aria-selected={!isLogin} onClick={() => switchMode("register")}>Create account</button>
          </div>

          <h1>{isLogin ? "Welcome back" : "Create your account"}</h1>
          <p className="auth-sub">
            {isLogin ? "Log in to continue to your CareerForge workspace." : "Register as a student to get started in a minute."}
          </p>

          {message && <div className="alert alert-error" role="alert"><span>{message}</span></div>}
          {info && <div className="alert alert-success" role="status"><span>{info}</span></div>}
          {error && <div className="alert alert-error" role="alert"><span>{error}</span></div>}

          <form className="auth-form" onSubmit={handleSubmit}>
            {!isLogin && (
              <div className="role-picker" role="radiogroup" aria-label="I am registering as">
                {ACCOUNT_TYPES.map((t) => (
                  <button type="button" key={t.value} role="radio" aria-checked={form.role === t.value}
                          className={form.role === t.value ? "selected" : ""}
                          onClick={() => setForm({ ...form, role: t.value })}>
                    <strong>{t.label}</strong>
                    <span>{t.hint}</span>
                  </button>
                ))}
              </div>
            )}
            {!isLogin && (
              <label>Full name
                <div className="input-wrap has-icon">
                  <span className="input-icon"><Icon name="user" /></span>
                  <input value={form.name} onChange={setField("name")} maxLength={100}
                         placeholder="e.g. Aditi Sharma" autoComplete="name" required />
                </div>
              </label>
            )}

            <label>Email address
              <div className="input-wrap has-icon">
                <span className="input-icon"><Icon name="mail" /></span>
                <input type="email" value={form.email} onChange={setField("email")} maxLength={254}
                       placeholder="you@example.com" autoComplete="email" required />
              </div>
            </label>

            <label>Password
              <PasswordInput icon="lock" value={form.password} onChange={setField("password")}
                             minLength={isLogin ? undefined : 8}
                             placeholder={isLogin ? "Enter your password" : "Create a password"}
                             autoComplete={isLogin ? "current-password" : "new-password"} />
            </label>

            {!isLogin && (
              <div className="pw-help" aria-live="polite">
                <div className={`pw-meter s${metCount}`}><span /><span /><span /></div>
                <ul className="pw-rules">
                  {rules.map((r) => (
                    <li key={r.label} className={r.ok ? "ok" : ""}>
                      <Icon name="check" size={14} /> {r.label}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <button className="auth-submit" disabled={busy}>
              {busy
                ? <><span className="btn-spinner" /> Please wait…</>
                : <>{isLogin ? "Log in" : "Create account"} <Icon name="arrow" size={16} /></>}
            </button>
          </form>

          <p className="auth-note"><Icon name="shield" size={14} /> Passwords are encrypted. Sessions are signed and revocable.</p>
        </div>
      </main>
    </div>
  );
}