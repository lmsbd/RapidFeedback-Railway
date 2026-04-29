import axios from 'axios';
import userStore from '@/stores/userStore';

const API_BASE_URL =
  process.env.NODE_ENV === 'development'
    ? '/api'
    : process.env.UMI_APP_API_BASE_URL ||
      'https://rapidfeedback-railway-production.up.railway.app/rfo/api';

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  withCredentials: false,
});

request.interceptors.request.use(
  (config) => {
    config.headers = {
      ...config.headers,
      'Content-Type': 'application/json',
    };

    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      userStore.logout();
      window.location.reload();
    }
    return Promise.reject(error);
  }
);

export default request;