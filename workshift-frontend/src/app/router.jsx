import { Navigate, createBrowserRouter } from 'react-router-dom'
import { RequireAuth } from '../features/auth/RequireAuth'
import { AppLayout } from '../layouts/AppLayout'
import { GroupLayout } from '../layouts/GroupLayout'
import { PublicLayout } from '../layouts/PublicLayout'
import { AuditLogsPage } from '../pages/AuditLogsPage'
import { CreateGroupPage } from '../pages/CreateGroupPage'
import { GroupHomePage } from '../pages/GroupHomePage'
import { GroupsPage } from '../pages/GroupsPage'
import { JoinGroupPage } from '../pages/JoinGroupPage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { MembersPage } from '../pages/MembersPage'
import { PendingMembersPage } from '../pages/PendingMembersPage'
import { PositionsPage } from '../pages/PositionsPage'
import { RegisterPage } from '../pages/RegisterPage'
import { ShiftTemplatesPage } from '../pages/ShiftTemplatesPage'
import { ShiftsPage } from '../pages/ShiftsPage'
import { GroupSettingsPage } from '../pages/GroupSettingsPage'
import { AvailabilityPage } from '../pages/AvailabilityPage'
import { MySchedulePage } from '../pages/MySchedulePage'
import { ProfilePage } from '../pages/ProfilePage'
import { SalaryConfigPage } from '../pages/SalaryConfigPage'
import { PayrollPage } from '../pages/PayrollPage'
import { PerformancePage } from '../pages/PerformancePage'
import { AlertsPage } from '../pages/AlertsPage'
import { ShiftChangeRequestsPage } from '../pages/ShiftChangeRequestsPage'
import { RequireAdmin } from '../features/auth/RequireAdmin'
import { AdminLayout } from '../layouts/AdminLayout'
import { AdminDashboardPage } from '../pages/AdminDashboardPage'
import { AdminUsersPage } from '../pages/AdminUsersPage'
import { AdminGroupsPage } from '../pages/AdminGroupsPage'
import { AdminAuditLogsPage } from '../pages/AdminAuditLogsPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/app/groups" replace />,
  },
  {
    path: '/auth',
    element: <PublicLayout />,
    children: [
      { index: true, element: <Navigate to="/auth/login" replace /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
    ],
  },
  {
    path: '/app',
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <Navigate to="/app/groups" replace /> },
      { path: 'groups', element: <GroupsPage /> },
      { path: 'groups/create', element: <CreateGroupPage /> },
      { path: 'groups/join', element: <JoinGroupPage /> },
    ],
  },
  {
    path: '/groups/:groupId',
    element: (
      <RequireAuth>
        <GroupLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <GroupHomePage /> },
      { path: 'members', element: <MembersPage /> },
      { path: 'members/pending', element: <PendingMembersPage /> },
      { path: 'shift-change-requests', element: <ShiftChangeRequestsPage /> },
      { path: 'positions', element: <PositionsPage /> },
      { path: 'shift-templates', element: <ShiftTemplatesPage /> },
      { path: 'shifts', element: <ShiftsPage /> },
      { path: 'availability', element: <AvailabilityPage /> },
      { path: 'my-schedule', element: <MySchedulePage /> },
      { path: 'audit-logs', element: <AuditLogsPage /> },
      { path: 'profile', element: <ProfilePage /> },
      { path: 'salary-configs', element: <SalaryConfigPage /> },
      { path: 'payroll', element: <PayrollPage /> },
      { path: 'performance', element: <PerformancePage /> },
      { path: 'alerts', element: <AlertsPage /> },
      { path: 'settings', element: <GroupSettingsPage /> },
    ],
  },
  {
    path: '/admin',
    element: (
      <RequireAdmin>
        <AdminLayout />
      </RequireAdmin>
    ),
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: 'users', element: <AdminUsersPage /> },
      { path: 'groups', element: <AdminGroupsPage /> },
      { path: 'audit-logs', element: <AdminAuditLogsPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
