import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import * as boardApi from '../api/boardApi';

export default function BoardListPage() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const [boards, setBoards] = useState([]);
  const [status, setStatus] = useState('loading');
  const [title, setTitle] = useState('');
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    refresh();
  }, []);

  async function refresh() {
    setStatus('loading');
    try {
      const list = await boardApi.listBoards();
      setBoards(list);
      setStatus('ready');
    } catch {
      setStatus('error');
    }
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!title.trim()) return;
    setCreating(true);
    setError('');
    try {
      const board = await boardApi.createBoard({ title: title.trim() });
      setTitle('');
      navigate(`/boards/${board.id}`);
    } catch (err) {
      setError(err.response?.data?.message || '보드 생성에 실패했습니다.');
    } finally {
      setCreating(false);
    }
  }

  return (
    <div style={{ padding: 32, maxWidth: 960, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>내 보드</h1>
        <div>
          <span style={{ marginRight: 12 }}>{user?.name}</span>
          <button onClick={logout}>로그아웃</button>
        </div>
      </div>

      <form onSubmit={handleCreate} style={{ display: 'flex', gap: 8, margin: '16px 0' }}>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="새 보드 이름"
          style={{ flex: 1, padding: 8 }}
        />
        <button type="submit" disabled={creating}>
          보드 생성
        </button>
      </form>
      {error && <div className="error">{error}</div>}

      {status === 'loading' && <p>불러오는 중...</p>}
      {status === 'error' && <p className="error">보드 목록을 불러오지 못했습니다.</p>}

      {status === 'ready' && boards.length === 0 && <p>아직 보드가 없습니다.</p>}

      <div className="board-grid">
        {boards.map((board) => (
          <Link key={board.id} to={`/boards/${board.id}`} className="board-grid-card">
            <strong>{board.title}</strong>
            <span className="board-grid-meta">
              {board.myRole === 'owner' ? '소유자' : '멤버'} · 멤버 {board.memberCount}명
            </span>
          </Link>
        ))}
      </div>
    </div>
  );
}
