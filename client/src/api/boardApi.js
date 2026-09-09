import http from './http';

export function listBoards() {
  return http.get('/boards').then((res) => res.data.boards);
}

export function createBoard({ title }) {
  return http.post('/boards', { title }).then((res) => res.data.board);
}

export function getBoardDetail(boardId) {
  return http.get(`/boards/${boardId}`).then((res) => res.data);
}

export function inviteMember(boardId, email) {
  return http.post(`/boards/${boardId}/invite`, { email }).then((res) => res.data.member);
}

export function createColumn(boardId, title) {
  return http.post(`/boards/${boardId}/columns`, { title }).then((res) => res.data.column);
}

export function renameColumn(columnId, title) {
  return http.patch(`/columns/${columnId}`, { title }).then((res) => res.data.column);
}

export function deleteColumn(columnId) {
  return http.delete(`/columns/${columnId}`).then((res) => res.data);
}

export function createCard(columnId, { title, description }) {
  return http.post(`/columns/${columnId}/cards`, { title, description }).then((res) => res.data.card);
}

export function updateCard(cardId, fields) {
  return http.patch(`/cards/${cardId}`, fields).then((res) => res.data.card);
}

export function deleteCard(cardId) {
  return http.delete(`/cards/${cardId}`).then((res) => res.data);
}
