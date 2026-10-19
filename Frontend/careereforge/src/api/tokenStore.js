const KEY = "careerforge_token";

export const tokenStore = {
  get() {
    try { return localStorage.getItem(KEY); } catch { return null; }
  },
  set(token) {
    try { localStorage.setItem(KEY, token); } catch { /* storage blocked */ }
  },
  clear() {
    try { localStorage.removeItem(KEY); } catch { /* storage blocked */ }
  },
};