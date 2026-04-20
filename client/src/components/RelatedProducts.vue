<script setup lang="ts">
import ProductCard from './ProductCard.vue'
</script>

<template>
  <div class="w-full flex flex-col gap-6 pt-16">

    <div class="flex justify-between items-end border-b border-white/10 pb-4">
      <h2 class="text-white text-2xl font-bold font-['Unbounded']">З цим товаром переглядають</h2>
      <button class="text-orange-500 text-sm font-['Onest'] hover:underline transition-all">
        Дивитися всі →
      </button>
    </div>

    <div class="grid grid-cols-4 gap-4">
      <ProductCard v-for="i in 4" :key="i" :simple="true" />
    </div>

  </div>
</template>
