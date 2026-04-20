<script setup lang="ts">
// Імпортуємо твою готові картку!
import ProductCard from './ProductCard.vue'
</script>

<template>
  <div class="w-full flex flex-col gap-6 py-8">

    <div class="flex justify-between items-end border-b border-white/10 pb-4">
      <div class="flex items-baseline gap-3">
        <h2 class="text-white text-2xl font-bold font-['Unbounded']">Популярні товари</h2>
        <span class="text-gray-500 text-sm font-['Onest']">1 247 позицій</span>
      </div>

      <div class="flex items-center gap-2">
        <span class="text-gray-500 text-sm font-['Onest']">Сортувати:</span>
        <button class="px-3 py-1.5 bg-[#1c1f2a] rounded-lg text-gray-300 text-sm font-['Onest'] outline outline-1 outline-white/10 transition-all hover:bg-white/5">
          За популярністю
        </button>
      </div>
    </div>

    <div class="grid grid-cols-4 gap-4">
      <ProductCard v-for="i in 8" :key="i" />
    </div>

    <div class="flex justify-center mt-4">
      <button class="px-6 py-2.5 bg-[#1c1f2a] rounded-xl outline outline-1 outline-white/10 text-gray-400 text-sm font-['Onest'] transition-all hover:bg-white/5 hover:text-white">
        Показати ще 24 товари ↓
      </button>
    </div>

  </div>
</template>
