import { AdminDashboard } from "@/components/dashboard/admin-dashboard";
import { DashboardErrorBoundary } from "@/components/dashboard/error-boundary";

export default function Home() {
  return (
    <DashboardErrorBoundary>
      <AdminDashboard />
    </DashboardErrorBoundary>
  );
}
