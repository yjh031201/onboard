const express = require('express');
const cors = require('cors');
const { clientOrigin } = require('./config/env');
const authRoutes = require('./routes/auth.routes');
const boardRoutes = require('./routes/board.routes');
const columnRoutes = require('./routes/column.routes');
const cardRoutes = require('./routes/card.routes');

const app = express();

app.use(cors({ origin: clientOrigin, credentials: true }));
app.use(express.json());

app.use('/api/auth', authRoutes);
app.use('/api/boards', boardRoutes);
app.use('/api/columns', columnRoutes);
app.use('/api/cards', cardRoutes);

app.use((req, res) => {
  res.status(404).json({ message: 'Not found' });
});

// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  console.error(err);
  if (err.code === 11000) {
    return res.status(409).json({ message: '이미 존재하는 값입니다.' });
  }
  res.status(500).json({ message: '서버 오류가 발생했습니다.' });
});

module.exports = app;
