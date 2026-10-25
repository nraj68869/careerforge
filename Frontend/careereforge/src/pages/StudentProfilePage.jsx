import { useEffect, useRef, useState } from "react";
import { profileApi } from "../api/profile";
import AppShell from "../components/AppShell";
import SkillsInput from "../components/SkillsInput";

const EMPTY = { college: "", degree: "", graduationYear: "", cgpa: "", about: "", resumeUrl: "" };
const MAX_BYTES = 2 * 1024 * 1024;

export default function StudentProfilePage() {
  const [form, setForm] = useState(EMPTY);
  const [skills, setSkills] = useState([]);
  const [completeness, setCompleteness] = useState(0);
  const [resume, setResume] = useState({ hasFile: false, name: null });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState({ type: "", text: "" });
  const fileInput = useRef(null);

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
        setResume({ hasFile: p.hasResumeFile, name: p.resumeFileName });
      })
      .catch((err) => { if (!cancelled) setMessage({ type: "error", text: err.message }); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });
  const showError = (text) => setMessage({ type: "error", text });

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
      showError(err.message || "Your profile could not be saved.");
    } finally {
      setSaving(false);
    }
  }

  async function handleFile(event) {
    const file = event.target.files?.[0];
    event.target.value = "";                 // lets you choose the same file again later
    if (!file) return;
    if (file.type !== "application/pdf" && !file.name.toLowerCase().endsWith(".pdf")) {
      showError("Please choose a PDF file.");
      return;
    }
    if (file.size > MAX_BYTES) {
      showError("The PDF must be 2 MB or smaller.");
      return;
    }

    setUploading(true);
    setMessage({ type: "", text: "" });
    try {
      const saved = await profileApi.uploadResume(file);
      setResume({ hasFile: saved.hasResumeFile, name: saved.resumeFileName });
      setCompleteness(saved.completeness);
      setMessage({ type: "success", text: "Resume uploaded." });
    } catch (err) {
      showError(err.message || "The resume could not be uploaded.");
    } finally {
      setUploading(false);
    }
  }

  async function handleRemove() {
    setUploading(true);
    setMessage({ type: "", text: "" });
    try {
      const saved = await profileApi.removeResume();
      setResume({ hasFile: saved.hasResumeFile, name: saved.resumeFileName });
      setCompleteness(saved.completeness);
      setMessage({ type: "success", text: "Resume file removed." });
    } catch (err) {
      showError(err.message || "The resume could not be removed.");
    } finally {
      setUploading(false);
    }
  }

  async function handleView() {
    try {
      await profileApi.viewMyResume();
    } catch (err) {
      showError(err.message || "The resume could not be opened.");
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
          <div className="resume-box">
            <div className="resume-info">
              <strong>Resume (PDF)</strong>
              {resume.hasFile
                ? <span className="resume-file">📄 {resume.name}</span>
                : <span className="muted-text">No file uploaded. PDF only, up to 2 MB.</span>}
            </div>
            <div className="resume-actions">
              {resume.hasFile && (
                <>
                  <button type="button" className="text-button" onClick={handleView}>View</button>
                  <button type="button" className="delete-button" onClick={handleRemove} disabled={uploading}>Remove</button>
                </>
              )}
              <input ref={fileInput} type="file" accept="application/pdf,.pdf" onChange={handleFile} hidden />
              <button type="button" className="button button-secondary small" disabled={uploading}
                      onClick={() => fileInput.current?.click()}>
                {uploading ? "Please wait…" : resume.hasFile ? "Replace PDF" : "Upload PDF"}
              </button>
            </div>
          </div>

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
            <label>Resume link (optional if you uploaded a PDF)
              <input type="url" value={form.resumeUrl} onChange={set("resumeUrl")} maxLength={500}
                     placeholder="https://drive.google.com/…" />
            </label>

            <div className="field full">
              <span className="field-label">Skills (these decide your job match scores)</span>
              <SkillsInput value={skills} onChange={setSkills} />
            </div>

            <label className="full">About you
              <textarea value={form.about} onChange={set("about")} maxLength={500}
                        placeholder="A few lines about what you are learning and what you want to do." />
              <span className="char-count">{form.about.length} / 500</span>
            </label>

            <p className="privacy-note full">
              The PDF upload saves immediately. The other fields are saved with the button below.
              A resume link must be public (Google Drive, OneDrive, LinkedIn).
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