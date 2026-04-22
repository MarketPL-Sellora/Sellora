import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { router } from './router.ts' // 1. Додали імпорт нашого роутера

const app = createApp(App) // 2. Створили сам додаток і записали в змінну app

app.use(router) // 3. Сказали додатку використовувати роутер

app.mount('#app') // 4. Запустили додаток на екрані
