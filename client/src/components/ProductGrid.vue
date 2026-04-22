<script setup lang="ts">
import { ref } from 'vue'
import ProductCard from './ProductCard.vue'

// ─── Тестові дані (Моки) ──────────────────────────────────────────────────────
const products = ref([
  {
    id: 1,
    brand: 'Samsung',
    name: 'Samsung Galaxy S24 Ultra 256GB',
    image: '../assets/product-placeholder.png',
    imageAlt: 'Samsung Galaxy S24 Ultra',
    rating: 5,
    reviewCount: 284,
    groupLabel: 'Учасників',
    groupCurrent: 14,
    groupTotal: 20,
    price: 45000,
    oldPrice: 50000,
  },
  {
    id: 2,
    brand: 'Apple',
    name: 'Apple iPhone 15 Pro 128GB',
    image: '../assets/product-placeholder.png',
    imageAlt: 'iPhone 15 Pro',
    rating: 5,
    reviewCount: 512,
    groupLabel: 'Учасників',
    groupCurrent: 19,
    groupTotal: 20,
    price: 42000,
    oldPrice: 46000,
  },
  {
    id: 3,
    brand: 'ASUS',
    name: 'Ноутбук ASUS ROG Strix G16',
    image: '../assets/product-placeholder.png',
    imageAlt: 'ASUS ROG',
    rating: 4,
    reviewCount: 89,
    groupLabel: 'Учасників',
    groupCurrent: 3,
    groupTotal: 10,
    price: 65000,
    oldPrice: 72000,
  },
  {
    id: 4,
    brand: 'Sony',
    name: 'Навушники Sony WH-1000XM5',
    image: '../assets/product-placeholder.png',
    imageAlt: 'Sony Headphones',
    rating: 5,
    reviewCount: 156,
    groupLabel: 'Учасників',
    groupCurrent: 8,
    groupTotal: 15,
    price: 14000,
    oldPrice: 16500,
  }
])
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
      <ProductCard
        v-for="item in products"
        :key="item.id"
        :product="item"
      />
    </div>

    <div class="flex justify-center mt-4">
      <button class="px-6 py-2.5 bg-[#1c1f2a] rounded-xl outline outline-1 outline-white/10 text-gray-400 text-sm font-['Onest'] transition-all hover:bg-white/5 hover:text-white">
        Показати ще 24 товари ↓
      </button>
    </div>

  </div>
</template>
