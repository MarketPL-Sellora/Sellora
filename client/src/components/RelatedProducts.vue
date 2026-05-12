<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import ProductCard from './ProductCard.vue'
import { apiClient } from '../api/axios'
import type { ProductApiItem } from '../state/productStore'

const props = defineProps<{
  categoryId: number
  currentProductId: number
}>()

const relatedProducts = ref<ProductApiItem[]>([])
const isLoading = ref(true)

async function fetchRelated() {
  if (!props.categoryId) return
  isLoading.value = true
  try {
    // Запитуємо 5 товарів, щоб точно вистачило 4 після видалення поточного
    const res = await apiClient.get('/products', {
      params: { categoryId: props.categoryId, page: 0, size: 5 }
    })
    
    const items = res.data.content || []
    
    // Відфільтровуємо поточний товар і беремо перші 4
    relatedProducts.value = items
      .filter((item: ProductApiItem) => item.id !== props.currentProductId)
      .slice(0, 4)
  } catch (error) {
    console.error('Failed to fetch related products', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchRelated)

// Оновлюємо, якщо користувач перейшов на інший товар з цього ж блоку
watch(() => props.currentProductId, fetchRelated)
</script>

<template>
  <div v-if="relatedProducts.length > 0" class="w-full flex flex-col gap-6">
    <div class="flex justify-between items-end border-b border-white/10 pb-4">
      <h2 class="text-white text-2xl font-bold font-['Unbounded']">Схожі товари</h2>
    </div>

    <div class="w-full flex overflow-x-auto gap-4 pb-4 scrollbar-none -mx-4 px-4 sm:mx-0 sm:px-0 sm:grid sm:grid-cols-2 lg:grid-cols-4">
      <ProductCard
        v-for="item in relatedProducts"
        :key="item.id"
        :product="item"
        class="w-[260px] shrink-0 lg:w-full lg:shrink"
      />
    </div>
  </div>
</template>
