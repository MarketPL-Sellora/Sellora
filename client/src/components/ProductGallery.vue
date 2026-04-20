<script setup lang="ts">
import { ref } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface GalleryImage {
  src: string
  alt: string
}

// ─── Images (replace with real paths) ────────────────────────────────────────

const images = ref<GalleryImage[]>([
  { src: '../assets/s24_1.png', alt: 'Samsung Galaxy S24 Ultra — вигляд спереду'  },
  { src: '../assets/s24_2.png', alt: 'Samsung Galaxy S24 Ultra — вигляд ззаду'   },
  { src: '../assets/s24_3.png', alt: 'Samsung Galaxy S24 Ultra — S Pen'           },
  { src: '../assets/s24_4.png', alt: 'Samsung Galaxy S24 Ultra — камера крупно'  },
  { src: '../assets/s24_5.png', alt: 'Samsung Galaxy S24 Ultra — колір Titanium' },
])

// ─── State ────────────────────────────────────────────────────────────────────

const activeImageIndex = ref(0)
const isWishlisted     = ref(false)
const isZoomed         = ref(false)

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'wishlist'): void
  (e: 'zoom', src: string): void
}>()

function toggleWishlist() {
  isWishlisted.value = !isWishlisted.value
  emit('wishlist')
}

function openZoom() {
  isZoomed.value = true
  emit('zoom', images.value[activeImageIndex.value].src)
}
</script>

<template>
  <div class="w-full self-stretch inline-flex flex-col justify-start items-start gap-3">

    <!-- ── Main image ─────────────────────────────────────────────────────── -->
    <div
      class="self-stretch aspect-square relative bg-[#1c1f2a] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] flex justify-center items-center overflow-hidden group"
    >
      <!-- Background radial gradient -->
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_98.99%_98.99%_at_30%_30%,#1A1040_0%,#0D1022_40%,#0A0B0F_100%)]" />

      <!-- Product image -->
      <img
        :src="images[activeImageIndex].src"
        :alt="images[activeImageIndex].alt"
        class="relative z-10 w-48 h-96 object-contain drop-shadow-2xl transition-transform duration-500 group-hover:scale-105"
        loading="lazy"
      />

      <!-- NEW badge -->
      <div class="absolute left-[17px] top-[17px] z-20 px-2.5 py-1 bg-orange-500 rounded-lg">
        <span class="text-white text-xs font-normal font-['Unbounded'] leading-4">NEW</span>
      </div>

      <!-- Discount badge -->
      <div class="absolute left-[17px] top-[49px] z-20 px-2.5 py-1 bg-red-600 rounded-lg">
        <span class="text-white text-xs font-normal font-['Unbounded'] leading-4">−29%</span>
      </div>

      <!-- Wishlist button -->
      <button
        :aria-label="isWishlisted ? 'Прибрати з обраного' : 'Додати до обраного'"
        class="absolute right-[17px] top-[17px] z-20 w-9 h-9 bg-[#1c1f2a]/80 rounded-xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] backdrop-blur-sm flex justify-center items-center transition-all duration-150 hover:bg-[#252b3d] hover:outline-[#3d4158] hover:scale-110 active:scale-95 focus:outline-none focus:ring-2 focus:ring-orange-500/50"
        @click="toggleWishlist"
      >
        <svg
          class="w-4 h-4 transition-colors duration-150"
          :class="isWishlisted ? 'text-orange-500' : 'text-[#787d99] hover:text-orange-400'"
          viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg"
        >
          <path
            d="M8 13.5S2 9.5 2 5.5A3 3 0 018 3.8 3 3 0 0114 5.5C14 9.5 8 13.5 8 13.5Z"
            :fill="isWishlisted ? 'currentColor' : 'none'"
            stroke="currentColor"
            stroke-width="1.2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>

      <!-- Zoom button (bottom-right, appears on hover) -->
      <button
        aria-label="Збільшити фото"
        class="absolute right-[17px] bottom-[17px] z-20 w-9 h-9 bg-[#1c1f2a]/80 rounded-xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] backdrop-blur-sm flex justify-center items-center opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-[#252b3d] active:scale-95 focus:outline-none focus:ring-2 focus:ring-orange-500/50"
        @click="openZoom"
      >
        <svg class="w-4 h-4 text-[#787d99]" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3" xmlns="http://www.w3.org/2000/svg">
          <circle cx="7" cy="7" r="4.5"/>
          <path d="M10.5 10.5L14 14" stroke-linecap="round"/>
          <path d="M5 7h4M7 5v4" stroke-linecap="round"/>
        </svg>
      </button>

      <!-- Prev / Next arrows (appear on hover) -->
      <button
        v-if="images.length > 1"
        aria-label="Попереднє фото"
        class="absolute left-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-[#1c1f2a]/80 rounded-full outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] backdrop-blur-sm flex justify-center items-center opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-[#252b3d] active:scale-95"
        @click="activeImageIndex = (activeImageIndex - 1 + images.length) % images.length"
      >
        <svg class="w-3.5 h-3.5 text-[#787d99]" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M9 11.5L4.5 7 9 2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <button
        v-if="images.length > 1"
        aria-label="Наступне фото"
        class="absolute right-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-[#1c1f2a]/80 rounded-full outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] backdrop-blur-sm flex justify-center items-center opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-[#252b3d] active:scale-95"
        @click="activeImageIndex = (activeImageIndex + 1) % images.length"
      >
        <svg class="w-3.5 h-3.5 text-[#787d99]" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M5 2.5L9.5 7 5 11.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </div>

    <!-- ── Thumbnails ─────────────────────────────────────────────────────── -->
    <div class="self-stretch inline-flex justify-start items-start gap-2">
      <button
        v-for="(img, i) in images"
        :key="img.src"
        :aria-label="`Фото ${i + 1}: ${img.alt}`"
        class="flex-1 aspect-square min-w-0 bg-[#1c1f2a] rounded-xl overflow-hidden flex justify-center items-center transition-all duration-150 hover:scale-[1.04] active:scale-95 focus:outline-none"
        :class="
          activeImageIndex === i
            ? 'outline outline-2 outline-offset-[-2px] outline-orange-500'
            : 'outline outline-2 outline-offset-[-2px] outline-[#2a2d3e] hover:outline-[#3d4158]'
        "
        @click="activeImageIndex = i"
      >
        <img
          :src="img.src"
          :alt="img.alt"
          class="w-full h-full object-contain p-1 transition-opacity duration-150"
          :class="activeImageIndex === i ? 'opacity-100' : 'opacity-50 hover:opacity-80'"
          loading="lazy"
        />
      </button>
    </div>

  </div>
</template>
