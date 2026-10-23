import { useEffect, useState } from "react";
import { profileApi } from "../api/profile";
import AppShell from "../components/AppShell";
import SkillsInput from "../components/SkillsInput";

const EMPTY = { college: "", degree: "", graduationYear: "", cgpa: "", about: "", resumeUrl: "" };

export default function StudentProfilePage() {
  const [form, setForm] = useState(EMPTY);
  const [skills, setSkills] = useState([]);
  const [completeness, setCompleteness] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState({ type: "", text: "" });

  useEffect(() => {
    let cancelled = false;
    profileApi.mine()
      .then((p) => {
        if (cancelled) return;
        setForm({
          college: p.college ?? "",
          degree: p.degree ?? "",
          graduationYear: p.graduationYear ?? "",
          cgpa: p.cgpa ?? "",
          about: p.about ?? "",
          resumeUrl: p.resumeUrl ?? "",
        });
        setSkills(p.skills ?? []);
        setCompleteness(p.completeness);
      })
      .catch((err) => { if (!cancelled) setMessage({ type: "error", text: err.message }); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  async function handleSave(event) {
    event.preventDefault();
    setSaving(true);
    setMessage({ type: "", text: "" });
    try {
      const saved = await profileApi.saveMine({
        college: form.college.trim(),
        degree: form.degree.trim(),
        graduationYear: form.graduationYear === "" ? null : Number(form.graduationYear),
        cgpa: form.cgpa === "" ? null : Number(form.cgpa),
        about: form.about.trim(),
        resumeUrl: form.resumeUrl.trim(),
        skills,
      });
      setSkills(saved.skills);
      setCompleteness(saved.completeness);
      setMessage({ type: "success", text: "Profile saved." });
    } catch (err) {
      setMessage({ type: "error", text: err.message || "Your profile could not be saved." });
    } finally {
      setSaving(false);
    }
  }

  return (
    <AppShell title="My profile">
      <section className="welcome-row">
        <div>
          <div className="eyebrow">YOUR DETAILS</div>
          <h1>My profile</h1>
          <p className="page-subtitle">Recruiters see this only when you apply to one of their jobs.</p>
        </div>
      </section>

      <div className="completeness">
        <div className="completeness-head">
          <span>Profile completeness</span><strong>{completeness}%</strong>
        </div>
        <div className="progress"><span style={{ width: `${completeness}%` }} /></div>
      </div>

      {message.text && (
        <div className={`alert alert-${message.type}`} role="status"><span>{message.text}</span></div>
      )}

      {loading ? (
        <div className="table-state"><span className="spinner" /><span>Loading your profile…</span></div>
      ) : (
        <section className="panel-card profile-form-panel">
          <form className="job-form" onSubmit={handleSave}>
            <label className="full">College or university
              <input value={form.college} onChange={set("college")} maxLength={150} placeholder="e.g. Graphic Era University" />
            </label>
            <label>Degree and branch
              <input value={form.degree} onChange={set("degree")} maxLength={100} placeholder="e.g. B.Tech Computer Science" />
            </label>
            <label>Graduation year
              <input type="number" min="2000" max="2100" value={form.graduationYear} onChange={set("graduationYear")} placeholder="e.g. 2027" />
            </label>
            <label>CGPA (out of 10)
              <input type="number" min="0" max="10" step="0.01" value={form.cgpa} onChange={set("cgpa")} placeholder="e.g. 8.4" />
            </label>
            <label>Resume link
              <input type="url" value={form.resumeUrl} onChange={set("resumeUrl")} maxLength={500}
                     placeholder="https://drive.google.com/…" />
            </label>

            <div className="field full">
              <span className="field-label">Skills</span>
              <SkillsInput value={skills} onChange={setSkills} />
            </div>

            <label className="full">About you
              <textarea value={form.about} onChange={set("about")} maxLength={500}
                        placeholder="A few lines about what you are learning and what you want to do." />
              <span className="char-count">{form.about.length} / 500</span>
            </label>

            <p className="privacy-note full">
              For the resume, paste a public link (Google Drive, OneDrive, LinkedIn) and check that anyone with the link can view it.
            </p>

            <div className="modal-actions">
              <button className="button button-primary" disabled={saving}>{saving ? "Saving…" : "Save profile"}</button>
            </div>
          </form>
        </section>
      )}
    </AppShell>
  );
}