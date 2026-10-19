import {useRecoilValueLoadable} from "recoil"
import { usersQuery } from "./userState";

export function useUsers(){
    const loadable = useRecoilValueLoadable(usersQuery); //load the user state value

    const data = loadable.state === "hasValue" ? loadable.contents: null; //check if 

    return {
        loading: loadable.state === "loading", //if loading
        error: //if error happens
            loadable.state === "hasError" ? loadable.contents?.message ||"We couldn't load the student directory."
            : "",
        users:data?.content ?? [], ///successfully loaded
        pageInfo: {
            totalElements:data?.totalElements ?? 0,
            totalPages: data?.totalPages ?? 0,
            size: data?.size?? 10,
        },
    };
}