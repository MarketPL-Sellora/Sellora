<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useProductStore } from '../state/productStore'
import ProductCard from './ProductCard.vue'

const productStore = useProductStore()

onMounted(() => {
  productStore.fetchProducts()
})

const mappedProducts = computed(() =>
  productStore.products.map((item) => ({
    id:           item.id,
    name:         item.title,
    price:        item.groupPrice,
    oldPrice:     item.standardPrice,
    groupTotal:   item.groupTargetSize,
    groupCurrent: item.groupCurrentSize ?? 1,
    image:        item.images && item.images.length > 0
      ? item.images[0]
      : '/src/assets/product-placeholder.png',
    imageAlt:     item.title,
    brand:        'Brand',
    rating:       5,
    reviewCount:  12,
    groupLabel:   'Учасників',
  }))
)
</script>

<template>
  <div class="flex flex-col gap-6 py-8 min-h-[800px]">

    <div class="flex justify-between items-end border-b border-white/10 pb-4">
      <div class="flex items-baseline gap-3">
        <h2 class="text-white text-2xl font-bold font-['Unbounded']">Популярні товари</h2>
        <span class="text-gray-500 text-sm font-['Onest']">
          {{ productStore.totalElements }} позицій
        </span>
      </div>

      <div class="flex items-center gap-2">
        <span class="text-gray-500 text-sm font-['Onest']">Сортувати:</span>
        <button class="px-3 py-1.5 bg-[#1c1f2a] rounded-lg text-gray-300 text-sm font-['Onest'] outline outline-1 outline-white/10 transition-all hover:bg-white/5">
          За популярністю
        </button>
      </div>
    </div>

    <Transition name="fade" mode="out-in">

      <div v-if="productStore.isLoading" key="loading" class="grid grid-cols-4 gap-4">
        <div
          v-for="n in 8"
          :key="n"
          class="w-72 h-96 bg-[#1c1f2a] rounded-2xl outline outline-1 outline-white/5 animate-pulse"
        />
      </div>

      <div v-else-if="productStore.error" key="error" class="flex flex-col items-center gap-4 py-16 text-center">
        <span class="text-4xl">⚠️</span>
        <p class="text-gray-400 text-sm font-['Onest']">
          Помилка завантаження: {{ productStore.error }}
        </p>
        <button
          class="px-4 py-2 bg-orange-500/10 rounded-xl outline outline-1 outline-orange-500/30 text-orange-400 text-sm font-['Onest'] transition-all hover:bg-orange-500/20"
          @click="productStore.fetchProducts()"
        >
          Спробувати ще раз
        </button>
      </div>

      <div v-else-if="mappedProducts.length === 0" key="empty" class="flex flex-col items-center gap-4 py-16 text-center">
        <span class="text-4xl">🔍</span>
        <p class="text-gray-400 text-sm font-['Onest']">
          Товарів за вашим запитом не знайдено
        </p>
      </div>

      <div v-else key="content" class="grid grid-cols-4 gap-4">
        <ProductCard
          v-for="item in mappedProducts"
          :key="item.id"
          :product="item"
        />
      </div>
    </Transition>

    <div
      v-if="!productStore.isLoading && mappedProducts.length > 0"
      class="flex justify-center mt-4"
    >
      <button
        class="px-6 py-2.5 bg-[#1c1f2a] rounded-xl outline outline-1 outline-white/10 text-gray-400 text-sm font-['Onest'] transition-all hover:bg-white/5 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed"
        :disabled="(productStore.filters.page + 1) * productStore.filters.size >= productStore.totalElements"
        @click="productStore.fetchProducts({ page: productStore.filters.page + 1 })"
      >
        Показати ще {{ productStore.filters.size }} товари ↓
      </button>
    </div>

  </div>
</template>

<style scoped>
/* Плавне зникнення та поява без "білих кадрів" */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.1s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
