import { create } from 'zustand';

const STORAGE_KEY = 'kanban.auth';

function loadPersisted() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : { token: null, user: null };
  } catch {
    return { token: null, user: null };
  }
}

const persisted = loadPersisted();

export const useAuthStore = create((set) => ({
  token: persisted.token,
  user: persisted.user,
  status: 'idle', // idle | checking | ready

  setAuth: (token, user) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, user }));
    set({ token, user, status: 'ready' });
  },

  setStatus: (status) => set({ status }),

  logout: () => {
    localStorage.removeItem(STORAGE_KEY);
    set({ token: null, user: null, status: 'ready' });
  },
}));
