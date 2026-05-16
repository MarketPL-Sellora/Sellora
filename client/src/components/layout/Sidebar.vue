<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCategoryStore, type Category } from '../../state/categoryStore'
import { useProductStore } from '../../state/productStore' // Імпортуємо стор товарів

// --- ДОДАНО ДЛЯ МОБІЛЬНОГО МЕНЮ ---
const props = defineProps<{
  isOpen?: boolean
}>()

// Ініціалізуємо стори
const categoryStore = useCategoryStore()
const productStore = useProductStore()
const route = useRoute()
const router = useRouter()

function syncCategoryFromUrl() {
  const catIdParam = route.query.categoryId
  if (!catIdParam) {
    activeCategory.value = null
    openCategoryId.value = null
    activeSubcategory.value = null
    return
  }
  
  const catId = Number(catIdParam)
  let found = false
  
  for (const rootCat of categoryStore.categories) {
    if (rootCat.id === catId) {
      activeCategory.value = rootCat.name
      openCategoryId.value = null
      activeSubcategory.value = null
      found = true
      break
    }
    
    if (rootCat.children && rootCat.children.length > 0) {
      const subCat = rootCat.children.find(c => c.id === catId)
      if (subCat) {
        activeCategory.value = rootCat.name
        openCategoryId.value = rootCat.id
        activeSubcategory.value = subCat.id
        found = true
        break
      }
    }
  }
  
  if (!found) {
    activeCategory.value = null
    openCategoryId.value = null
    activeSubcategory.value = null
  }
  // ─── Group Buy sync ───────────────────────────────────────────────────────
  isGroupBuyActive.value = route.query.groupMode === 'ONLY_GROUP'

  if (route.query.minPrice) priceMin.value = Number(route.query.minPrice)
  else priceMin.value = PRICE_ABSOLUTE_MIN

  if (route.query.maxPrice) priceMax.value = Number(route.query.maxPrice)
  else priceMax.value = PRICE_ABSOLUTE_MAX
}

onMounted(async () => {
  if (categoryStore.categories.length === 0) {
    await categoryStore.fetchCategories()
  }
  syncCategoryFromUrl()
})

watch(() => route.query.categoryId, syncCategoryFromUrl)

// ─── Accordion state ──────────────────────────────────────────────────────────

const activeCategory   = ref<string | null>(null)
const openCategoryId   = ref<number | null>(null)
const activeSubcategory = ref<number | null>(null)

function toggleCategory(id: number) {
  openCategoryId.value = openCategoryId.value === id ? null : id
}

function selectCategory(cat: Category) {
  activeCategory.value = cat.name
  activeSubcategory.value = null
  if (!cat.children?.length) {
    openCategoryId.value = null
  } else {
    toggleCategory(cat.id)
  }
  router.replace({ query: { ...route.query, categoryId: cat.id } })
  emit('category', { id: cat.id, name: cat.name })
}

function selectSubcategory(sub: Category) {
  activeSubcategory.value = sub.id
  // Не змінюємо activeCategory, щоб підсвічувався батько, але передаємо дані саб-категорії
  router.replace({ query: { ...route.query, categoryId: sub.id } })
  emit('category', { id: sub.id, name: sub.name })
}

// ─── Group Buy toggle ────────────────────────────────────────────────────────

const isGroupBuyActive = ref(false)

function toggleGroupBuy() {
  isGroupBuyActive.value = true
  router.replace({
    query: {
      ...route.query,
      groupMode: 'ONLY_GROUP',
    },
  })
}

// ─── Price filter ─────────────────────────────────────────────────────────────

const PRICE_ABSOLUTE_MIN = 0
const PRICE_ABSOLUTE_MAX = 100000
const priceMin = ref(0)
const priceMax = ref(100000)

const sliderFillLeft = computed(() =>
  ((priceMin.value - PRICE_ABSOLUTE_MIN) / (PRICE_ABSOLUTE_MAX - PRICE_ABSOLUTE_MIN)) * 100
)

const sliderFillWidth = computed(() =>
  ((priceMax.value - priceMin.value) / (PRICE_ABSOLUTE_MAX - PRICE_ABSOLUTE_MIN)) * 100
)

function applyFilter() {
  router.replace({
    query: {
      ...route.query,
      minPrice: priceMin.value > PRICE_ABSOLUTE_MIN ? priceMin.value : undefined,
      maxPrice: priceMax.value < PRICE_ABSOLUTE_MAX ? priceMax.value : undefined,
    }
  })
  emit('filter', { priceMin: priceMin.value, priceMax: priceMax.value })
}

// ─── Reset Logic ─────────────────────────────────────────────────────────────

function handleReset() {
  // 1. Скидаємо локальні фільтри ціни
  priceMin.value = PRICE_ABSOLUTE_MIN
  priceMax.value = PRICE_ABSOLUTE_MAX

  // 2. Скидаємо group buy
  isGroupBuyActive.value = false

  // 3. Скидаємо локальні змінні активної категорії
  activeCategory.value = null
  openCategoryId.value = null
  activeSubcategory.value = null

  // 4. Очищаємо URL від параметрів (зокрема categoryId) без перезавантаження сторінки
  router.replace({ query: {} })

  // 5. Скидаємо фільтри в сторі (це викличе новий запит за всіма товарами)
  productStore.resetFilters()
  
  // 6. Закриваємо мобільне меню, якщо воно відкрите
  emit('close')
}

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'filter', payload: { priceMin: number; priceMax: number }): void
  (e: 'category', payload: { id: number; name: string }): void
  (e: 'close'): void // ДОДАНО ДЛЯ МОБІЛЬНОГО МЕНЮ
}>()
</script>

<template>
  <div
    class="lg:block z-50 transition-all duration-300"
    :class="[
      isOpen ? 'fixed inset-0 bg-black/60 backdrop-blur-sm lg:static lg:bg-transparent lg:backdrop-blur-none' : 'hidden lg:block',
    ]"
    @click.self="emit('close')"
  >
    <aside
      class="h-full max-h-[100dvh] lg:max-h-none w-72 lg:w-52 overflow-y-auto lg:overflow-visible bg-[#0f1117] lg:bg-transparent p-4 lg:p-0 flex flex-col justify-start items-start gap-4 transition-transform duration-300 transform lg:transform-none"
      :class="isOpen ? 'translate-x-0' : '-translate-x-full'"
    >

      <div class="lg:hidden w-full flex justify-between items-center pb-2 border-b border-white/10 shrink-0">
        <span class="text-white font-bold font-['Unbounded']">Фільтри</span>
        <button
          class="p-2 text-gray-400 hover:text-white transition-colors"
          @click="emit('close')"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>

      <div
        class="w-full shrink-0 pt-4 pb-3 bg-[#1c1f2a] rounded-2xl outline outline-1 outline-offset-[-1px] outline-white/5 flex flex-col justify-start items-center gap-2 overflow-hidden"
      >
        <div class="w-full px-4 flex flex-col justify-start items-start">
          <span class="text-gray-600 text-[9px] font-semibold font-['Unbounded'] uppercase leading-4 tracking-wide">
            Категорії
          </span>
        </div>

        <div class="self-stretch flex flex-col">
          <template v-for="cat in categoryStore.categories" :key="cat.id">
            <button
              class="w-full px-4 py-2.5 border-l-2 inline-flex justify-start items-center gap-3 transition-all duration-150 group"
              :class="
                activeCategory === cat.name
                  ? 'bg-orange-500/10 border-orange-500'
                  : 'border-transparent hover:bg-white/5 hover:border-white/20 active:bg-white/10'
              "
              @click="selectCategory(cat)"
            >
              <span class="flex-1 text-sm leading-5 font-['Onest'] transition-colors duration-150 truncate text-left"
                    :class="activeCategory === cat.name ? 'text-orange-500 font-medium' : 'text-gray-400 font-normal group-hover:text-gray-200'"
              >
                {{ cat.name }}
              </span>

              <svg v-if="cat.children?.length" class="w-3 h-3 shrink-0 transition-transform duration-200"
                   :class="[openCategoryId === cat.id ? 'rotate-180' : 'rotate-0', activeCategory === cat.name ? 'text-orange-500' : 'text-gray-600 group-hover:text-gray-400']"
                   viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.5"
              >
                <path d="M2.5 4.5L6 8L9.5 4.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>

            <Transition
              enter-active-class="transition-all duration-200 ease-out overflow-hidden"
              leave-active-class="transition-all duration-150 ease-in overflow-hidden"
              enter-from-class="max-h-0 opacity-0"
              enter-to-class="max-h-60 opacity-100"
            >
              <div v-if="cat.children?.length && openCategoryId === cat.id" class="flex flex-col bg-black/20 border-l-2 border-orange-500/30 ml-0">
                <button
                  v-for="sub in cat.children" :key="sub.id"
                  class="w-full pl-10 pr-4 py-2 inline-flex justify-start items-center transition-all duration-100 group/sub"
                  :class="activeSubcategory === sub.id ? 'bg-orange-500/10 text-orange-400' : 'text-gray-500 hover:bg-white/5 hover:text-gray-200'"
                  @click="selectSubcategory(sub)"
                >
                  <span class="w-1 h-1 rounded-full shrink-0 mr-2.5 transition-colors duration-100"
                        :class="activeSubcategory === sub.id ? 'bg-orange-400' : 'bg-gray-600 group-hover/sub:bg-gray-400'"
                  />
                  <span class="text-xs font-normal font-['Onest'] leading-4 truncate text-left">{{ sub.name }}</span>
                </button>
              </div>
            </Transition>
          </template>
        </div>

      </div>

      <div class="w-full shrink-0 mb-8 lg:mb-0 p-4 bg-[#1c1f2a] rounded-2xl outline outline-1 outline-offset-[-1px] outline-white/5 flex flex-col justify-start items-start gap-2">
        <span class="self-stretch text-gray-600 text-[9px] font-bold font-['Unbounded'] uppercase leading-4 tracking-wide">Фільтр</span>
        <span class="self-stretch pt-1 text-gray-300 text-sm font-semibold font-['Onest'] leading-5">Ціна, ₴</span>

        <div class="self-stretch pb-1 inline-flex justify-start items-center gap-2">
          <input v-model.number="priceMin" type="number" min="0" :max="priceMax" class="flex-1 w-0 min-w-0 px-2.5 py-1.5 bg-[#1c1f2a] rounded-lg outline outline-1 outline-white/10 text-gray-300 text-xs font-normal font-['Onest'] focus:outline-orange-500/40 focus:outline-2" />
          <span class="text-gray-600 text-base font-normal font-['Onest'] shrink-0">—</span>
          <input v-model.number="priceMax" type="number" :min="priceMin" max="100000" class="flex-1 w-0 min-w-0 px-2.5 py-1.5 bg-[#1c1f2a] rounded-lg outline outline-1 outline-white/10 text-gray-300 text-xs font-normal font-['Onest'] focus:outline-orange-500/40 focus:outline-2" />
        </div>

        <div class="self-stretch h-1.5 relative bg-[#2a2d3e] rounded-full overflow-hidden">
          <div class="h-1.5 absolute top-0 bg-gradient-to-r from-orange-500 to-amber-400 rounded-full transition-all duration-200" :style="{ left: sliderFillLeft + '%', width: sliderFillWidth + '%' }" />
        </div>

        <!-- Банер "РАЗОМ — ДЕШЕВШЕ" -->
        <div class="self-stretch mt-6 mb-4 p-3 bg-gradient-to-br from-orange-500/20 to-orange-600/10 rounded-xl outline outline-1 outline-offset-[-1px] outline-orange-500/20 flex flex-col justify-start items-start gap-[3.30px]">
          <span class="self-stretch text-orange-500 text-[10px] font-bold font-['Unbounded'] leading-4">РАЗОМ — ДЕШЕВШЕ</span>
          <p class="self-stretch pb-1.5 text-gray-400 text-xs font-normal font-['Onest'] leading-5">Об'єднуйся з іншими та<br />купуй зі знижкою</p>
          <button
            class="self-stretch py-1.5 rounded-lg outline outline-1 outline-offset-[-1px] text-xs font-semibold font-['Onest'] leading-4 text-center transition-all duration-150 active:scale-[0.98]"
            :class="
              isGroupBuyActive
                ? 'bg-orange-500 text-white outline-orange-500 hover:bg-orange-400 shadow-[0px_2px_12px_0px_rgba(249,115,22,0.45)]'
                : 'bg-orange-500/10 text-orange-500 outline-orange-500/25 hover:bg-orange-500/20 hover:outline-orange-500/50'
            "
            @click="toggleGroupBuy"
          >
            Показати товари
          </button>
        </div>

        <div class="self-stretch flex flex-col gap-2">
          <button
            class="self-stretch py-2 bg-gradient-to-b from-orange-500 to-orange-600 rounded-xl shadow-[0px_4px_16px_0px_rgba(249,115,22,0.35)] text-white text-xs font-semibold font-['Onest'] transition-all hover:from-orange-400 hover:to-orange-500 active:scale-[0.98]"
            @click="applyFilter"
          >
            Застосувати
          </button>

          <button
            class="self-stretch py-2 bg-transparent border border-white/10 rounded-xl text-gray-400 text-xs font-semibold font-['Onest'] transition-all hover:bg-white/5 hover:text-white active:scale-[0.98]"
            @click="handleReset"
          >
            Скинути фільтри
          </button>
        </div>
      </div>

    </aside>
  </div>
</template>

<style scoped>
input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type='number'] {
  -moz-appearance: textfield;
}

/* Кастомний скролбар для мобільного меню, щоб виглядав акуратно */
aside::-webkit-scrollbar {
  width: 4px;
}
aside::-webkit-scrollbar-track {
  background: transparent;
}
aside::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}
</style>