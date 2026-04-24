<script setup lang="ts">
import { ref } from 'vue'
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'
import HeroBanner from './HeroBanner.vue'
import ProductGrid from './ProductGrid.vue'
import Footer from './Footer.vue'

// 1. Імпортуємо стор, щоб викликати завантаження даних
import { useProductStore } from '../state/productStore'

const productStore = useProductStore()

// --- Стан для мобільного меню фільтрів ---
const isMobileMenuOpen = ref(false)

// 2. Функція для фільтрації (ціна + бренди)
// Спрацьовує, коли в Сайдбарі натискають "Застосувати"
function handleFilter(payload: { priceMin: number; priceMax: number; brands: string[] }) {
  productStore.fetchProducts({
    minPrice: payload.priceMin,
    maxPrice: payload.priceMax,
    page: 0 // Скидаємо на першу сторінку
  })
  isMobileMenuOpen.value = false // Закриваємо меню після застосування
}

// 3. Функція для вибору категорії
function handleCategory(name: string) {
  productStore.fetchProducts({
    keyword: name,
    page: 0
  })
  isMobileMenuOpen.value = false // Закриваємо меню після вибору
}
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-4 md:px-6 py-4 md:py-8 flex flex-col lg:flex-row gap-6 lg:gap-8">

      <aside class="w-full lg:w-64 shrink-0">
        <Sidebar
          :is-open="isMobileMenuOpen"
          @close="isMobileMenuOpen = false"
          @filter="handleFilter"
          @category="handleCategory"
        />
      </aside>

      <section class="flex-1 flex flex-col gap-6 lg:gap-10 min-w-0">

        <HeroBanner />

        <div class="lg:hidden w-full">
          <button
            class="w-full py-3 bg-[#1c1f2a] rounded-xl outline outline-1 outline-white/10 text-white text-sm font-semibold flex justify-center items-center gap-2 transition-all active:scale-[0.98] active:bg-[#252a3a] shadow-sm"
            @click="isMobileMenuOpen = true"
          >
            <svg class="w-5 h-5 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"></path>
            </svg>
            Фільтри та Категорії
          </button>
        </div>

        <ProductGrid />

      </section>

    </main>

    <Footer />

  </div>
</template>

<style>
/* Твої стилі залишаються без змін */
body {
  margin: 0;
  padding: 0;
  overflow-x: hidden;
}

.scrollbar-none::-webkit-scrollbar {
  display: none;
}
.scrollbar-none {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
