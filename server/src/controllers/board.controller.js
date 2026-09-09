const Board = require('../models/Board');
const Column = require('../models/Column');
const Card = require('../models/Card');
const User = require('../models/User');

function sanitizeBoard(board, viewerId) {
  const membership = board.members.find((m) => m.userId.toString() === viewerId);
  return {
    id: board._id,
    title: board.title,
    ownerId: board.ownerId,
    myRole: membership ? membership.role : null,
    memberCount: board.members.length,
    createdAt: board.createdAt,
    updatedAt: board.updatedAt,
  };
}

async function createBoard(req, res) {
  const { title } = req.body;
  if (!title || !title.trim()) {
    return res.status(400).json({ message: 'title은 필수입니다.' });
  }

  const board = await Board.create({
    title: title.trim(),
    ownerId: req.user.id,
    members: [{ userId: req.user.id, role: 'owner' }],
  });

  return res.status(201).json({ board: sanitizeBoard(board, req.user.id) });
}

async function listMyBoards(req, res) {
  const boards = await Board.find({ 'members.userId': req.user.id }).sort({ createdAt: -1 });
  return res.json({ boards: boards.map((b) => sanitizeBoard(b, req.user.id)) });
}

async function getBoardDetail(req, res) {
  const board = req.board;

  const memberIds = board.members.map((m) => m.userId);
  const users = await User.find({ _id: { $in: memberIds } }).select('name email avatarUrl');
  const usersById = new Map(users.map((u) => [u._id.toString(), u]));

  const members = board.members.map((m) => {
    const user = usersById.get(m.userId.toString());
    return {
      userId: m.userId,
      role: m.role,
      name: user?.name ?? '(알 수 없음)',
      email: user?.email ?? null,
    };
  });

  const columns = await Column.find({ boardId: board._id }).sort({ position: 1 });
  const cards = await Card.find({ boardId: board._id }).sort({ position: 1 });

  return res.json({
    board: {
      id: board._id,
      title: board.title,
      ownerId: board.ownerId,
      labels: board.labels,
      myRole: req.memberRole,
    },
    members,
    columns,
    cards,
  });
}

async function renameBoard(req, res) {
  const { title } = req.body;
  if (!title || !title.trim()) {
    return res.status(400).json({ message: 'title은 필수입니다.' });
  }
  if (req.memberRole !== 'owner') {
    return res.status(403).json({ message: '보드 소유자만 이름을 변경할 수 있습니다.' });
  }

  req.board.title = title.trim();
  await req.board.save();

  return res.json({ board: sanitizeBoard(req.board, req.user.id) });
}

async function deleteBoard(req, res) {
  if (req.memberRole !== 'owner') {
    return res.status(403).json({ message: '보드 소유자만 삭제할 수 있습니다.' });
  }

  const boardId = req.board._id;
  await Card.deleteMany({ boardId });
  await Column.deleteMany({ boardId });
  await req.board.deleteOne();

  return res.json({ success: true, boardId });
}

async function removeMember(req, res) {
  const { userId } = req.params;
  if (req.memberRole !== 'owner') {
    return res.status(403).json({ message: '보드 소유자만 멤버를 제거할 수 있습니다.' });
  }

  const board = req.board;
  const target = board.members.find((m) => m.userId.toString() === userId);
  if (!target) {
    return res.status(404).json({ message: '보드 멤버가 아닙니다.' });
  }
  if (target.role === 'owner') {
    return res.status(400).json({ message: '소유자는 제거할 수 없습니다.' });
  }

  board.members = board.members.filter((m) => m.userId.toString() !== userId);
  await board.save();

  return res.json({ success: true, userId });
}

async function inviteMember(req, res) {
  const { email } = req.body;
  if (!email || !email.trim()) {
    return res.status(400).json({ message: 'email은 필수입니다.' });
  }
  if (req.memberRole !== 'owner') {
    return res.status(403).json({ message: '보드 소유자만 멤버를 초대할 수 있습니다.' });
  }

  const targetUser = await User.findOne({ email: email.toLowerCase().trim() });
  if (!targetUser) {
    return res.status(404).json({ message: '가입되지 않은 이메일입니다.' });
  }

  const board = req.board;
  const alreadyMember = board.members.some((m) => m.userId.toString() === targetUser._id.toString());
  if (alreadyMember) {
    return res.status(409).json({ message: '이미 보드 멤버입니다.' });
  }

  board.members.push({ userId: targetUser._id, role: 'member' });
  await board.save();

  return res.status(201).json({
    member: { userId: targetUser._id, role: 'member', name: targetUser.name, email: targetUser.email },
  });
}

module.exports = {
  createBoard,
  listMyBoards,
  getBoardDetail,
  renameBoard,
  deleteBoard,
  inviteMember,
  removeMember,
};
