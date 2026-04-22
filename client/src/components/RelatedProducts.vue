<script setup lang="ts">
import { ref } from 'vue'
import ProductCard from './ProductCard.vue'

// Ті самі тестові дані, щоб карткам було з чим працювати
const relatedProducts = ref([
  {
    id: 5,
    brand: 'Samsung',
    name: 'Samsung Galaxy Watch 6',
    image: '../assets/product-placeholder.png',
    imageAlt: 'Galaxy Watch',
    rating: 5,
    reviewCount: 112,
    groupLabel: 'Учасників',
    groupCurrent: 5,
    groupTotal: 10,
    price: 12000,
    oldPrice: 14000,
  },
  {
    id: 6,
    brand: 'Apple',
    name: 'Apple AirPods Pro 2',
    image: '../assets/product-placeholder.png',
    imageAlt: 'AirPods Pro',
    rating: 5,
    reviewCount: 340,
    groupLabel: 'Учасників',
    groupCurrent: 18,
    groupTotal: 20,
    price: 10500,
    oldPrice: 11500,
  },
  {
    id: 7,
    brand: 'Samsung',
    name: 'Бездротовий зарядний пристрій',
    image: '../assets/product-placeholder.png',
    imageAlt: 'Wireless Charger',
    rating: 4,
    reviewCount: 45,
    groupLabel: 'Учасників',
    groupCurrent: 2,
    groupTotal: 5,
    price: 1500,
    oldPrice: 2000,
  },
  {
    id: 8,
    brand: 'Spigen',
    name: 'Чохол для Samsung S24 Ultra',
    image: '../assets/product-placeholder.png',
    imageAlt: 'Case',
    rating: 5,
    reviewCount: 89,
    groupLabel: 'Учасників',
    groupCurrent: 10,
    groupTotal: 15,
    price: 800,
    oldPrice: 1200,
  }
])
</script>

<template>
  <div class="w-full flex flex-col gap-6">
    <div class="flex justify-between items-end border-b border-white/10 pb-4">
      <h2 class="text-white text-2xl font-bold font-['Unbounded']">З цим товаром купують</h2>
    </div>

    <div class="grid grid-cols-4 gap-4">
      <ProductCard
        v-for="item in relatedProducts"
        :key="item.id"
        :product="item"
      />
    </div>
  </div>
</template>
