import { request } from "./client";
import { tokenStore } from "./tokenStore";

export const authApi = {
  async login(email, password) {
    const data = await request("/auth/login", {
      method: "POST",
      auth: false,
      body: JSON.stringify({ email, password }),
    });
    tokenStore.set(data.token);
    return data.user;
  },
  changePassword(currentPassword, newPassword) {
  return request("/auth/password", {
    method: "PUT",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
},

  register(name, email, password, role) {
    return request("/auth/register", {
      method: "POST",
      auth: false,
      body: JSON.stringify({ name, email, password, role }),
    });
  },

  me() {
    return request("/auth/me");
  },

  async logout() {
    try {
      await request("/auth/logout", { method: "POST" });   // tells the server to cancel the token
    } catch {
      /* even if the server is unreachable, we still forget the token below */
    } finally {
      tokenStore.clear();
    }
  },
};