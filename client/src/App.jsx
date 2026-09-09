import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import BoardListPage from './pages/BoardListPage';
import BoardDetailPage from './pages/BoardDetailPage';
import ProtectedRoute from './components/ProtectedRoute';
import { useAuthStore } from './stores/authStore';
import { fetchMe } from './api/authApi';

export default function App() {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const setAuth = useAuthStore((s) => s.setAuth);
  const setStatus = useAuthStore((s) => s.setStatus);
  const logout = useAuthStore((s) => s.logout);

  useEffect(() => {
    if (!token) {
      setStatus('ready');
      return;
    }
    setStatus('checking');
    fetchMe()
      .then(({ user: freshUser }) => setAuth(token, freshUser))
      .catch(() => logout());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<BoardListPage />} />
          <Route path="/boards/:boardId" element={<BoardDetailPage />} />
        </Route>
        <Route path="*" element={<Navigate to={token && user ? '/' : '/login'} replace />} />
      </Routes>
    </BrowserRouter>
  );
}
