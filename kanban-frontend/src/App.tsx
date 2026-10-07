import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/layout/AppLayout";
import Dashboard from "./pages/Dashboard";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/auth/LoginPage";
import SignupPage from "./pages/auth/SignupPage";
import FindIdPage from "./pages/auth/FindIdPage";
import FindPasswordPage from "./pages/auth/FindPasswordPage";
import OAuthCallbackPage from "./pages/auth/OAuthCallbackPage";
import KanbanPage from "./pages/kanban/KanbanPage";
import TimelinePage from "./pages/timeline/TimelinePage";
import MembersPage from "./pages/members/MembersPage";
import FilesPage from "./pages/files/FilesPage";
import SettingsPage from "./pages/settings/SettingsPage";
import PrivacyPage from "./pages/PrivacyPage";

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/find-id" element={<FindIdPage />} />
      <Route path="/find-password" element={<FindPasswordPage />} />
      <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

      <Route path="/privacy" element={<PrivacyPage />} />

      <Route path="/" element={<HomePage />} />

      {/* 한 사람이 여러 프로젝트를 가질 수 있어서, 보드/타임라인/팀원 등은 모두 특정 프로젝트 하위 경로다. */}
      <Route path="/projects/:projectId" element={<AppLayout />}>
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="kanban" element={<KanbanPage />} />
        <Route path="timeline" element={<TimelinePage />} />
        <Route path="members" element={<MembersPage />} />
        <Route path="files" element={<FilesPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
