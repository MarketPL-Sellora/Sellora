<script setup lang="ts">
import { ref } from 'vue'
import Header          from './Header.vue'
import Breadcrumbs     from './Breadcrumbs.vue'
import Footer          from './Footer.vue'
import ProductDetails  from './ProductDetails.vue'
import ProductGallery  from './ProductGallery.vue'
import ProductTabs     from './ProductTabs.vue'
import RelatedProducts from './RelatedProducts.vue'
import GroupBuyModal   from './GroupModal.vue'

// —— Стан модального вікна групової покупки ——
// Керує видимістю модального вікна «Відкрити збір».
const isGroupBuyModalOpen = ref(false)

// —— Хлібні крихти сторінки товару ——
// Масив передається у компонент <Breadcrumbs :items="productBreadcrumbs">.
// Останній елемент (без поля path) — поточна сторінка, не клікабельна.
const productBreadcrumbs = ref([
  {
    name: 'Головна',  // Назва кроку — посилання на корінь сайту
    path: '/',        // Маршрут переходу
  },
  {
    name: 'Ноутбуки', // Назва кроку — категорія товарів
    path: '/laptops', // Маршрут категорії
  },
  {
    name: 'MacBook Pro 16", M3 Pro, 18/512GB', // Назва поточного товару
    // path відсутній — це поточна сторінка, router-link не рендериться
  },
])
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header :showCategories="false" />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-6 py-8 md:py-12 flex flex-col gap-8 md:gap-12">

      <!-- —— Хлібні крихти ——
           Передаємо масив productBreadcrumbs через prop :items.
           Компонент сам визначить, який елемент клікабельний, а який — поточна сторінка. -->
      <Breadcrumbs :items="productBreadcrumbs" />

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-10 items-start">
        <div class="w-full">
          <ProductGallery />
        </div>

        <div class="w-full">
          <!-- ProductDetails емітує 'start-group' при кліку на кнопку -->
          <ProductDetails @start-group="isGroupBuyModalOpen = true" />
        </div>
      </div>

      <div class="flex flex-col gap-16">
        <ProductTabs />
        <RelatedProducts />
      </div>

    </main>

    <Footer />

    <!-- ── Group buy modal ──────────────────────────────────────────────── -->
    <GroupBuyModal
      v-if="isGroupBuyModalOpen"
      @close="isGroupBuyModalOpen = false"
    />

  </div>
</template>
