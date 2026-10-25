export function formatRole(role = "STUDENT") {
  return role.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (c) => c.toUpperCase());
}

const TYPE_LABELS = {
  FULL_TIME: "Full-time",
  INTERNSHIP: "Internship",
  PART_TIME: "Part-time",
  CONTRACT: "Contract",
};
const STATUS_LABELS = {
  APPLIED: "Applied",
  SHORTLISTED: "Shortlisted",
  SELECTED: "Selected",
  REJECTED: "Rejected",
  WITHDRAWN: "Withdrawn",
};
export const JOB_TYPES = Object.keys(TYPE_LABELS);
export const formatType = (type) => TYPE_LABELS[type] ?? type;
export const STATUS_OPTIONS = Object.keys(STATUS_LABELS);
export const formatStatus = (status) => STATUS_LABELS[status] ?? status;

const inr = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

export function formatSalary(min, max) {
  if (min == null && max == null) return "Not disclosed";
  if (min != null && max != null) return `${inr.format(min)} – ${inr.format(max)} / year`;
  if (min != null) return `From ${inr.format(min)} / year`;
  return `Up to ${inr.format(max)} / year`;
}

function parseDate(iso) {
  return new Date(`${iso}T00:00:00`);   // local midnight, avoids timezone shifts
}

export function formatDate(iso) {
  return parseDate(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

export function daysLeft(iso) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((parseDate(iso) - today) / 86400000);
}

export function deadlineText(iso) {
  const d = daysLeft(iso);
  if (d < 0) return "Deadline passed";
  if (d === 0) return "Closes today";
  if (d === 1) return "Closes tomorrow";
  return `${d} days left`;
}

export function todayIso() {
  const d = new Date();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${month}-${day}`;
}

export function initials(name = "") {
  return name.trim().split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? "").join("");
}
export function safeUrl(url) {
  return typeof url === "string" && /^https?:\/\//i.test(url.trim()) ? url.trim() : null;
}

export function timeAgo(iso) {
  const seconds = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days} d ago`;
  return formatDate(iso.slice(0, 10));
}