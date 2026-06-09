import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from './state/userStore'

// Імпортуємо всі твої ГОЛОВНІ сторінки
import HomePage     from './views/HomePage.vue'
import ProductPage  from './views/ProductPage.vue'
import CheckoutPage from './views/CheckoutPage.vue'
import OrderPage    from './views/OrderPage.vue'
import CabinetPage  from './views/CabinetPage.vue'
import NotFound from './views/NotFound.vue'


// Налаштовуємо шляхи (посилання)
const routes = [
  { path: '/',             component: HomePage     },
  { path: '/product/:id',
    name: 'product',
    component: ProductPage  },
  { path: '/checkout',     component: CheckoutPage },
  { path: '/order/:id',    name: 'order', component: OrderPage },
  { path: '/cabinet',      component: CabinetPage, meta: { requiresAuth: true } },
  // Catch-all route для 404 помилки
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound },
]

// Створюємо сам роутер
export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
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
