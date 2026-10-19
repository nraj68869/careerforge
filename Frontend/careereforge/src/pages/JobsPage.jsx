import AppShell from "../components/AppShell";
import JobBoard from "../components/JobBoard";

export default function JobsPage() {
  return (
    <AppShell title="Jobs">
      <JobBoard mode="browse" />
    </AppShell>
  );
}