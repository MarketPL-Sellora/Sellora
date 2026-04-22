import axios from 'axios';

const api = axios.create({
  // Заміни на порт, на якому Влад запустив бекенд (наприклад, 5000 або 8080)
  baseURL: 'http://localhost:5000/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
});

export default api;
