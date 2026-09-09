const Board = require('../models/Board');
const Column = require('../models/Column');
const Card = require('../models/Card');

function findMembership(board, userId) {
  return board.members.find((m) => m.userId.toString() === userId);
}

async function loadBoardMember(req, res, next) {
  const board = await Board.findById(req.params.boardId);
  if (!board) {
    return res.status(404).json({ message: '보드를 찾을 수 없습니다.' });
  }
  const membership = findMembership(board, req.user.id);
  if (!membership) {
    return res.status(403).json({ message: '이 보드의 멤버가 아닙니다.' });
  }
  req.board = board;
  req.memberRole = membership.role;
  next();
}

async function loadColumnMember(req, res, next) {
  const column = await Column.findById(req.params.columnId);
  if (!column) {
    return res.status(404).json({ message: '컬럼을 찾을 수 없습니다.' });
  }
  const board = await Board.findById(column.boardId);
  if (!board) {
    return res.status(404).json({ message: '보드를 찾을 수 없습니다.' });
  }
  const membership = findMembership(board, req.user.id);
  if (!membership) {
    return res.status(403).json({ message: '이 보드의 멤버가 아닙니다.' });
  }
  req.column = column;
  req.board = board;
  req.memberRole = membership.role;
  next();
}

async function loadCardMember(req, res, next) {
  const card = await Card.findById(req.params.cardId);
  if (!card) {
    return res.status(404).json({ message: '카드를 찾을 수 없습니다.' });
  }
  const board = await Board.findById(card.boardId);
  if (!board) {
    return res.status(404).json({ message: '보드를 찾을 수 없습니다.' });
  }
  const membership = findMembership(board, req.user.id);
  if (!membership) {
    return res.status(403).json({ message: '이 보드의 멤버가 아닙니다.' });
  }
  req.card = card;
  req.board = board;
  req.memberRole = membership.role;
  next();
}

module.exports = { loadBoardMember, loadColumnMember, loadCardMember };
