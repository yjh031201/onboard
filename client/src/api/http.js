import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
});

http.interceptors.request.use((config) => {
  const { token } = useAuthStore.getState();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default http;
