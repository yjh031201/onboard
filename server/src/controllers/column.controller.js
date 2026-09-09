const Column = require('../models/Column');
const Card = require('../models/Card');
const { nextPosition } = require('../utils/position');

async function createColumn(req, res) {
  const { title } = req.body;
  if (!title || !title.trim()) {
    return res.status(400).json({ message: 'title은 필수입니다.' });
  }

  const lastColumn = await Column.findOne({ boardId: req.board._id }).sort({ position: -1 });
  const position = nextPosition(lastColumn?.position);

  const column = await Column.create({
    boardId: req.board._id,
    title: title.trim(),
    position,
  });

  return res.status(201).json({ column });
}

async function renameColumn(req, res) {
  const { title } = req.body;
  if (!title || !title.trim()) {
    return res.status(400).json({ message: 'title은 필수입니다.' });
  }

  req.column.title = title.trim();
  await req.column.save();

  return res.json({ column: req.column });
}

async function deleteColumn(req, res) {
  await Card.deleteMany({ columnId: req.column._id });
  await req.column.deleteOne();

  return res.json({ success: true, columnId: req.column._id });
}

module.exports = { createColumn, renameColumn, deleteColumn };
