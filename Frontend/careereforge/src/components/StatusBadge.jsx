import { formatStatus } from "../utils/format";

export default function StatusBadge({status}){
    return <span className={`app-badge ${status.toLowerCase()}`}>{formatStatus(status)}</span>
}