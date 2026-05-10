import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from './state/userStore'

// Імпортуємо всі твої ГОЛОВНІ сторінки
import HomePage     from './components/HomePage.vue'
import ProductPage  from './components/ProductPage.vue'
import CheckoutPage from './components/CheckoutPage.vue'
import CabinetPage  from './components/CabinetPage.vue'


// Налаштовуємо шляхи (посилання)
const routes = [
  { path: '/',             component: HomePage     },
  { path: '/product/:id',
    name: 'product',
    component: ProductPage  },  // :id — динамічний параметр
  { path: '/checkout',     component: CheckoutPage },
  { path: '/cabinet',      component: CabinetPage, meta: { requiresAuth: true } },
]

// Створюємо сам роутер
export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      // Якщо користувач натиснув "Назад" у браузері, повертаємо туди, де він був
      return savedPosition
    } else {
      // При кожному новому переході (кліку) скролимо на самий верх
      return { top: 0, behavior: 'smooth' }
    }
  },
})

// ─── Auth Guard ──────────────────────────────────────────────────────────────
router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth) {
    const userStore = useUserStore() // Інстанціюємо всередині guard, щоб Pinia вже була ініціалізована
    if (!userStore.isAuthenticated) {
      return next('/')
    }
  }
  next()
})
