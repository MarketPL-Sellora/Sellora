import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from './state/userStore'

import HomePage from './views/HomePage.vue'
import ProductPage from './views/ProductPage.vue'
import CheckoutPage from './views/CheckoutPage.vue'
import OrderPage from './views/OrderPage.vue'
import CabinetPage from './views/CabinetPage.vue'
import NotFound from './views/NotFound.vue'


const routes = [
  { path: '/', component: HomePage },
  {
    path: '/product/:id',
    name: 'product',
    component: ProductPage
  },
  { path: '/checkout', component: CheckoutPage },
  { path: '/order/:id', name: 'order', component: OrderPage },
  { path: '/cabinet', component: CabinetPage, meta: { requiresAuth: true } },
  { path: '/info/:section?', name: 'InfoPage', component: () => import('./views/InfoPage.vue') },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound },
]

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

router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth) {
    const userStore = useUserStore()
    if (!userStore.isAuthenticated) {
      return next('/')
    }
  }
  next()
})
