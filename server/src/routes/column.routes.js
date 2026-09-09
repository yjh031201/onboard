const { Router } = require('express');
const requireAuth = require('../middleware/jwt.middleware');
const { loadColumnMember } = require('../middleware/membership.middleware');
const asyncHandler = require('../utils/asyncHandler');
const { renameColumn, deleteColumn } = require('../controllers/column.controller');
const { createCard } = require('../controllers/card.controller');

const router = Router();

router.use(requireAuth);

router.patch('/:columnId', loadColumnMember, asyncHandler(renameColumn));
router.delete('/:columnId', loadColumnMember, asyncHandler(deleteColumn));
router.post('/:columnId/cards', loadColumnMember, asyncHandler(createCard));

module.exports = router;
