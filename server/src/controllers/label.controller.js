const Card = require('../models/Card');

function sanitizeLabel(label) {
  return { id: label._id, name: label.name, color: label.color };
}

async function createLabel(req, res) {
  const { name, color } = req.body;
  if (!name || !name.trim()) {
    return res.status(400).json({ message: 'name은 필수입니다.' });
  }
  if (!color || !color.trim()) {
    return res.status(400).json({ message: 'color는 필수입니다.' });
  }

  const board = req.board;
  board.labels.push({ name: name.trim(), color: color.trim() });
  await board.save();

  const label = board.labels[board.labels.length - 1];
  return res.status(201).json({ label: sanitizeLabel(label) });
}

async function updateLabel(req, res) {
  const { labelId } = req.params;
  const { name, color } = req.body;

  const board = req.board;
  const label = board.labels.id(labelId);
  if (!label) {
    return res.status(404).json({ message: '라벨을 찾을 수 없습니다.' });
  }

  if (name !== undefined) {
    if (!name.trim()) {
      return res.status(400).json({ message: 'name은 비어 있을 수 없습니다.' });
    }
    label.name = name.trim();
  }
  if (color !== undefined) {
    if (!color.trim()) {
      return res.status(400).json({ message: 'color는 비어 있을 수 없습니다.' });
    }
    label.color = color.trim();
  }

  await board.save();
  return res.json({ label: sanitizeLabel(label) });
}

async function deleteLabel(req, res) {
  const { labelId } = req.params;
  const board = req.board;
  const label = board.labels.id(labelId);
  if (!label) {
    return res.status(404).json({ message: '라벨을 찾을 수 없습니다.' });
  }

  const affectedCards = await Card.find({ boardId: board._id, labels: labelId }).select('_id');
  const cardIds = affectedCards.map((c) => c._id);

  label.deleteOne();
  await board.save();

  if (cardIds.length > 0) {
    await Card.updateMany({ boardId: board._id }, { $pull: { labels: labelId } });
  }

  return res.json({ success: true, labelId, cardIds });
}

module.exports = { createLabel, updateLabel, deleteLabel };
