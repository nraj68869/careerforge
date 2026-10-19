import { createContext, useContext } from "react";

export const AppContext = createContext(null);

// gives any component: user, page, navigate, logout
export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used inside <AppContext.Provider>");
  return ctx;
}