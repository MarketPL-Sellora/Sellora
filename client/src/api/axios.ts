import axios, { type AxiosInstance } from 'axios';

// Виправлено: додано /v1 відповідно до Swagger (/api/v1/auth/register)
const API_URL = 'http://localhost:8080/api/v1';

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  // Дозволяє браузеру автоматично прикріплювати HttpOnly cookie
  withCredentials: true,
});

// ПЕРЕХОПЛЮВАЧ: Цей код спрацьовує ПЕРЕД кожним запитом на бекенд
apiClient.interceptors.request.use(
  (config) => {
    // 1. Дістаємо токен з LocalStorage (переконайся, що Вадим зберігає його саме під ключем 'token')
    const token = localStorage.getItem('token');

    // 2. Якщо токен є — чіпляємо його в заголовок Authorization
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// export default залишати не обов'язково, якщо всюди використовується import { apiClient }
// але я залишу для сумісності, якщо Вадим використовував дефолтний імпорт.
export default apiClient;
