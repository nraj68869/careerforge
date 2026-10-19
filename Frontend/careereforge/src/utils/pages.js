// which role may open which page
const ALLOWED = {
  dashboard: ["ADMIN"],
  jobs: ["STUDENT", "PLACEMENT_OFFICER", "ADMIN"],
  "my-jobs": ["RECRUITER", "PLACEMENT_OFFICER", "ADMIN"],
  account: ["STUDENT", "RECRUITER", "PLACEMENT_OFFICER", "ADMIN"],
};

const DEFAULT_PAGE = {
  ADMIN: "dashboard",
  STUDENT: "jobs",
  PLACEMENT_OFFICER: "jobs",
  RECRUITER: "my-jobs",
};

// If the URL asks for a page this role may not open, fall back to their home page
export function resolvePage(route, role) {
  return ALLOWED[route]?.includes(role) ? route : DEFAULT_PAGE[role] ?? "account";
}