const { Router } = require('express');
const requireAuth = require('../middleware/jwt.middleware');
const { loadBoardMember } = require('../middleware/membership.middleware');
const asyncHandler = require('../utils/asyncHandler');
const {
  createBoard,
  listMyBoards,
  getBoardDetail,
  renameBoard,
  deleteBoard,
  inviteMember,
  removeMember,
} = require('../controllers/board.controller');
const { createColumn } = require('../controllers/column.controller');
const { createLabel, updateLabel, deleteLabel } = require('../controllers/label.controller');

const router = Router();

router.use(requireAuth);

router.post('/', asyncHandler(createBoard));
router.get('/', asyncHandler(listMyBoards));
router.get('/:boardId', loadBoardMember, asyncHandler(getBoardDetail));
router.patch('/:boardId', loadBoardMember, asyncHandler(renameBoard));
router.delete('/:boardId', loadBoardMember, asyncHandler(deleteBoard));

router.post('/:boardId/invite', loadBoardMember, asyncHandler(inviteMember));
router.delete('/:boardId/members/:userId', loadBoardMember, asyncHandler(removeMember));

router.post('/:boardId/labels', loadBoardMember, asyncHandler(createLabel));
router.patch('/:boardId/labels/:labelId', loadBoardMember, asyncHandler(updateLabel));
router.delete('/:boardId/labels/:labelId', loadBoardMember, asyncHandler(deleteLabel));

router.post('/:boardId/columns', loadBoardMember, asyncHandler(createColumn));

module.exports = router;
