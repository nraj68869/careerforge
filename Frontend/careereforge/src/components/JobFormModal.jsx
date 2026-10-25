import { useEffect, useState } from "react";
import { JOB_TYPES, formatType, todayIso } from "../utils/format";
import SkillsInput from "./SkillsInput";

const EMPTY = {
  title: "", company: "", location: "", type: "FULL_TIME",
  description: "", salaryMin: "", salaryMax: "", deadline: "", minCgpa: "",
};

export default function JobFormModal({ job, onSave, onClose }) {
  const [form, setForm] = useState(job ? {
    title: job.title, company: job.company, location: job.location, type: job.type,
    description: job.description, salaryMin: job.salaryMin ?? "", salaryMax: job.salaryMax ?? "",
    deadline: job.deadline, minCgpa: job.minCgpa ?? "",
  } : EMPTY);
  const [skills, setSkills] = useState(job?.requiredSkills ?? []);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const onKey = (e) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    const min = form.salaryMin === "" ? null : Number(form.salaryMin);
    const max = form.salaryMax === "" ? null : Number(form.salaryMax);
    if (min != null && max != null && max < min) {
      setError("Maximum salary cannot be lower than minimum salary.");
      return;
    }

    setSaving(true);
    try {
      await onSave({
        title: form.title.trim(),
        company: form.company.trim(),
        location: form.location.trim(),
        type: form.type,
        description: form.description.trim(),
        salaryMin: min,
        salaryMax: max,
        deadline: form.deadline,
        requiredSkills: skills,
        minCgpa: form.minCgpa === "" ? null : Number(form.minCgpa),
      });
    } catch (err) {
      setError(err.message || "The job could not be saved.");
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop">
      <div className="modal-card wide" role="dialog" aria-modal="true">
        <h3>{job ? "Edit job" : "Post a job"}</h3>
        <p>{job ? "Update the details of this job." : "Students will see it as soon as you post it."}</p>

        {error && <div className="alert alert-error" role="alert"><span>{error}</span></div>}

        <form className="job-form" onSubmit={handleSubmit}>
          <label className="full">Job title
            <input value={form.title} onChange={set("title")} maxLength={150} placeholder="e.g. Java Backend Intern" required />
          </label>
          <label>Company
            <input value={form.company} onChange={set("company")} maxLength={150} required />
          </label>
          <label>Location
            <input value={form.location} onChange={set("location")} maxLength={150} placeholder="e.g. Dehradun or Remote" required />
          </label>
          <label>Job type
            <select value={form.type} onChange={set("type")}>
              {JOB_TYPES.map((t) => <option key={t} value={t}>{formatType(t)}</option>)}
            </select>
          </label>
          <label>Apply before
            <input type="date" value={form.deadline} onChange={set("deadline")} min={todayIso()} required />
          </label>
          <label>Minimum salary (₹ / year, optional)
            <input type="number" min="0" value={form.salaryMin} onChange={set("salaryMin")} />
          </label>
          <label>Maximum salary (₹ / year, optional)
            <input type="number" min="0" value={form.salaryMax} onChange={set("salaryMax")} />
          </label>

          <div className="field full">
            <span className="field-label">Required skills (used to calculate each student's match)</span>
            <SkillsInput value={skills} onChange={setSkills} max={15} />
          </div>
          <label>Minimum CGPA (optional)
            <input type="number" min="0" max="10" step="0.01" value={form.minCgpa} onChange={set("minCgpa")}
                   placeholder="e.g. 7.5" />
          </label>

          <label className="full">Description
            <textarea value={form.description} onChange={set("description")} minLength={20} maxLength={5000}
                      placeholder="What will the person do? What skills are needed?" required />
            <span className="char-count">{form.description.length} / 5000 (minimum 20)</span>
          </label>

          <div className="modal-actions">
            <button type="button" className="button button-secondary" onClick={onClose}>Cancel</button>
            <button className="button button-primary" disabled={saving}>
              {saving ? "Saving…" : job ? "Save changes" : "Post job"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}