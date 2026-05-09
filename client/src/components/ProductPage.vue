<script setup lang="ts">
import { ref, onMounted }   from 'vue'
import { useRoute, useRouter } from 'vue-router' // ДОДАНО useRouter
import Header               from './Header.vue'
import Breadcrumbs          from './Breadcrumbs.vue'
import Footer               from './Footer.vue'
import ProductDetails       from './ProductDetails.vue'
import ProductGallery       from './ProductGallery.vue'
import ProductTabs          from './ProductTabs.vue'
import RelatedProducts      from './RelatedProducts.vue'
import GroupBuyModal        from './GroupModal.vue'

import { useGroupBuyStore } from '../state/groupBuyStore'
import type { GroupBuySession } from '../state/groupBuyStore'

const route = useRoute()
const router = useRouter() // Ініціалізуємо роутер
const groupBuyStore = useGroupBuyStore()

const productId = Number(route.params.id) || 3

const isGroupBuyModalOpen = ref(false)
const sessionData = ref<GroupBuySession | null>(null)

onMounted(async () => {
  const uuid = route.query.session as string | undefined
  if (uuid) {
    sessionData.value = await groupBuyStore.fetchSession(uuid)
  }
})

// ─── ОНОВЛЕНА ЛОГІКА ЗАКРИТТЯ МОДАЛКИ ─────────────────────────────────────
function closeGroupModal() {
  isGroupBuyModalOpen.value = false

  // Якщо юзер щойно створив сесію (в сторі є uuid)
  if (groupBuyStore.session?.uuid) {
    // 1. Передаємо дані сесії в компонент деталей
    sessionData.value = groupBuyStore.session
    // 2. Оновлюємо URL браузера, щоб там з'явився ?session=uuid (без перезавантаження сторінки)
    router.replace({ query: { session: groupBuyStore.session.uuid } })
  } else {
    // Якщо просто закрили вікно без створення
    groupBuyStore.clearSession()
  }
}

const productBreadcrumbs = ref([
  { name: 'Головна', path: '/' },
  { name: 'Ноутбуки', path: '/laptops' },
  { name: 'MacBook Pro 16", M3 Pro, 18/512GB' },
])
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">
    <Header :showCategories="false" />
    <Breadcrumbs :items="productBreadcrumbs" />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-4 md:px-6 py-6 md:py-8 flex flex-col gap-8 md:gap-12 overflow-hidden">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-10 items-start">
        <div class="w-full">
          <ProductGallery />
        </div>

        <div class="w-full">
          <ProductDetails
            :session-data="sessionData"
            @start-group="isGroupBuyModalOpen = true"
          />
        </div>
      </div>

      <div class="flex flex-col gap-16">
        <ProductTabs />
        <RelatedProducts />
      </div>
    </main>

    <Footer />

    <GroupBuyModal
      v-if="isGroupBuyModalOpen"
      :product-id="productId"
      @close="closeGroupModal"
    />
  </div>
</template>
