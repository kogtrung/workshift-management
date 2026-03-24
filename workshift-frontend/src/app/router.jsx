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
import { RegisterPage } from '../pages/RegisterPage'

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
      { path: 'audit-logs', element: <AuditLogsPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
