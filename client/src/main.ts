import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import { router } from './router.ts' // 1. Додали імпорт нашого роутера

const app   = createApp(App)  // 2. Створили сам додаток і записали в змінну app
const pinia = createPinia()   // 3. Створюємо Pinia — централізоване сховище стану

app.use(pinia)  // 4. Підключаємо Pinia до додатку
app.use(router) // 5. Підключаємо роутер

app.mount('#app') // 6. Запускаємо додаток на екрані
