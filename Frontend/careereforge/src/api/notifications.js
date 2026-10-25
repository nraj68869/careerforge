import { request } from "./client";

export const notificationsApi = {
  list({ page = 0, size = 10, unreadOnly = false } = {}) {
    return request(`/notifications?page=${page}&size=${size}&unreadOnly=${unreadOnly}`);
  },
  unreadCount() { return request("/notifications/unread-count"); },
  markRead(id) { return request(`/notifications/${id}/read`, { method: "PUT" }); },
  markAllRead() { return request("/notifications/read-all", { method: "PUT" }); },
};