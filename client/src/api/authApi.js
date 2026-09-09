import http from './http';

export function register({ email, password, name }) {
  return http.post('/auth/register', { email, password, name }).then((res) => res.data);
}

export function login({ email, password }) {
  return http.post('/auth/login', { email, password }).then((res) => res.data);
}

export function fetchMe() {
  return http.get('/auth/me').then((res) => res.data);
}
