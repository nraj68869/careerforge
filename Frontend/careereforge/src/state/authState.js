import { atom } from "recoil";

export const currentUserState = atom({ key: "currentUser", default: null });  // who is logged in
export const authReadyState = atom({ key: "authReady", default: false });     // finished checking the saved token?