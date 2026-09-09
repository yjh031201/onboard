require('dotenv').config();

function required(name, fallback) {
  const value = process.env[name] ?? fallback;
  if (value === undefined) {
    throw new Error(`Missing required env var: ${name}`);
  }
  return value;
}

module.exports = {
  port: required('PORT', '4000'),
  mongoUri: required('MONGO_URI', 'mongodb://localhost:27017/kanban'),
  jwtSecret: required('JWT_SECRET'),
  jwtExpiresIn: required('JWT_EXPIRES_IN', '2h'),
  clientOrigin: required('CLIENT_ORIGIN', 'http://localhost:5173'),
};
