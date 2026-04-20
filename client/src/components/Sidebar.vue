<script setup lang="ts">
import { ref, reactive } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface Category {
  emoji: string
  label: string
  hot?: boolean
  dividerBefore?: boolean
}

interface Brand {
  name: string
  count: number
}

// ─── Categories ───────────────────────────────────────────────────────────────

const categories: Category[] = [
  { emoji: '📱', label: 'Смартфони' },
  { emoji: '💻', label: 'Ноутбуки' },
  { emoji: '🎮', label: 'Ігрові консолі' },
  { emoji: '🎧', label: 'Аудіотехніка' },
  { emoji: '📷', label: 'Фото та відео' },
  { emoji: '👟', label: 'Взуття' },
  { emoji: '👕', label: 'Одяг' },
  { emoji: '🏠', label: 'Дім і сад' },
  { emoji: '🧴', label: 'Краса та догляд' },
  { emoji: '🔧', label: 'Інструменти' },
  { emoji: '🚗', label: 'Авто товари' },
  { emoji: '🐾', label: 'Зоотовари' },
  { emoji: '📚', label: 'Книги' },
  { emoji: '🔥', label: 'Групові покупки', hot: true, dividerBefore: true },
]

const activeCategory = ref('Смартфони')

// ─── Brands ───────────────────────────────────────────────────────────────────

const brands: Brand[] = [
  { name: 'Apple',   count: 42 },
  { name: 'Samsung', count: 38 },
  { name: 'Xiaomi',  count: 57 },
  { name: 'Sony',    count: 24 },
]

const selectedBrands = reactive<Set<string>>(new Set())

function toggleBrand(name: string) {
  selectedBrands.has(name) ? selectedBrands.delete(name) : selectedBrands.add(name)
}

// ─── Price filter ─────────────────────────────────────────────────────────────

const priceMin = ref(500)
const priceMax = ref(50000)
const PRICE_ABSOLUTE_MIN = 0
const PRICE_ABSOLUTE_MAX = 100000

const sliderFillLeft = computed(() => {
  return ((priceMin.value - PRICE_ABSOLUTE_MIN) / (PRICE_ABSOLUTE_MAX - PRICE_ABSOLUTE_MIN)) * 100
})

const sliderFillWidth = computed(() => {
  return ((priceMax.value - priceMin.value) / (PRICE_ABSOLUTE_MAX - PRICE_ABSOLUTE_MIN)) * 100
})

function applyFilter() {
  emit('filter', { priceMin: priceMin.value, priceMax: priceMax.value, brands: [...selectedBrands] })
}

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'filter', payload: { priceMin: number; priceMax: number; brands: string[] }): void
  (e: 'category', name: string): void
}>()

function selectCategory(label: string) {
  activeCategory.value = label
  emit('category', label)
}

// ─── Missing import ───────────────────────────────────────────────────────────
import { computed } from 'vue'
</script>

<template>
  <aside class="w-52 self-stretch inline-flex flex-col justify-start items-start gap-4">

    <!-- ── Categories card ─────────────────────────────────────────────────── -->
    <div
      class="self-stretch pt-4 pb-3 bg-[#1c1f2a] rounded-2xl outline outline-1 outline-offset-[-1px] outline-white/5 flex flex-col justify-start items-center gap-2 overflow-hidden"
    >
      <!-- Heading -->
      <div class="w-44 flex flex-col justify-start items-start">
        <span class="text-gray-600 text-[9px] font-semibold font-['Unbounded'] uppercase leading-4 tracking-wide">
          Категорії
        </span>
      </div>

      <!-- List -->
      <div class="self-stretch flex flex-col">
        <template v-for="cat in categories" :key="cat.label">
          <!-- Optional divider -->
          <div
            v-if="cat.dividerBefore"
            class="w-44 mx-auto h-px my-1.5 border-t border-white/5"
          />

          <button
            class="w-full px-4 py-2.5 border-l-2 inline-flex justify-start items-center gap-3 transition-all duration-150 group"
            :class="
              activeCategory === cat.label
                ? 'bg-orange-500/10 border-orange-500'
                : 'border-transparent hover:bg-white/5 hover:border-white/20 active:bg-white/10'
            "
            @click="selectCategory(cat.label)"
          >
            <!-- Emoji -->
            <span class="text-base font-normal font-['Segoe_UI_Emoji'] leading-6 shrink-0">
              {{ cat.emoji }}
            </span>

            <!-- Label -->
            <span
              class="text-sm leading-5 font-['Onest'] transition-colors duration-150 truncate"
              :class="
                activeCategory === cat.label
                  ? 'text-orange-500 font-medium'
                  : cat.hot
                  ? 'text-orange-500 font-semibold'
                  : 'text-gray-400 font-normal group-hover:text-gray-200'
              "
            >
              {{ cat.label }}
            </span>

            <!-- HOT badge -->
            <span
              v-if="cat.hot"
              class="ml-auto shrink-0 px-1.5 py-0.5 bg-orange-500 rounded-md text-white text-[9px] font-normal font-['Onest'] leading-4"
            >
              HOT
            </span>
          </button>
        </template>
      </div>

      <!-- Promo banner -->
      <div
        class="w-44 px-3 pt-4 pb-3 bg-gradient-to-br from-orange-500/20 to-orange-600/10 rounded-xl outline outline-1 outline-offset-[-1px] outline-orange-500/20 flex flex-col justify-start items-start gap-[3.30px]"
      >
        <span class="self-stretch text-orange-500 text-[10px] font-bold font-['Unbounded'] leading-4">
          РАЗОМ — ДЕШЕВШЕ
        </span>
        <p class="self-stretch pb-1.5 text-gray-400 text-xs font-normal font-['Onest'] leading-5">
          Об'єднуйся з іншими та<br />купуй зі знижкою до 30%
        </p>
        <button
          class="self-stretch py-1.5 bg-orange-500/10 rounded-lg outline outline-1 outline-offset-[-1px] outline-orange-500/25 text-orange-500 text-xs font-semibold font-['Onest'] leading-4 text-center transition-all duration-150 hover:bg-orange-500/20 hover:outline-orange-500/50 active:scale-[0.98]"
        >
          Дізнатись більше →
        </button>
      </div>
    </div>

    <!-- ── Filter card ─────────────────────────────────────────────────────── -->
    <div
      class="self-stretch p-4 bg-[#1c1f2a] rounded-2xl outline outline-1 outline-offset-[-1px] outline-white/5 flex flex-col justify-start items-start gap-2"
    >
      <!-- Section label -->
      <span class="self-stretch text-gray-600 text-[9px] font-bold font-['Unbounded'] uppercase leading-4 tracking-wide">
        Фільтр
      </span>

      <!-- Price heading -->
      <span class="self-stretch pt-1 text-gray-300 text-sm font-semibold font-['Onest'] leading-5">
        Ціна, ₴
      </span>

      <!-- Price inputs -->
      <div class="self-stretch pb-1 inline-flex justify-start items-center gap-2">
        <input
          v-model.number="priceMin"
          type="number"
          min="0"
          :max="priceMax"
          class="flex-1 w-0 min-w-0 px-2.5 py-1.5 bg-[#1c1f2a] rounded-lg outline outline-1 outline-offset-[-1px] outline-white/10 text-gray-300 text-xs font-normal font-['Onest'] leading-4 transition-all duration-150 focus:outline-orange-500/40 focus:outline-2 focus:bg-[#222637]"
        />
        <span class="text-gray-600 text-base font-normal font-['Onest'] leading-6 shrink-0">—</span>
        <input
          v-model.number="priceMax"
          type="number"
          :min="priceMin"
          max="100000"
          class="flex-1 w-0 min-w-0 px-2.5 py-1.5 bg-[#1c1f2a] rounded-lg outline outline-1 outline-offset-[-1px] outline-white/10 text-gray-300 text-xs font-normal font-['Onest'] leading-4 transition-all duration-150 focus:outline-orange-500/40 focus:outline-2 focus:bg-[#222637]"
        />
      </div>

      <!-- Price range track -->
      <div class="self-stretch h-1.5 relative bg-[#2a2d3e] rounded-full overflow-hidden">
        <div
          class="h-1.5 absolute top-0 bg-gradient-to-r from-orange-500 to-amber-400 rounded-full transition-all duration-200"
          :style="{ left: sliderFillLeft + '%', width: sliderFillWidth + '%' }"
        />
      </div>

      <!-- Brand heading -->
      <span class="self-stretch pt-2 text-gray-300 text-sm font-semibold font-['Onest'] leading-5">
        Бренд
      </span>

      <!-- Brand checkboxes -->
      <div class="self-stretch pb-2 flex flex-col justify-start items-start gap-1.5">
        <label
          v-for="brand in brands"
          :key="brand.name"
          class="self-stretch inline-flex justify-start items-center gap-2 cursor-pointer group"
        >
          <!-- Custom checkbox -->
          <span
            class="w-3 h-3 shrink-0 rounded-sm border flex items-center justify-center transition-all duration-150"
            :class="
              selectedBrands.has(brand.name)
                ? 'bg-orange-500 border-orange-500'
                : 'bg-white border-[#7c6e7e] group-hover:border-orange-500/60'
            "
            @click="toggleBrand(brand.name)"
          >
            <!-- Checkmark SVG -->
            <svg
              v-if="selectedBrands.has(brand.name)"
              class="w-2 h-2 text-white"
              viewBox="0 0 8 8"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path d="M1 4L3 6L7 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </span>

          <span
            class="text-gray-400 text-xs font-normal font-['Onest'] leading-4 transition-colors duration-150 group-hover:text-gray-200"
            @click="toggleBrand(brand.name)"
          >
            {{ brand.name }}
          </span>

          <span class="flex-1 text-right text-gray-600 text-xs font-normal font-['Onest'] leading-4">
            {{ brand.count }}
          </span>
        </label>
      </div>

      <!-- Apply button -->
      <button
        class="self-stretch py-2 bg-gradient-to-b from-orange-500 to-orange-600 rounded-xl shadow-[0px_4px_16px_0px_rgba(249,115,22,0.35)] text-white text-xs font-semibold font-['Onest'] leading-4 text-center transition-all duration-150 hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.55)] active:scale-[0.98] active:shadow-none"
        @click="applyFilter"
      >
        Застосувати
      </button>
    </div>

  </aside>
</template>

<style scoped>
/* Hide number input spinners */
input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type='number'] {
  -moz-appearance: textfield;
}
</style>
