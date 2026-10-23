import { useEffect, useState } from "react";
import { profileApi } from "../api/profile";

export default function ProfileNudge() {
  const [completeness, setCompleteness] = useState(null);

  useEffect(() => {
    let cancelled = false;
    profileApi.mine()
      .then((p) => { if (!cancelled) setCompleteness(p.completeness); })
      .catch(() => { /* the banner just does not show */ });
    return () => { cancelled = true; };
  }, []);

  if (completeness === null || completeness >= 70) return null;

  return (
    <div className="nudge">
      <div>
        <strong>Your profile is {completeness}% complete.</strong>
        <span> Recruiters see it when you apply. A full profile helps you stand out.</span>
      </div>
      <a className="button button-primary small" href="#/profile">Complete profile</a>
    </div>
  );
}