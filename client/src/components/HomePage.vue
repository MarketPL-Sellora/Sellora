<script setup lang="ts">
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'
import HeroBanner from './HeroBanner.vue'
import ProductGrid from './ProductGrid.vue'
import Footer from './Footer.vue'

// 1. Імпортуємо стор, щоб викликати завантаження даних
import { useProductStore } from '../state/productStore'

const productStore = useProductStore()

// 2. Функція для фільтрації (ціна + бренди)
// Спрацьовує, коли в Сайдбарі натискають "Застосувати"
function handleFilter(payload: { priceMin: number; priceMax: number; brands: string[] }) {
  productStore.fetchProducts({
    minPrice: payload.priceMin,
    maxPrice: payload.priceMax,
    page: 0 // Скидаємо на першу сторінку
  })
}

// 3. Функція для вибору категорії
function handleCategory(name: string) {
  productStore.fetchProducts({
    keyword: name,
    page: 0
  })
}
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-6 py-8 flex gap-8">

      <aside class="w-64 shrink-0">
        <Sidebar
          @filter="handleFilter"
          @category="handleCategory"
        />
      </aside>

      <section class="flex-1 flex flex-col gap-10 min-w-0">
        <HeroBanner />

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
