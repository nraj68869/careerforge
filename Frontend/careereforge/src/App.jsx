import { useCallback, useEffect, useState } from "react";
import { useRecoilState, useResetRecoilState, useSetRecoilState } from "recoil";
import { authApi } from "./api/auth";
import { AUTH_EXPIRED_EVENT } from "./api/client";
import { tokenStore } from "./api/tokenStore";
import AuthPage from "./components/AuthPage";
import { AppContext } from "./context/AppContext";
import { useHashRoute } from "./hooks/useHashRoute";
import AdminDashboard from "./pages/AdminDashboard";
import JobsPage from "./pages/JobsPage";
import MyJobsPage from "./pages/MyJobsPage";
import ProfilePage from "./pages/ProfilePage";
import { authReadyState, currentUserState } from "./state/authState";
import PendingApprovalPage from "./pages/PendingApprovalPage";
import StudentProfilePage from "./pages/StudentProfilePage";
import {
  noticeState,
  pageState,
  reloadKeyState,
  searchInputState,
  searchState,
} from "./state/userState";
import { resolvePage } from "./utils/pages";
import "./App.css";
import ApplicationsPage from "./pages/ApplicationsPage";
import MyApplicationsPage from "./pages/MyApplicationsPage";

function App() {
  const [user, setUser] = useRecoilState(currentUserState);
  const [ready, setReady] = useRecoilState(authReadyState);
  const [sessionMessage, setSessionMessage] = useState("");
  const [route, navigate] = useHashRoute();

  const resetSearchInput = useResetRecoilState(searchInputState);
  const resetSearch = useResetRecoilState(searchState);
  const resetPage = useResetRecoilState(pageState);
  const resetNotice = useResetRecoilState(noticeState);
  const setReloadKey = useSetRecoilState(reloadKeyState);

  const clearSession = useCallback((message = "") => {
    tokenStore.clear();
    setUser(null);
    resetSearchInput();
    resetSearch();
    resetPage();
    resetNotice();
    setReloadKey((key) => key + 1);
    setSessionMessage(message);
    navigate("");                       // the next person starts at their own home page
  }, [setUser, resetSearchInput, resetSearch, resetPage, resetNotice, setReloadKey, navigate]);

  useEffect(() => {
    const onExpired = () => clearSession("Your session expired. Please log in again.");
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired);
  }, [clearSession]);

  useEffect(() => {
    let cancelled = false;
    async function restoreSession() {
      if (!tokenStore.get()) {
        setReady(true);
        return;
      }
      try {
        const me = await authApi.me();
        if (!cancelled) setUser(me);
      } catch {
        tokenStore.clear();
      } finally {
        if (!cancelled) setReady(true);
      }
    }
    restoreSession();
    return () => { cancelled = true; };
  }, [setReady, setUser]);

  async function handleLogout() {
    await authApi.logout();
    clearSession("");
  }

  function handleLoggedIn(loggedInUser) {
    setSessionMessage("");
    setUser(loggedInUser);
  }

  function handlePasswordChanged() {
    clearSession("Password changed. Please log in again.");
  }

  if (!ready) return <div className="auth-loading">Loading…</div>;
  if (!user) return <AuthPage onLoggedIn={handleLoggedIn} message={sessionMessage} />;
  if (user.role === "RECRUITER" && user.approvalStatus !== "APPROVED") {
  return <PendingApprovalPage onLogout={handleLogout} />;
}
  const page = resolvePage(route, user.role);
  const context = { user, page, navigate, logout: handleLogout };

    let content;
  if (page === "dashboard") {
    content = <AdminDashboard onLogout={handleLogout} onOpenAccount={() => navigate("account")} />;
  } else if (page === "jobs") {
    content = <JobsPage />;
  } else if (page === "my-jobs") {
    content = <MyJobsPage />;
  } else if (page === "my-applications") {
    content = <MyApplicationsPage />;
  } else if (page === "applications") {
    content = <ApplicationsPage />;
    } else if (page === "profile") {
  content = <StudentProfilePage />;
  } else {
    content = <ProfilePage onPasswordChanged={handlePasswordChanged} />;
  }

  return <AppContext.Provider value={context}>{content}</AppContext.Provider>;
}

export default App;