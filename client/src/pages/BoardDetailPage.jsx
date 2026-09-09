import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useBoardStore } from '../stores/boardStore';
import ColumnView from '../components/board/ColumnView';

export default function BoardDetailPage() {
  const { boardId } = useParams();
  const { board, members, columns, cards, status, error, loadBoard, reset, addColumn, invite } =
    useBoardStore();

  const [newColumnTitle, setNewColumnTitle] = useState('');
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteError, setInviteError] = useState('');
  const [inviteOk, setInviteOk] = useState('');

  useEffect(() => {
    loadBoard(boardId);
    return () => reset();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [boardId]);

  async function handleAddColumn(e) {
    e.preventDefault();
    if (!newColumnTitle.trim()) return;
    await addColumn(newColumnTitle.trim());
    setNewColumnTitle('');
  }

  async function handleInvite(e) {
    e.preventDefault();
    setInviteError('');
    setInviteOk('');
    try {
      const member = await invite(inviteEmail.trim());
      setInviteOk(`${member?.name ?? inviteEmail}님을 초대했습니다.`);
      setInviteEmail('');
    } catch (err) {
      setInviteError(err.response?.data?.message || '초대에 실패했습니다.');
    }
  }

  if (status === 'loading' || status === 'idle') {
    return <div style={{ padding: 32 }}>불러오는 중...</div>;
  }

  if (status === 'error') {
    return (
      <div style={{ padding: 32 }}>
        <p className="error">{error}</p>
        <Link to="/">보드 목록으로</Link>
      </div>
    );
  }

  return (
    <div className="board-page">
      <header className="board-header">
        <div>
          <Link to="/" className="back-link">
            ← 보드 목록
          </Link>
          <h1>{board.title}</h1>
        </div>

        <div className="board-header-right">
          <div className="members">
            {members.map((m) => (
              <span key={m.userId} className="member-chip" title={m.email}>
                {m.name} {m.role === 'owner' ? '(소유자)' : ''}
              </span>
            ))}
          </div>

          {board.myRole === 'owner' && (
            <form className="invite-form" onSubmit={handleInvite}>
              <input
                type="email"
                placeholder="이메일로 초대"
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                required
              />
              <button type="submit">초대</button>
            </form>
          )}
        </div>
      </header>

      {inviteError && <div className="error" style={{ padding: '0 24px' }}>{inviteError}</div>}
      {inviteOk && <div className="success" style={{ padding: '0 24px' }}>{inviteOk}</div>}

      <div className="board-columns">
        {columns.map((column) => (
          <ColumnView
            key={column._id}
            column={column}
            cards={cards.filter((c) => c.columnId === column._id)}
          />
        ))}

        <form className="add-column-form" onSubmit={handleAddColumn}>
          <input
            value={newColumnTitle}
            onChange={(e) => setNewColumnTitle(e.target.value)}
            placeholder="+ 컬럼 추가"
          />
          {newColumnTitle && <button type="submit">추가</button>}
        </form>
      </div>
    </div>
  );
}
