import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import { router } from './router.ts'
import { useUserStore } from './state/userStore' 

const app   = createApp(App)
const pinia = createPinia()

app.use(pinia) 

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
