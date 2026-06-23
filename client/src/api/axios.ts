import axios, { type AxiosInstance } from 'axios';

const API_URL = 'http://localhost:8080/api/v1';

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const requestUrl: string = error.config?.url || '';
      if (requestUrl.includes('/auth/logout')) {
        return Promise.reject(error);
      }

      const { useUserStore } = await import('../state/userStore');
      const userStore = useUserStore();

      userStore.$patch({
        user: null,
        isAuthenticated: false,
        sellerStore: null,
        isAuthModalOpen: true,
      });

      const protectedPaths = ['/cabinet'];
      if (protectedPaths.some((p) => window.location.pathname.startsWith(p))) {
        window.location.href = '/';
      }

      return Promise.reject({ ...error, isHandled: true });
    }

    return Promise.reject(error);
  }
);

export default apiClient;
