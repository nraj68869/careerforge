import { useApp } from "../context/AppContext";

const NAV = [
  { key: "dashboard", label: "Dashboard", icon: "▦", roles: ["ADMIN"] },
  { key: "jobs", label: "Jobs", icon: "▤", roles: ["STUDENT", "PLACEMENT_OFFICER", "ADMIN"] },
  { key: "my-jobs", label: "My jobs", icon: "✎", roles: ["RECRUITER", "PLACEMENT_OFFICER", "ADMIN"] },
  { key: "my-applications", label: "My applications", icon: "☑", roles: ["STUDENT"] },
  { key: "profile", label: "My profile", icon: "☺", roles: ["STUDENT"] },
  { key: "applications", label: "Applications", icon: "↗", roles: ["PLACEMENT_OFFICER", "ADMIN"] },
  { key: "account", label: "My account", icon: "♙", roles: ["STUDENT", "RECRUITER", "PLACEMENT_OFFICER", "ADMIN"] },
];

function Sidebar() {
  const { user, page, logout } = useApp();
  const items = NAV.filter((item) => item.roles.includes(user.role));

  return (
    <aside className="sidebar">
      <a className="brand" href="#/" aria-label="CareerForge home">
        <span className="brand-mark">C</span>
        <span className="brand-name">career<span>forge</span></span>
      </a>

      <div className="workspace-label">WORKSPACE</div>
      <nav className="side-nav" aria-label="Main navigation">
        {items.map((item) => (
          <a key={item.key} href={`#/${item.key}`} className={`nav-item${page === item.key ? " active" : ""}`}>
            <span className="nav-icon">{item.icon}</span> {item.label}
          </a>
        ))}
      </nav>

      <div className="sidebar-bottom">
        <div className="profile-mini">
          <div className="avatar avatar-dark">{user.name.charAt(0).toUpperCase()}</div>
          <div className="profile-copy"><strong>{user.name}</strong><span>{user.role}</span></div>
        </div>
        <button className="button logout-button" onClick={logout}>Log out</button>
      </div>
    </aside>
  );
}

export default Sidebar;