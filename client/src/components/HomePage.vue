<script setup lang="ts">
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'
import HeroBanner from './HeroBanner.vue'
import ProductGrid from './ProductGrid.vue'
import Footer from './Footer.vue'
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-6 py-8 flex gap-8">

      <aside class="w-64 shrink-0">
        <Sidebar />
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
/* Глобальні стилі для всієї сторінки */
body {
  margin: 0;
  padding: 0;
  overflow-x: hidden;
}

/* Прибираємо стандартні скролбари, де вони заважають */
.scrollbar-none::-webkit-scrollbar {
  display: none;
}
.scrollbar-none {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
