const { Router } = require('express');
const { register, login, me } = require('../controllers/auth.controller');
const requireAuth = require('../middleware/jwt.middleware');
const asyncHandler = require('../utils/asyncHandler');

const router = Router();

router.post('/register', asyncHandler(register));
router.post('/login', asyncHandler(login));
router.get('/me', requireAuth, asyncHandler(me));

module.exports = router;
