import { request } from "./client";

// turns {q:"java", type:""} into "q=java" and skips empty values
function toQuery(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== "" && value != null) search.set(key, String(value));
  });
  return search.toString();
}

export const jobsApi = {
  browse(params) { return request(`/jobs?${toQuery(params)}`); },
  mine(params) { return request(`/jobs/mine?${toQuery(params)}`); },
  get(id) { return request(`/jobs/${id}`); },
  create(payload) { return request("/jobs", { method: "POST", body: JSON.stringify(payload) }); },
  update(id, payload) { return request(`/jobs/${id}`, { method: "PUT", body: JSON.stringify(payload) }); },
  setStatus(id, status) { return request(`/jobs/${id}/status`, { method: "PUT", body: JSON.stringify({ status }) }); },
  remove(id) { return request(`/jobs/${id}`, { method: "DELETE" }); },
};