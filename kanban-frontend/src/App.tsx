import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/layout/AppLayout";
import Dashboard from "./pages/Dashboard";
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

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/find-id" element={<FindIdPage />} />
      <Route path="/find-password" element={<FindPasswordPage />} />
      <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

      <Route element={<AppLayout />}>
        <Route index element={<Dashboard />} />
        <Route path="/kanban" element={<KanbanPage />} />
        <Route path="/timeline" element={<TimelinePage />} />
        <Route path="/members" element={<MembersPage />} />
        <Route path="/files" element={<FilesPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
