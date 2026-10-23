import { formatStatus } from "../utils/format";

const ORDER = ["APPLIED", "SHORTLISTED", "SELECTED"];
export default function ApplicationTracker({ status }) {
  let steps;
  if (status === "REJECTED") {
    steps = [{ label: "Applied", state: "done" }, { label: "Rejected", state: "bad" }];
  } else if (status === "WITHDRAWN") {
    steps = [{ label: "Applied", state: "done" }, { label: "Withdrawn", state: "muted" }];
  } else {
    const current = ORDER.indexOf(status);
    steps = ORDER.map((s, i) => ({
      label: formatStatus(s),
      state: i < current || status === "SELECTED" ? "done" : i === current ? "current" : "todo",
    }));
  }

  return (
    <ol className="tracker" aria-label="Application progress">
      {steps.map((step) => (
        <li key={step.label} className={`step ${step.state}`}>
          <span className="dot" />{step.label}
        </li>
      ))}
    </ol>
  );
}