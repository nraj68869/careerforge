import { useState } from "react";

const MAX_SKILLS = 20;
const MAX_LENGTH = 40;

export default function SkillsInput({ value, onChange }) {
  const [draft, setDraft] = useState("");
  const [hint, setHint] = useState("");

  function add(raw) {
    const skill = raw.trim().replace(/\s+/g, " ");
    if (!skill) return;
    if (skill.length > MAX_LENGTH) {
      setHint(`A skill can have at most ${MAX_LENGTH} characters.`);
      return;
    }
    if (value.some((s) => s.toLowerCase() === skill.toLowerCase())) {
      setDraft("");
      setHint("");
      return;
    }
    if (value.length >= MAX_SKILLS) {
      setHint(`You can add up to ${MAX_SKILLS} skills.`);
      return;
    }
    onChange([...value, skill]);
    setDraft("");
    setHint("");
  }

  function onKeyDown(e) {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      add(draft);
    } else if (e.key === "Backspace" && draft === "" && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  }

  return (
    <div>
      <div className="skills-box">
        {value.map((skill) => (
          <span className="skill-chip" key={skill}>
            {skill}
            <button type="button" onClick={() => onChange(value.filter((s) => s !== skill))}
                    aria-label={`Remove ${skill}`}>×</button>
          </span>
        ))}
        <input value={draft} onChange={(e) => setDraft(e.target.value)} onKeyDown={onKeyDown}
               onBlur={() => add(draft)}
               placeholder={value.length ? "Add another…" : "Type a skill and press Enter"} />
      </div>
      <span className="char-count">{value.length} / {MAX_SKILLS} skills{hint && ` · ${hint}`}</span>
    </div>
  );
}