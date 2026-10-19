import { useCallback, useEffect, useState } from "react";

function readRoute() {
  return window.location.hash.replace(/^#\/?/, "");
}

export function useHashRoute() {
  const [route, setRoute] = useState(readRoute);

  useEffect(() => {
    const onChange = () => setRoute(readRoute());
    window.addEventListener("hashchange", onChange);
    return () => window.removeEventListener("hashchange", onChange);
  }, []);

  const navigate = useCallback((to) => {
    window.location.hash = "/" + to;
  }, []);

  return [route, navigate];
}