<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useProductStore } from '../../state/productStore'
import { useCategoryStore } from '../../state/categoryStore'
import ProductCard from './ProductCard.vue'

const route = useRoute()
const productStore = useProductStore()
const categoryStore = useCategoryStore()

const sortOptions = [
  { value: '', label: 'За замовчуванням' },
  { value: 'standardPrice,asc', label: 'Від дешевих до дорогих' },
  { value: 'standardPrice,desc', label: 'Від дорогих до дешевих' }
]

const isSortOpen = ref(false)
const selectedSort = ref(
  productStore.filters.sortBy && productStore.filters.sortDir
    ? `${productStore.filters.sortBy},${productStore.filters.sortDir}`
    : ''
)

const selectedSortObj = computed(() => 
  sortOptions.find(o => o.value === selectedSort.value) || sortOptions[0]
)

function handleSortSelect(opt: { value: string, label: string }) {
  selectedSort.value = opt.value
  isSortOpen.value = false
  
  if (!opt.value) {
    productStore.fetchProducts({ page: 0, sortBy: undefined, sortDir: undefined })
  } else {
    const [sortBy, sortDir] = opt.value.split(',')
    productStore.fetchProducts({ page: 0, sortBy, sortDir })
  }
}

// Закриття дропдауна при кліку поза ним
function closeSortOutside(e: Event) {
  if (!(e.target as Element).closest('.sort-dropdown-container')) {
    isSortOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', closeSortOutside)
  productStore.fetchProducts()
})

onUnmounted(() => {
  document.removeEventListener('click', closeSortOutside)
})

// Ми більше не перейменовуємо price на groupPrice!
// Ми просто передаємо об'єкт з бекенду як є, додаючи лише декілька заглушок для бренду та рейтингу.
const mappedProducts = computed(() =>
  productStore.products.map((item) => ({
    ...item, // Передаємо всі поля бекенду без змін (standardPrice, groupPrice, groupTargetSize тощо)
    name:         item.title, // Залишаємо name для сумісності
    image:        item.images && item.images.length > 0
      ? item.images[0]
      : '/src/assets/product-placeholder.png',
    imageAlt:     item.title,
    rating:       5,
    reviewCount:  12,
    groupLabel:   'Учасників',
  }))
)

const storeName = computed(() => {
  const items = mappedProducts.value
  return items && items.length > 0 ? (items[0] as any).storeName || (items[0] as any).store_name || '' : ''
})

const gridTitle = computed(() => {
  // 1. Якщо ми дивимось товари конкретного магазину
  if (route.query.storeId) {
    return 'Товари продавця'
  }

  // 2. Якщо ми фільтруємо за категорією, спробуємо знайти її назву
  if (route.query.categoryId) {
    const catId = Number(route.query.categoryId)
    let catName = 'Товари категорії'

    for (const rootCat of categoryStore.categories) {
      if (rootCat.id === catId) {
        catName = rootCat.name
        break
      }
      if (rootCat.children && rootCat.children.length > 0) {
        const subCat = rootCat.children.find(c => c.id === catId)
        if (subCat) {
          catName = subCat.name
          break
        }
      }
    }
    return catName
  }

  // 3. Стандартний заголовок
  return 'Популярні товари'
})

const remainingItems = computed(() => {
  const currentlyLoaded = productStore.products.length;
  const total = productStore.totalElements;
  const remaining = total - currentlyLoaded;
  // Показуємо розмір сторінки (24), але якщо залишилось менше, то реальну кількість
  return remaining > productStore.filters.size ? productStore.filters.size : remaining;
});

function handleWishlistUpdate(payload: { id: number, isFavorite: boolean }) {
  // Якщо ми на сторінці улюблених і юзер зняв лайк
  if (route.path === '/favorites' && !payload.isFavorite) {
    // Видаляємо товар з локального стору для миттєвого зникнення
    productStore.products = productStore.products.filter(p => p.id !== payload.id)
    productStore.totalElements = Math.max(0, productStore.totalElements - 1)
  }
}
</script>

<template>
  <div class="flex flex-col gap-6 py-8 min-h-[800px]">

    <div v-if="route.path !== '/cabinet'" class="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 border-b border-white/10 pb-4">
      <div class="flex items-baseline gap-3">
        <h2 class="text-white text-2xl font-bold font-['Unbounded']">{{ gridTitle }} <span v-if="storeName && route.query.storeId" class="text-orange-500 ml-2">{{ storeName }}</span></h2>
        <span class="text-gray-500 text-sm font-['Onest']">
          {{ productStore.totalElements }} позицій
        </span>
      </div>

      <div class="flex items-center gap-2 sort-dropdown-container relative">
        <span class="text-gray-500 text-sm font-['Onest']">Сортувати:</span>
        
        <div class="relative">
          <button
            @click="isSortOpen = !isSortOpen"
            class="flex items-center justify-between w-56 px-4 py-2 bg-[#1c1f2a] rounded-xl text-gray-300 text-sm font-['Onest'] outline outline-1 outline-white/10 transition-all hover:bg-white/5 focus:outline-orange-500"
          >
            <span>{{ selectedSortObj.label }}</span>
            <svg 
              class="w-4 h-4 text-gray-400 transition-transform duration-200" 
              :class="{ 'rotate-180': isSortOpen }" 
              viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            >
              <path d="M4 6l4 4 4-4"/>
            </svg>
          </button>
      
          <transition name="fade">
            <div 
              v-if="isSortOpen" 
              class="absolute right-0 z-50 w-56 mt-2 bg-[#161820] border border-[#2a2d3e] rounded-xl shadow-[0_8px_30px_rgb(0,0,0,0.5)] overflow-hidden py-1.5"
            >
              <button
                v-for="opt in sortOptions"
                :key="opt.value"
                @click="handleSortSelect(opt)"
                class="w-full text-left px-4 py-2.5 text-sm font-['Onest'] transition-colors hover:bg-white/5"
                :class="selectedSort === opt.value ? 'text-orange-400 bg-orange-500/10 font-medium' : 'text-gray-300'"
              >
                {{ opt.label }}
              </button>
            </div>
          </transition>
        </div>
      </div>
    </div>

    <Transition name="fade" mode="out-in">

      <div v-if="productStore.isLoading" key="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
        <div
          v-for="n in 8"
          :key="n"
          class="w-full h-96 bg-[#1c1f2a] rounded-2xl outline outline-1 outline-white/5 animate-pulse"
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

      <TransitionGroup tag="div" name="list" key="content" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
        <ProductCard
          v-for="item in mappedProducts"
          :key="item.id"
          :product="item"
          @wishlist="handleWishlistUpdate"
        />
      </TransitionGroup>
    </Transition>

    <div
      v-if="remainingItems > 0"
      class="flex justify-center mt-4"
    >
      <button
        class="px-6 py-2.5 bg-[#1c1f2a] rounded-xl outline outline-1 outline-white/10 text-gray-400 text-sm font-['Onest'] transition-all hover:bg-white/5 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed"
        :disabled="productStore.isLoading"
        @click="productStore.fetchProducts({ page: productStore.filters.page + 1 })"
      >
        Показати ще {{ remainingItems }} товарів ↓
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

.list-move,
.list-enter-active,
.list-leave-active {
  transition: all 0.4s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
.list-leave-active {
  position: absolute;
  z-index: -1;
}
</style>
