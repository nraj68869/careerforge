import AppShell from "../components/AppShell";
import JobBoard from "../components/JobBoard";

export default function MyJobsPage() {
  return (
    <AppShell title="My jobs">
      <JobBoard mode="mine" />
    </AppShell>
  );
}