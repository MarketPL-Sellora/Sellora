import axios, { type AxiosInstance } from 'axios'

// Виправлено: додано /v1 відповідно до Swagger (/api/v1/auth/register)
const API_URL = 'http://localhost:8080/api/v1'

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
  // Дозволяє браузеру автоматично прикріплювати HttpOnly cookie
  withCredentials: true,
})
