import { useState } from 'react';
import { useBoardStore } from '../../stores/boardStore';

export default function CardItem({ card }) {
  const editCard = useBoardStore((s) => s.editCard);
  const removeCard = useBoardStore((s) => s.removeCard);
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState(card.title);
  const [description, setDescription] = useState(card.description || '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  async function handleSave(e) {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    setError('');
    try {
      await editCard(card._id, { title: title.trim(), description });
      setEditing(false);
    } catch (err) {
      setError(err.response?.data?.message || '수정에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!confirm('이 카드를 삭제할까요?')) return;
    await removeCard(card._id);
  }

  if (editing) {
    return (
      <form className="card-item card-item-editing" onSubmit={handleSave}>
        <input value={title} onChange={(e) => setTitle(e.target.value)} autoFocus />
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="설명 (선택)"
          rows={3}
        />
        {error && <div className="error">{error}</div>}
        <div className="card-item-actions">
          <button type="submit" disabled={saving}>저장</button>
          <button type="button" onClick={() => setEditing(false)}>취소</button>
          <button type="button" className="danger" onClick={handleDelete}>삭제</button>
        </div>
      </form>
    );
  }

  return (
    <div className="card-item" onClick={() => setEditing(true)}>
      <div className="card-item-title">{card.title}</div>
      {card.description && <div className="card-item-desc">{card.description}</div>}
    </div>
  );
}
