<script setup lang="ts">
import { ref, computed } from 'vue'

// ─── Props ────────────────────────────────────────────────────────────────────
// Додано проп `simple` для різних режимів відображення картки
defineProps({
  simple: {
    type: Boolean,
    default: false
  }
})

// ─── Types ────────────────────────────────────────────────────────────────────

interface Product {
  brand: string
  name: string
  image: string
  imageAlt: string
  rating: number
  reviewCount: number
  groupLabel: string
  groupCurrent: number
  groupTotal: number
  price: number
  oldPrice: number
}

// ─── Mock data (replace with props later) ─────────────────────────────────────

const product = ref<Product>({
  brand: 'Samsung',
  name: 'Samsung Galaxy S24 Ultra 256GB',
  image: '../assets/product-placeholder.png',
  imageAlt: 'Samsung Galaxy S24 Ultra',
  rating: 5,
  reviewCount: 284,
  groupLabel: 'Учасників',
  groupCurrent: 14,
  groupTotal: 20,
  price: 25000,
  oldPrice: 30000,
})

// ─── Computed ─────────────────────────────────────────────────────────────────

const groupPercent = computed(() =>
  Math.round((product.value.groupCurrent / product.value.groupTotal) * 100)
)

const stars = computed(() =>
  '★'.repeat(product.value.rating) + '☆'.repeat(5 - product.value.rating)
)

const formattedPrice = computed(() =>
  product.value.price.toLocaleString('uk-UA') + ' ₴'
)

const formattedOldPrice = computed(() =>
  product.value.oldPrice.toLocaleString('uk-UA') + ' ₴'
)

// ─── State ────────────────────────────────────────────────────────────────────

const isWishlisted = ref(false)

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'open-group', product: Product): void
  (e: 'wishlist', product: Product): void
}>()

function handleOpenGroup() {
  emit('open-group', product.value)
}

function toggleWishlist() {
  isWishlisted.value = !isWishlisted.value
  emit('wishlist', product.value)
}
</script>

<template>
  <article
    class="w-72 relative bg-[#1c1f2a] rounded-2xl outline outline-1 outline-offset-[-1px] outline-orange-500/30 inline-flex flex-col justify-start items-start cursor-pointer
           transition-all duration-300 ease-out
           hover:-translate-y-1.5 hover:shadow-[0px_16px_48px_0px_rgba(0,0,0,0.50),0px_0px_0px_1px_rgba(249,115,22,0.30)]
           group"
    :class="simple ? 'h-auto' : 'h-96'"
  >
    <div
      class="pointer-events-none absolute inset-0 rounded-2xl shadow-[0px_0px_0px_1px_rgba(249,115,22,0.15)] shadow-[inset_0px_0px_40px_1px_rgba(249,115,22,0.03)]"
    />

    <div
      class="self-stretch h-40 relative bg-gradient-to-br from-[#2a2d3e] via-indigo-900 to-violet-900 rounded-tl-2xl rounded-tr-2xl overflow-hidden"
    >
      <div class="absolute inset-0 flex justify-center items-center">
        <img
          :src="product.image"
          :alt="product.imageAlt"
          class="w-24 h-24 object-contain drop-shadow-[0px_10px_40px_rgba(99,102,241,0.40)] transition-transform duration-500 group-hover:scale-110"
          loading="lazy"
        />
      </div>

      <div
        v-if="!simple"
        class="absolute left-2 top-2 px-2 py-1 bg-gradient-to-b from-orange-500 to-orange-600 rounded-lg shadow-[0px_4px_14px_0px_rgba(249,115,22,0.50)]"
      >
        <span class="text-white text-[9px] font-black font-['Unbounded'] uppercase leading-4 tracking-tight">
          РАЗОМ ДЕШЕВШЕ
        </span>
      </div>

      <button
        :aria-label="isWishlisted ? 'Прибрати з обраного' : 'Додати до обраного'"
        class="absolute right-2 top-2 w-7 h-7 bg-black/40 rounded-lg flex justify-center items-center transition-all duration-150 hover:bg-black/60 active:scale-90"
        @click.stop="toggleWishlist"
      >
        <svg class="w-3.5 h-3.5 transition-colors duration-150" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg"
             :class="isWishlisted ? 'text-orange-500' : 'text-gray-400 hover:text-orange-400'"
        >
          <path
            d="M7 12S1.5 8.5 1.5 4.5A2.5 2.5 0 016 3a2.5 2.5 0 015.5 1.5C11.5 8.5 7 12 7 12Z"
            :fill="isWishlisted ? 'currentColor' : 'none'"
            stroke="currentColor"
            stroke-width="1.2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
    </div>

    <div class="self-stretch flex-1 p-3 flex flex-col justify-between items-start" :class="simple ? 'pb-4' : ''">

      <div class="self-stretch pb-0.5">
        <span class="text-[#7c6e7e] text-xs font-normal font-['Onest'] leading-4">
          {{ product.brand }}
        </span>
      </div>

      <div class="self-stretch h-6 relative overflow-hidden">
        <span class="text-gray-100 text-sm font-semibold font-['Onest'] leading-5 line-clamp-1">
          {{ product.name }}
        </span>
      </div>

      <div class="self-stretch pb-2 inline-flex justify-start items-center gap-1">
        <span class="text-amber-400 text-xs font-normal font-['Segoe_UI_Symbol'] leading-4">
          {{ stars }}
        </span>
        <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">
          ({{ product.reviewCount }})
        </span>
      </div>

      <div v-if="!simple" class="self-stretch pb-2 flex flex-col gap-1">
        <div class="self-stretch inline-flex justify-between items-center">
          <span class="text-[#7c6e7e] text-xs font-normal font-['Onest'] leading-4">
            {{ product.groupLabel }}:&nbsp;
            <span class="text-orange-500 font-semibold">
              {{ product.groupCurrent }} / {{ product.groupTotal }}
            </span>
          </span>
          <span class="text-orange-500 text-xs font-normal font-['Onest'] leading-4">
            {{ groupPercent }}%
          </span>
        </div>

        <div class="self-stretch h-1.5 relative bg-[#2a2d3e] rounded-full overflow-hidden">
          <div
            class="h-1.5 absolute left-0 top-0 bg-gradient-to-r from-orange-500 to-amber-400 rounded-full transition-all duration-500"
            :style="{ width: groupPercent + '%' }"
          />
        </div>
      </div>

      <div class="self-stretch pb-3 flex flex-col" :class="simple ? 'pb-0' : ''">
        <span class="text-gray-600 text-xs font-normal font-['Onest'] line-through leading-4">
          {{ formattedOldPrice }}
        </span>
        <span class="text-orange-500 text-xl font-bold font-['Unbounded'] leading-7">
          {{ formattedPrice }}
        </span>
      </div>

      <button
        v-if="!simple"
        class="self-stretch py-2 bg-gradient-to-b from-orange-500 to-orange-600 rounded-xl shadow-[0px_4px_16px_0px_rgba(249,115,22,0.35)] text-white text-xs font-semibold font-['Onest'] leading-4 text-center
               transition-all duration-150
               hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_4px_24px_0px_rgba(249,115,22,0.55)]
               active:scale-[0.98] active:shadow-none"
        @click.stop="handleOpenGroup"
      >
        🤝 Відкрити збір
      </button>
    </div>
  </article>
</template>
