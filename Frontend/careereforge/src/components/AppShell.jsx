import { useApp } from "../context/AppContext";
import { formatRole } from "../utils/format";
import Sidebar from "./Sidebar";

export default function AppShell({ title, children }) {
  const { user } = useApp();

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <header className="topbar">
          <div className="breadcrumbs">
            <span>Workspace</span><span className="crumb-slash">/</span><strong>{title}</strong>
          </div>
          <a className="user-chip" href="#/account" title="My account">
            <span className="chip-avatar">{user.name.charAt(0).toUpperCase()}</span>
            <span className="chip-copy"><strong>{user.name}</strong><span>{formatRole(user.role)}</span></span>
          </a>
        </header>
        <div className="page-content">{children}</div>
      </main>
    </div>
  );
}