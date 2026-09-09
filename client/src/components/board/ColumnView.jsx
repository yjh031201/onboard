import { useState } from 'react';
import { useBoardStore } from '../../stores/boardStore';
import CardItem from './CardItem';

export default function ColumnView({ column, cards }) {
  const renameColumn = useBoardStore((s) => s.renameColumn);
  const removeColumn = useBoardStore((s) => s.removeColumn);
  const addCard = useBoardStore((s) => s.addCard);

  const [renaming, setRenaming] = useState(false);
  const [title, setTitle] = useState(column.title);
  const [newCardTitle, setNewCardTitle] = useState('');
  const [adding, setAdding] = useState(false);

  async function handleRename(e) {
    e.preventDefault();
    if (!title.trim() || title.trim() === column.title) {
      setRenaming(false);
      setTitle(column.title);
      return;
    }
    await renameColumn(column._id, title.trim());
    setRenaming(false);
  }

  async function handleDeleteColumn() {
    if (!confirm(`"${column.title}" 컬럼과 그 안의 카드를 모두 삭제할까요?`)) return;
    await removeColumn(column._id);
  }

  async function handleAddCard(e) {
    e.preventDefault();
    if (!newCardTitle.trim()) return;
    setAdding(true);
    try {
      await addCard(column._id, { title: newCardTitle.trim() });
      setNewCardTitle('');
    } finally {
      setAdding(false);
    }
  }

  return (
    <div className="column">
      <div className="column-header">
        {renaming ? (
          <form onSubmit={handleRename}>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onBlur={handleRename}
              autoFocus
            />
          </form>
        ) : (
          <span className="column-title" onClick={() => setRenaming(true)}>
            {column.title}
          </span>
        )}
        <button className="icon-btn" onClick={handleDeleteColumn} title="컬럼 삭제">
          ×
        </button>
      </div>

      <div className="column-cards">
        {cards.map((card) => (
          <CardItem key={card._id} card={card} />
        ))}
      </div>

      <form className="add-card-form" onSubmit={handleAddCard}>
        <input
          value={newCardTitle}
          onChange={(e) => setNewCardTitle(e.target.value)}
          placeholder="+ 카드 추가"
        />
        {newCardTitle && (
          <button type="submit" disabled={adding}>
            추가
          </button>
        )}
      </form>
    </div>
  );
}
