import { createRouter, createWebHistory } from 'vue-router'

// Імпортуємо всі твої ГОЛОВНІ сторінки
import HomePage from './components/HomePage.vue'
import ProductPage from './components/ProductPage.vue'
import CheckoutPage from './components/CheckoutPage.vue'
import CabinetPage from './components/CabinetPage.vue'

// Налаштовуємо шляхи (посилання)
const routes = [
  { path: '/', component: HomePage },
  { path: '/product', component: ProductPage },
  { path: '/checkout', component: CheckoutPage },
  { path: '/cabinet', component: CabinetPage }
]

// Створюємо сам роутер
export const router = createRouter({
  history: createWebHistory(),
  routes
})
