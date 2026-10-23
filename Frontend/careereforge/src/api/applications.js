import { request } from "./client";

function toQuery(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== "" && value != null) search.set(key, String(value));
  });
  return search.toString();
}

export const applicationsApi = {
  apply(jobId, coverNote) {
    return request(`/jobs/${jobId}/applications`, { method: "POST", body: JSON.stringify({ coverNote }) });
  },
  mine(params) { return request(`/applications/mine?${toQuery(params)}`); },
  withdraw(id) { return request(`/applications/${id}/withdraw`, { method: "PUT" }); },
  forJob(jobId, params) { return request(`/jobs/${jobId}/applications?${toQuery(params)}`); },
  all(params) { return request(`/applications?${toQuery(params)}`); },
  updateStatus(id, status) {
    return request(`/applications/${id}/status`, { method: "PUT", body: JSON.stringify({ status }) });
  },
  stats() { return request("/applications/stats"); },
};