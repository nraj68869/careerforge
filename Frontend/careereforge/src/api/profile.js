import { request } from "./client";

export const profileApi = {
  mine() { return request("/profile/me"); },
  saveMine(payload) { return request("/profile/me", { method: "PUT", body: JSON.stringify(payload) }); },
  forStudent(studentId) { return request(`/students/${studentId}/profile`); },
};