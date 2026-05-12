<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
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
import { useCategoryStore } from '../state/categoryStore'
import type { GroupBuySession } from '../state/groupBuyStore'

const route  = useRoute()
const router = useRouter()
const groupBuyStore = useGroupBuyStore()
const productStore  = useProductStore()
const categoryStore = useCategoryStore()

const productId = Number(route.params.id) || 3

const isGroupBuyModalOpen = ref(false)
const sessionData = ref<GroupBuySession | null>(null)
const headerRef = ref<any>(null)

function handleOpenAuth() {
  isGroupBuyModalOpen.value = false;
  if (headerRef.value) {
    headerRef.value.isAuthModalOpen = true;
  }
}

onMounted(async () => {
  await categoryStore.fetchFlatCategories()
  // Завантажуємо товар з бекенду
  await productStore.fetchProductById(productId)

  // Завантажуємо сесію групової покупки (якщо є)
  const uuid = route.query.session as string | undefined
  if (uuid) {
    sessionData.value = await groupBuyStore.fetchSession(uuid)
  }
})

// Слідкуємо за зміною ID в URL (наприклад, при кліку на "Схожі товари")
watch(
  () => route.params.id,
  async (newId, oldId) => {
    if (newId && newId !== oldId) {
      const id = Number(newId);
      
      // Завантажуємо новий товар
      await productStore.fetchProductById(id);
      
      // Опціонально: завантажуємо сесію, якщо в URL є ?session=...
      const uuid = route.query.session as string | undefined;
      if (uuid) {
        sessionData.value = await groupBuyStore.fetchSession(uuid);
      } else {
        sessionData.value = null; // Скидаємо сесію від попереднього товару
      }

      // Скролимо сторінку нагору, щоб користувач побачив новий товар
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }
)

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

const productBreadcrumbs = computed(() => {
  const catId = productStore.currentProduct?.categoryId
  const cat = categoryStore.flatCategories.find(c => c.id === catId)
  
  return [
    { name: 'Головна', path: '/' },
    { name: cat ? cat.name : 'Каталог', path: catId ? `/?categoryId=${catId}` : '/catalog' },
    { name: productStore.currentProduct?.title ?? 'Товар' },
  ]
})
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">
    <Header ref="headerRef" :showCategories="false" />
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
        <RelatedProducts 
          v-if="productStore.currentProduct?.categoryId"
          :category-id="productStore.currentProduct.categoryId" 
          :current-product-id="productStore.currentProduct.id" 
        />
      </div>
    </main>

    <Footer />

    <GroupBuyModal
      v-if="isGroupBuyModalOpen"
      :product-id="productId"
      @close="closeGroupModal"
      @open-auth="handleOpenAuth"
    />
  </div>
</template>
