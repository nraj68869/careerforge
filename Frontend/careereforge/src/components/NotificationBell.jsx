import { useCallback, useEffect, useRef, useState } from "react";
import { notificationsApi } from "../api/notifications";
import { timeAgo } from "../utils/format";
import Icon from "./Icon";

const POLL_MS = 30_000;

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [count, setCount] = useState(0);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const box = useRef(null);

  const refreshCount = useCallback(async () => {
    try {
      const result = await notificationsApi.unreadCount();
      setCount(result.count);
    } catch {
      /* the badge simply stays as it was */
    }
  }, []);

  // check for new notifications every 30 seconds, and when you come back to the tab
  useEffect(() => {
    refreshCount();
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") refreshCount();
    }, POLL_MS);
    const onVisible = () => { if (document.visibilityState === "visible") refreshCount(); };
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [refreshCount]);

  // close on outside click or Escape
  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e) => { if (box.current && !box.current.contains(e.target)) setOpen(false); };
    const onKey = (e) => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  async function loadItems() {
    setLoading(true);
    setError("");
    try {
      const page = await notificationsApi.list({ page: 0, size: 10 });
      setItems(page.content);
    } catch (err) {
      setError(err.message || "Notifications could not be loaded.");
    } finally {
      setLoading(false);
    }
  }

  function toggle() {
    const next = !open;
    setOpen(next);
    if (next) {
      loadItems();
      refreshCount();
    }
  }

  async function openItem(n) {
    setOpen(false);
    if (!n.read) {
      try { await notificationsApi.markRead(n.id); } catch { /* ignore */ }
      refreshCount();
    }
    if (n.route) window.location.hash = `/${n.route}`;   // pages a role may not open fall back to home
  }

  async function markAll() {
    try {
      await notificationsApi.markAllRead();
      setItems((list) => list.map((n) => ({ ...n, read: true })));
      setCount(0);
    } catch (err) {
      setError(err.message || "Could not mark them as read.");
    }
  }

  return (
    <div className="bell-wrap" ref={box}>
      <button className="bell-button" onClick={toggle} aria-expanded={open} aria-haspopup="true"
              aria-label={count ? `Notifications, ${count} unread` : "Notifications"}>
        <Icon name="bell" size={18} />
        {count > 0 && <span className="bell-badge">{count > 9 ? "9+" : count}</span>}
      </button>

      {open && (
        <div className="bell-panel" role="dialog" aria-label="Notifications">
          <div className="bell-head">
            <strong>Notifications</strong>
            {count > 0 && <button className="text-button" onClick={markAll}>Mark all as read</button>}
          </div>

          {error && <div className="bell-empty">{error}</div>}
          {loading && items.length === 0 && <div className="bell-empty">Loading…</div>}
          {!loading && !error && items.length === 0 && <div className="bell-empty">You are all caught up.</div>}

          {items.length > 0 && (
            <ul className="bell-list">
              {items.map((n) => (
                <li key={n.id}>
                  <button className={`bell-item${n.read ? "" : " unread"}`} onClick={() => openItem(n)}>
                    <span className="bell-dot" />
                    <span className="bell-text">
                      <strong>{n.title}</strong>
                      <span>{n.message}</span>
                      <time dateTime={n.createdAt}>{timeAgo(n.createdAt)}</time>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}