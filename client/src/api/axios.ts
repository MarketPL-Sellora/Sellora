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

// Ми ПОВНІСТЮ ВИДАЛИЛИ інтерсептор request, тому що:
// 1. Влад сказав, що авторизація не має бути побудована на токенах з localStorage.
// 2. Влад сказав, що авторизація працює ТІЛЬКИ через відправлення cookies браузером.
// 3. Браузер сам додасть cookie "accessToken" до запиту завдяки withCredentials: true.

// export default залишати не обов'язково, якщо всюди використовується import { apiClient }
export default apiClient;
