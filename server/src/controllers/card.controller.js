const Card = require('../models/Card');
const { nextPosition } = require('../utils/position');

const UPDATABLE_FIELDS = ['title', 'description', 'assigneeId', 'labels', 'dueDate'];

async function createCard(req, res) {
  const { title, description } = req.body;
  if (!title || !title.trim()) {
    return res.status(400).json({ message: 'title은 필수입니다.' });
  }

  const lastCard = await Card.findOne({ columnId: req.column._id }).sort({ position: -1 });
  const position = nextPosition(lastCard?.position);

  const card = await Card.create({
    boardId: req.column.boardId,
    columnId: req.column._id,
    title: title.trim(),
    description: description || '',
    position,
    createdBy: req.user.id,
  });

  return res.status(201).json({ card });
}

async function updateCard(req, res) {
  const card = req.card;

  for (const field of UPDATABLE_FIELDS) {
    if (field in req.body) {
      card[field] = req.body[field];
    }
  }
  if ('title' in req.body && !req.body.title.trim()) {
    return res.status(400).json({ message: 'title은 비어 있을 수 없습니다.' });
  }

  await card.save();
  return res.json({ card });
}

async function deleteCard(req, res) {
  await req.card.deleteOne();
  return res.json({ success: true, cardId: req.card._id });
}

module.exports = { createCard, updateCard, deleteCard };
