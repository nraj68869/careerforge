import { request, requestBlob } from "./client";
import { saveBlob } from "../utils/download";

// The PDF needs the login token, so it is fetched with the token and opened as a temporary local file.
async function openPdf(path) {
  const win = window.open("", "_blank");   // opened inside the click, so popup blockers allow it
  try {
    const blob = await requestBlob(path);
    if (win) {
      const url = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
      win.location.href = url;
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } else {
      saveBlob(blob, "resume.pdf");        // popup blocked: download instead
    }
  } catch (err) {
    if (win) win.close();
    throw err;
  }
}

export const profileApi = {
  mine() { return request("/profile/me"); },
  saveMine(payload) { return request("/profile/me", { method: "PUT", body: JSON.stringify(payload) }); },
  forStudent(studentId) { return request(`/students/${studentId}/profile`); },

  uploadResume(file) {
    const body = new FormData();
    body.append("file", file);
    return request("/profile/me/resume", { method: "POST", body });
  },
  removeResume() { return request("/profile/me/resume", { method: "DELETE" }); },
  viewMyResume() { return openPdf("/profile/me/resume"); },
  viewStudentResume(studentId) { return openPdf(`/students/${studentId}/resume`); },
};