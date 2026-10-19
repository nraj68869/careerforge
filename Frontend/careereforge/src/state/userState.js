import {atom,selector} from "recoil";
import { usersApi } from "../api/user";

export const searchInputState = atom({key : "searchInput", default:""}); //what user types in search box

export const searchState = atom({key: "search", default:""}); //The debounced search sent to api

export const pageState = atom({key: "pageState", default:0}); //current page of student table

export const reloadKeyState = atom({key:"realoadKey", default: 0}); //Bump this no. for the refresh the state

export const noticeState = atom({key: "noticeState", default:""});

export const usersQuery = selector({
    key: "usersQuery",
    get: async ({get})  =>{
        const page = get(pageState);
        const search = get(searchState);
        get(reloadKeyState);
        return usersApi.list({page,size : 10, search});
    },
    cachePolicy_UNSTABLE: { eviction: "most-recent" },
});
