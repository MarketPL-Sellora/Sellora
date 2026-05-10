<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter }      from 'vue-router'
import Header               from './Header.vue'
import Breadcrumbs          from './Breadcrumbs.vue'
import Footer               from './Footer.vue'
import ProductDetails       from './ProductDetails.vue'
import ProductGallery       from './ProductGallery.vue'
import ProductTabs          from './ProductTabs.vue'
import RelatedProducts      from './RelatedProducts.vue'
import GroupBuyModal        from './GroupModal.vue'

import { useGroupBuyStore } from '../state/groupBuyStore'
import { useProductStore }  from '../state/productStore'
import type { GroupBuySession } from '../state/groupBuyStore'

const route  = useRoute()
const router = useRouter()
const groupBuyStore = useGroupBuyStore()
const productStore  = useProductStore()

const productId = Number(route.params.id) || 3

const isGroupBuyModalOpen = ref(false)
const sessionData = ref<GroupBuySession | null>(null)

onMounted(async () => {
  // Завантажуємо товар з бекенду
  await productStore.fetchProductById(productId)

  // Завантажуємо сесію групової покупки (якщо є)
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

const productBreadcrumbs = computed(() => [
  { name: 'Головна', path: '/' },
  { name: 'Каталог', path: '/catalog' },
  { name: productStore.currentProduct?.title ?? 'Товар' },
])
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">
    <Header :showCategories="false" />
    <Breadcrumbs :items="productBreadcrumbs" />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-4 md:px-6 py-6 md:py-8 flex flex-col gap-8 md:gap-12 overflow-hidden">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-10 items-start">
        <div class="w-full">
          <ProductGallery :product-images="productStore.currentProduct?.images" />
        </div>

        <div class="w-full">
          <ProductDetails
            :api-product="productStore.currentProduct"
            :session-data="sessionData"
            @start-group="isGroupBuyModalOpen = true"
          />
        </div>
      </div>

      <div class="flex flex-col gap-16">
        <ProductTabs :description="productStore.currentProduct?.description" />
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
