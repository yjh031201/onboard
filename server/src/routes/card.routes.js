const { Router } = require('express');
const requireAuth = require('../middleware/jwt.middleware');
const { loadCardMember } = require('../middleware/membership.middleware');
const asyncHandler = require('../utils/asyncHandler');
const { updateCard, deleteCard } = require('../controllers/card.controller');

const router = Router();

router.use(requireAuth);

router.patch('/:cardId', loadCardMember, asyncHandler(updateCard));
router.delete('/:cardId', loadCardMember, asyncHandler(deleteCard));

module.exports = router;
