import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import { router } from './router.ts'
import { useUserStore } from './state/userStore'
import Vue3Toastify, { type ToastContainerOptions } from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'

const app   = createApp(App)
const pinia = createPinia()

app.use(pinia)

app.use(Vue3Toastify, {
  theme: 'dark',
  position: 'bottom-right',
  autoClose: 3000,
  hideProgressBar: false,
  newestOnTop: true,
  toastStyle: {
    backgroundColor: '#0f172a',
    borderRadius: '12px',
    border: '1px solid #1e293b',
    fontFamily: "'Onest', sans-serif",
    fontSize: '14px',
  },
} as ToastContainerOptions)

// Асинхронна ініціалізація додатку
async function initApp() {
  const userStore = useUserStore()
  
  // Чекаємо перевірку сесії ДО запуску роутера
  await userStore.fetchMe()

  // Тепер роутер точно знає статус авторизації і не помилиться в beforeEach
  app.use(router)
  app.mount('#app')
}

initApp()
