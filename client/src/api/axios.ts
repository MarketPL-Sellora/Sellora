import axios, { type AxiosInstance } from 'axios';

const API_URL = 'http://localhost:8080/api/v1';

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  // ДОЗВОЛЯЄ БРАУЗЕРУ АВТОМАТИЧНО ПРИКРІПЛЮВАТИ COOKIES (зокрема accessToken)
  withCredentials: true,
});

// ─── Response Interceptor: обробка 401 Unauthorized ─────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    // Якщо бекенд повернув 401 — cookie протухла або відсутня
    if (error.response?.status === 401) {
      // Не обробляємо 401 від самого /auth/logout, щоб уникнути рекурсії
      const requestUrl: string = error.config?.url || '';
      if (requestUrl.includes('/auth/logout')) {
        return Promise.reject(error);
      }

      // Ленивий імпорт для уникнення циклічних залежностей (axios → userStore → axios)
      const { useUserStore } = await import('../state/userStore');
      const userStore = useUserStore();

      // Скидаємо стан авторизації
      userStore.$patch({
        user: null,
        isAuthenticated: false,
        sellerStore: null,
      });

      // Редірект з захищених сторінок на головну
      const protectedPaths = ['/cabinet'];
      if (protectedPaths.some((p) => window.location.pathname.startsWith(p))) {
        window.location.href = '/';
      }
    }

    return Promise.reject(error);
  }
);

// export default залишати не обов'язково, якщо всюди використовується import { apiClient }
export default apiClient;
