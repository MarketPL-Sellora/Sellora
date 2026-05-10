<script setup lang="ts">
import { ref, computed } from 'vue'

// ─── Props ────────────────────────────────────────────────────────────────────
const props = defineProps<{
  product: any
  simple?: boolean
}>()

// ─── Адаптер (Нормалізація даних) ─────────────────────────────────────────────
const item = computed(() => {
  const p = props.product
  
  // Визначаємо, чи це Групова покупка
  // (Або з бекенду прийшли groupPrice і groupTargetSize, або це старі mock-дані)
  const isGroupBuy = (p.groupPrice > 0 && p.groupTargetSize > 0) || (p.oldPrice > 0 && p.groupTotal > 0)

  return {
    id:           p.id,
    brand:        p.brand || 'Sellora',
    name:         p.title || p.name || 'Без назви',
    image:        (p.images && p.images.length > 0) ? p.images[0] : (p.image || 'https://via.placeholder.com/400?text=No+Image'),
    imageAlt:     p.imageAlt || p.title || p.name || 'Зображення товару',
    rating:       p.rating || 0,
    reviewCount:  p.reviewCount || 0,
    isGroupBuy:   isGroupBuy,
    // Якщо це групова покупка — показуємо groupPrice, якщо ні — звичайну ціну standardPrice
    price:        isGroupBuy ? (p.groupPrice ?? p.price) : (p.standardPrice ?? p.price ?? 0),
    oldPrice:     isGroupBuy ? (p.standardPrice ?? p.oldPrice ?? 0) : 0,
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
    class="relative w-full lg:max-w-none mx-auto sm:max-w-none bg-[#1c1f2a] rounded-2xl outline outline-1 outline-offset-[-1px] outline-orange-500/30 flex flex-col justify-start items-start cursor-pointer transition-all duration-300 ease-out hover:-translate-y-1.5 hover:shadow-[0px_16px_48px_0px_rgba(0,0,0,0.50),0px_0px_0px_1px_rgba(249,115,22,0.30)] group"
    :class="simple ? 'h-auto' : 'h-full min-h-[24rem]'"
  >
    <div class="pointer-events-none absolute inset-0 rounded-2xl shadow-[0px_0px_0px_1px_rgba(249,115,22,0.15)] shadow-[inset_0px_0px_40px_1px_rgba(249,115,22,0.03)]" />

    <div class="self-stretch h-40 relative bg-gradient-to-br from-[#2a2d3e] via-indigo-900 to-violet-900 rounded-tl-2xl rounded-tr-2xl overflow-hidden">
      <div class="absolute inset-0 flex justify-center items-center">
        <img
          :src="item.image"
          :alt="item.imageAlt"
          class="w-24 h-24 object-contain drop-shadow-[0px_10px_40px_rgba(99,102,241,0.40)] transition-transform duration-500 group-hover:scale-110"
          loading="lazy"
        />
      </div>

      <div v-if="!simple && item.isGroupBuy" class="absolute left-2 top-2 px-2 py-1 bg-gradient-to-b from-orange-500 to-orange-600 rounded-lg shadow-[0px_4px_14px_0px_rgba(249,115,22,0.50)]">
        <span class="text-white text-[9px] font-black font-['Unbounded'] uppercase leading-4 tracking-tight">РАЗОМ ДЕШЕВШЕ</span>
      </div>

      <button
        :aria-label="isWishlisted ? 'Прибрати з обраного' : 'Додати до обраного'"
        class="absolute right-2 top-2 w-7 h-7 bg-black/40 rounded-lg flex justify-center items-center transition-all duration-150 hover:bg-black/60 active:scale-90"
        @click.prevent.stop="toggleWishlist"
      >
        <svg class="w-3.5 h-3.5 transition-colors duration-150" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg" :class="isWishlisted ? 'text-orange-500' : 'text-gray-400 hover:text-orange-400'">
          <path d="M7 12S1.5 8.5 1.5 4.5A2.5 2.5 0 016 3a2.5 2.5 0 015.5 1.5C11.5 8.5 7 12 7 12Z" :fill="isWishlisted ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </div>

    <div class="self-stretch flex-1 p-3 flex flex-col justify-between items-start" :class="simple ? 'pb-4' : ''">
      <div class="self-stretch pb-0.5">
        <span class="text-[#7c6e7e] text-xs font-normal font-['Onest'] leading-4">{{ item.brand }}</span>
      </div>

      <div class="self-stretch h-6 relative overflow-hidden">
        <span class="text-gray-100 text-sm font-semibold font-['Onest'] leading-5 line-clamp-2">{{ item.name }}</span>
      </div>

      <div class="self-stretch pb-2 inline-flex justify-start items-center gap-1">
        <span class="text-amber-400 text-xs font-normal font-['Segoe_UI_Symbol'] leading-4">{{ stars }}</span>
        <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">({{ item.reviewCount }})</span>
      </div>

      <div class="self-stretch flex flex-col justify-end min-h-[52px]" :class="simple ? 'pb-0' : 'pb-3'">
        <span v-if="item.isGroupBuy" class="text-gray-600 text-xs font-normal font-['Onest'] line-through leading-4">{{ formattedOldPrice }}</span>
        <div v-else class="h-4"></div>
        <span class="text-orange-500 text-xl font-bold font-['Unbounded'] leading-7">{{ formattedPrice }}</span>
      </div>

      <button
        v-if="!simple && item.isGroupBuy"
        class="self-stretch py-2 bg-gradient-to-b from-orange-500 to-orange-600 rounded-xl shadow-[0px_4px_16px_0px_rgba(249,115,22,0.35)] text-white text-xs font-semibold font-['Onest'] leading-4 text-center transition-all duration-150 hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_4px_24px_0px_rgba(249,115,22,0.55)] active:scale-[0.98] active:shadow-none"
        @click.prevent.stop="handleOpenGroup"
      >
        🤝 Відкрити збір
      </button>
    </div>
  </router-link>
</template>