import { create } from 'zustand';
import * as boardApi from '../api/boardApi';

export const useBoardStore = create((set, get) => ({
  board: null,
  members: [],
  columns: [],
  cards: [],
  status: 'idle', // idle | loading | ready | error
  error: '',

  async loadBoard(boardId) {
    set({ status: 'loading', error: '' });
    try {
      const { board, members, columns, cards } = await boardApi.getBoardDetail(boardId);
      set({ board, members, columns, cards, status: 'ready' });
    } catch (err) {
      set({ status: 'error', error: err.response?.data?.message || '보드를 불러오지 못했습니다.' });
    }
  },

  reset() {
    set({ board: null, members: [], columns: [], cards: [], status: 'idle', error: '' });
  },

  async addColumn(title) {
    const column = await boardApi.createColumn(get().board.id, title);
    set((s) => ({ columns: [...s.columns, column] }));
  },

  async renameColumn(columnId, title) {
    const updated = await boardApi.renameColumn(columnId, title);
    set((s) => ({
      columns: s.columns.map((c) => (c._id === updated._id ? updated : c)),
    }));
  },

  async removeColumn(columnId) {
    await boardApi.deleteColumn(columnId);
    set((s) => ({
      columns: s.columns.filter((c) => c._id !== columnId),
      cards: s.cards.filter((c) => c.columnId !== columnId),
    }));
  },

  async addCard(columnId, { title, description }) {
    const card = await boardApi.createCard(columnId, { title, description });
    set((s) => ({ cards: [...s.cards, card] }));
  },

  async editCard(cardId, fields) {
    const updated = await boardApi.updateCard(cardId, fields);
    set((s) => ({
      cards: s.cards.map((c) => (c._id === updated._id ? updated : c)),
    }));
  },

  async removeCard(cardId) {
    await boardApi.deleteCard(cardId);
    set((s) => ({ cards: s.cards.filter((c) => c._id !== cardId) }));
  },

  async invite(email) {
    const member = await boardApi.inviteMember(get().board.id, email);
    set((s) => ({ members: [...s.members, member] }));
  },
}));
