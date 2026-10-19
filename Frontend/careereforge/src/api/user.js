import { request } from "./client";

export const usersApi = {
  list({ page = 0, size = 10, search = "" } = {}) {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    const query = search.trim();
    if (query) params.set("name", query);
    const path = query ? `/users/search?${params}` : `/users?${params}`;
    return request(path);
  },
  create(payload) {
    return request("/users", { method: "POST", body: JSON.stringify(payload) });
  },
  getById(id) {
    return request(`/users/${id}`);
  },
  update(id, payload) {
    return request(`/users/${id}`, { method: "PUT", body: JSON.stringify(payload) });
  },
  changeRole(id, role) {
    return request(`/users/${id}/role`, { method: "PUT", body: JSON.stringify({ role }) });
  },
  setEnabled(id, enabled) {
    return request(`/users/${id}/enabled`, { method: "PUT", body: JSON.stringify({ enabled }) });
  },
  remove(id) {
    return request(`/users/${id}`, { method: "DELETE" });
  },
  stats() {
  return request("/users/stats");
},
};