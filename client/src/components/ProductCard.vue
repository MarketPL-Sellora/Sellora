<script setup lang="ts">
import { ref, computed } from 'vue'

// ─── Props ────────────────────────────────────────────────────────────────────
const props = defineProps<{
  product: any
  simple?: boolean
}>()

// ─── Адаптер (Нормалізація даних) ─────────────────────────────────────────────
const item = computed(() => {
  const p = props.product || {}

  // 1. БЕЗПЕЧНИЙ ПАРСИНГ ЦІН (перетворюємо null/undefined на 0)
  const standardPrice = Number(p.standardPrice) || 0
  const groupPrice    = Number(p.groupPrice) || 0
  const fallbackPrice = Number(p.price) || 0       // для старих тестових даних
  const fallbackOld   = Number(p.oldPrice) || 0    // для старих тестових даних

  const targetSize    = Number(p.groupTargetSize) || 0
  const totalFallback = Number(p.groupTotal) || 0

  // 2. Визначаємо, чи це Групова покупка
  const isGroupBuy = (groupPrice > 0 && targetSize > 0) || (fallbackOld > 0 && totalFallback > 0)

  // 3. Залізобетонна логіка вибору ціни
  const finalPrice = isGroupBuy
    ? (groupPrice || fallbackPrice)
    : (standardPrice || fallbackPrice || groupPrice)

  return {
    id:           p.id,
    // Шукаємо бренд в attributes (кирилицею або латиницею), потім у звичайному полі
    brand:        p.attributes?.['Бренд'] || p.attributes?.['brand'] || p.brand || '',
    name:         p.title || p.name || 'Без назви',
    image:        (p.images && p.images.length > 0) ? p.images[0] : (p.image || 'https://via.placeholder.com/400?text=No+Image'),
    imageAlt:     p.imageAlt || p.title || p.name || 'Зображення товару',
    rating:       p.rating || 0,
    reviewCount:  p.reviewCount || 0,
    isGroupBuy:   isGroupBuy,
    price:        finalPrice,
    oldPrice:     isGroupBuy ? (standardPrice || fallbackOld) : 0,
    groupCurrent: p.groupCurrentSize ?? p.groupCurrent ?? 0,
    groupTotal:   targetSize || totalFallback || 3,
    groupLabel:   p.groupLabel || 'Учасників',
  }
})

// ─── Computed ─────────────────────────────────────────────────────────────────

const stars = computed(() =>
  '★'.repeat(item.value.rating) + '☆'.repeat(5 - item.value.rating)
)

const formattedPrice = computed(() =>
  item.value.price.toLocaleString('uk-UA') + ' ₴'
)

const formattedOldPrice = computed(() =>
  item.value.oldPrice.toLocaleString('uk-UA') + ' ₴'
)

// ─── State ────────────────────────────────────────────────────────────────────

const isWishlisted = ref(false)

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'open-group', product: any): void
  (e: 'wishlist',   product: any): void
}>()

function handleOpenGroup(e: MouseEvent) {
  e.preventDefault()
  e.stopPropagation()
  emit('open-group', props.product)
}

function toggleWishlist(e: MouseEvent) {
  e.preventDefault()
  e.stopPropagation()
  isWishlisted.value = !isWishlisted.value
  emit('wishlist', props.product)
}
</script>

<template>
  <router-link
    :to="'/product/' + item.id"
    class="relative w-full lg:max-w-none mx-auto sm:max-w-none bg-[#1c1f2a] rounded-2xl outline outline-1 outline-offset-[-1px] outline-orange-500/30 flex flex-col justify-start items-start cursor-pointer transition-all duration-300 ease-out hover:-translate-y-1.5 hover:shadow-[0px_16px_48px_0px_rgba(0,0,0,0.50),0px_0px_0px_1px_rgba(249,115,22,0.30)] group overflow-hidden"
    :class="simple ? 'h-auto' : 'h-full min-h-[22rem]'"
  >
    <div class="pointer-events-none absolute inset-0 rounded-2xl shadow-[0px_0px_0px_1px_rgba(249,115,22,0.15)] shadow-[inset_0px_0px_40px_1px_rgba(249,115,22,0.03)] z-10" />

    <div class="self-stretch h-52 relative bg-[#2a2d3e] flex-shrink-0 overflow-hidden">
      <img
        :src="item.image"
        :alt="item.imageAlt"
        class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
        loading="lazy"
      />

      <div v-if="!simple && item.isGroupBuy" class="absolute left-3 top-3 px-2.5 py-1.5 bg-gradient-to-b from-orange-500 to-orange-600 rounded-lg shadow-[0px_4px_14px_0px_rgba(249,115,22,0.50)] z-20">
        <span class="text-white text-[9px] font-black font-['Unbounded'] uppercase leading-4 tracking-tight">РАЗОМ ДЕШЕВШЕ</span>
      </div>

      <button
        :aria-label="isWishlisted ? 'Прибрати з обраного' : 'Додати до обраного'"
        class="absolute right-3 top-3 w-8 h-8 bg-black/40 backdrop-blur-md rounded-lg flex justify-center items-center transition-all duration-150 hover:bg-black/60 active:scale-90 z-20"
        @click.prevent.stop="toggleWishlist"
      >
        <svg class="w-4 h-4 transition-colors duration-150" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg" :class="isWishlisted ? 'text-orange-500' : 'text-gray-300 hover:text-orange-400'">
          <path d="M7 12S1.5 8.5 1.5 4.5A2.5 2.5 0 016 3a2.5 2.5 0 015.5 1.5C11.5 8.5 7 12 7 12Z" :fill="isWishlisted ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </div>

    <div class="self-stretch flex-1 p-4 flex flex-col items-start relative z-20 w-full">

      <span v-if="item.brand" class="text-[#7c6e7e] text-xs font-normal font-['Onest'] leading-4 mb-1.5">{{ item.brand }}</span>

      <span class="text-gray-100 text-sm font-semibold font-['Onest'] leading-5 line-clamp-2 mb-2 w-full">{{ item.name }}</span>

      <div class="inline-flex justify-start items-center gap-1.5">
        <span class="text-amber-400 text-xs font-normal font-['Segoe_UI_Symbol'] leading-4">{{ stars }}</span>
        <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">({{ item.reviewCount }})</span>
      </div>

      <div class="self-stretch flex flex-col mt-auto pt-5 w-full">

        <div class="flex flex-col min-h-[44px] justify-end mb-3">
          <span v-if="item.isGroupBuy" class="text-gray-500 text-xs font-normal font-['Onest'] line-through leading-4 mb-0.5">{{ formattedOldPrice }}</span>
          <span class="text-orange-500 text-xl font-bold font-['Unbounded'] leading-7">{{ formattedPrice }}</span>
        </div>

        <button
          v-if="!simple && item.isGroupBuy"
          class="self-stretch py-2.5 bg-gradient-to-b from-orange-500 to-orange-600 rounded-xl shadow-[0px_4px_16px_0px_rgba(249,115,22,0.35)] text-white text-sm font-semibold font-['Onest'] leading-5 text-center transition-all duration-150 hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_4px_24px_0px_rgba(249,115,22,0.55)] active:scale-[0.98] active:shadow-none"
          @click.prevent.stop="handleOpenGroup"
        >
          🤝 Відкрити збір
        </button>
      </div>

    </div>
  </router-link>
</template>
