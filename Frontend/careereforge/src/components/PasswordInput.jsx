import { useState } from "react";
import Icon from "./Icon";

export default function PasswordInput({
  value, onChange, autoComplete, minLength, maxLength = 72, icon, placeholder,
}) {
  const [visible, setVisible] = useState(false);

  return (
    <div className={`password-field${icon ? " has-icon" : ""}`}>
      {icon && <span className="input-icon"><Icon name={icon} /></span>}
      <input
        type={visible ? "text" : "password"}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete}
        minLength={minLength}
        maxLength={maxLength}
        placeholder={placeholder}
        required
      />
      <button
        type="button"
        className="password-toggle"
        onClick={() => setVisible((v) => !v)}
        aria-label={visible ? "Hide password" : "Show password"}
        aria-pressed={visible}
      >
        {visible ? "Hide" : "Show"}
      </button>
    </div>
  );
}