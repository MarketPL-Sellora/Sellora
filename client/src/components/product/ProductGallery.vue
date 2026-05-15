<script setup lang="ts">
import { ref, computed } from 'vue'

// ─── Props ───────────────────────────────────────────────────────────────────

const props = defineProps<{
  productImages?: string[] | null
}>()

// ─── Computed images ─────────────────────────────────────────────────────────

interface GalleryImage {
  src: string
  alt: string
}

const images = computed<GalleryImage[]>(() => {
  const raw = props.productImages
  if (!raw || raw.length === 0) return []
  return raw.map((url, i) => ({
    src: url,
    alt: `Фото товару ${i + 1}`,
  }))
})

const hasImages = computed(() => images.value.length > 0)

// ─── State ───────────────────────────────────────────────────────────────────

const activeImageIndex = ref(0)
const isWishlisted     = ref(false)

// ─── Zoom / Loupe state ──────────────────────────────────────────────────────

const isZoomMode = ref(false) // Чи увімкнена кнопка лупи
const isHovering = ref(false) // Чи знаходиться мишка над фотографією
const zoomX      = ref(0)     // позиція курсору 0-1 відносно контейнера
const zoomY      = ref(0)

const LENS_SIZE = 180      // розмір лупи (діаметр у пікселях)
const ZOOM_FACTOR = 2.5    // наскільки сильно збільшувати

const mainContainerRef = ref<HTMLDivElement | null>(null)

function onMouseEnter() {
  if (!hasImages.value) return
  isHovering.value = true
}

function onMouseMove(e: MouseEvent) {
  // Обчислюємо координати тільки якщо увімкнено режим лупи
  if (!mainContainerRef.value || !hasImages.value || !isZoomMode.value) return
  const rect = mainContainerRef.value.getBoundingClientRect()
  zoomX.value = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  zoomY.value = Math.max(0, Math.min(1, (e.clientY - rect.top) / rect.height))
}

function onMouseLeave() {
  isHovering.value = false
}

function toggleZoomMode() {
  isZoomMode.value = !isZoomMode.value
}

// Стилі для позиціонування самої лупи
const lensStyle = computed(() => {
  const half = LENS_SIZE / 2
  return {
    width:  `${LENS_SIZE}px`,
    height: `${LENS_SIZE}px`,
    left:   `calc(${zoomX.value * 100}% - ${half}px)`,
    top:    `calc(${zoomY.value * 100}% - ${half}px)`,
  }
})

// Стилі для фону всередині лупи (збільшене фото)
const lensImageStyle = computed(() => {
  if (!hasImages.value) return {}
  return {
    backgroundImage:    `url("${images.value[activeImageIndex.value].src}")`,
    backgroundSize:     `${ZOOM_FACTOR * 100}%`,
    backgroundPosition: `${zoomX.value * 100}% ${zoomY.value * 100}%`,
  }
})

// ─── Emits ───────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'wishlist'): void
}>()

function toggleWishlist() {
  isWishlisted.value = !isWishlisted.value
  emit('wishlist')
}
</script>

<template>
  <div class="w-full self-stretch inline-flex flex-col justify-start items-start gap-3">

    <div
      ref="mainContainerRef"
      class="self-stretch aspect-square relative bg-[#0f1117] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] flex justify-center items-center overflow-hidden group transition-colors duration-300"
      :class="isZoomMode ? 'cursor-crosshair' : 'cursor-default'"
      @mouseenter="onMouseEnter"
      @mousemove="onMouseMove"
      @mouseleave="onMouseLeave"
    >

      <img
        v-if="hasImages"
        :src="images[activeImageIndex].src"
        :alt="''"
        aria-hidden="true"
        class="absolute inset-0 w-full h-full object-cover scale-110 blur-[40px] brightness-[0.45] saturate-[1.3] pointer-events-none select-none"
      />

      <div
        v-if="hasImages"
        class="absolute inset-0 bg-[radial-gradient(ellipse_90%_90%_at_50%_50%,transparent_30%,rgba(15,17,23,0.85)_100%)] pointer-events-none"
      />

      <div v-if="!hasImages" class="absolute inset-0 bg-[radial-gradient(ellipse_98.99%_98.99%_at_30%_30%,#1A1040_0%,#0D1022_40%,#0A0B0F_100%)]" />

      <img
        v-if="hasImages"
        :src="images[activeImageIndex].src"
        :alt="images[activeImageIndex].alt"
        class="relative z-10 max-w-[90%] max-h-[90%] object-contain drop-shadow-[0_8px_30px_rgba(0,0,0,0.5)] transition-transform duration-500 select-none"
        draggable="false"
        loading="lazy"
      />

      <div v-if="!hasImages" class="relative z-10 flex flex-col items-center gap-3">
        <svg class="w-16 h-16 text-[#2a2d3e]" viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="8" y="12" width="48" height="40" rx="4" />
          <circle cx="24" cy="28" r="6" />
          <path d="M8 44l12-10 8 6 12-14 16 18" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span class="text-[#3d4158] text-sm font-normal font-['Onest']">Фото відсутнє</span>
      </div>

      <div
        v-if="isZoomMode && isHovering && hasImages"
        class="absolute z-30 rounded-full pointer-events-none border-2 border-white/20 shadow-[0_0_20px_rgba(0,0,0,0.6),0_0_60px_rgba(0,0,0,0.3)]"
        :style="lensStyle"
      >
        <div
          class="w-full h-full rounded-full bg-no-repeat"
          :style="lensImageStyle"
        />
      </div>

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

      <button
        v-if="hasImages"
        aria-label="Увімкнути/Вимкнути збільшення"
        class="absolute right-[17px] bottom-[17px] z-20 w-9 h-9 rounded-xl outline outline-1 outline-offset-[-1px] backdrop-blur-sm flex justify-center items-center transition-all duration-200 active:scale-95 focus:outline-none focus:ring-2 focus:ring-orange-500/50"
        :class="isZoomMode 
          ? 'opacity-100 bg-[#252b3d] outline-orange-500 text-orange-500' 
          : 'opacity-0 group-hover:opacity-100 bg-[#1c1f2a]/80 outline-[#2a2d3e] text-[#787d99] hover:bg-[#252b3d] hover:text-orange-400'"
        @click.prevent.stop="toggleZoomMode"
      >
        <svg class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3" xmlns="http://www.w3.org/2000/svg">
          <circle cx="7" cy="7" r="4.5"/>
          <path d="M10.5 10.5L14 14" stroke-linecap="round"/>
          <path d="M5 7h4M7 5v4" stroke-linecap="round"/>
        </svg>
      </button>

      <button
        v-if="images.length > 1"
        aria-label="Попереднє фото"
        class="absolute left-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-[#1c1f2a]/80 rounded-full outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] backdrop-blur-sm flex justify-center items-center opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-[#252b3d] active:scale-95"
        @click.prevent.stop="activeImageIndex = (activeImageIndex - 1 + images.length) % images.length"
      >
        <svg class="w-3.5 h-3.5 text-[#787d99]" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M9 11.5L4.5 7 9 2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <button
        v-if="images.length > 1"
        aria-label="Наступне фото"
        class="absolute right-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-[#1c1f2a]/80 rounded-full outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] backdrop-blur-sm flex justify-center items-center opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-[#252b3d] active:scale-95"
        @click.prevent.stop="activeImageIndex = (activeImageIndex + 1) % images.length"
      >
        <svg class="w-3.5 h-3.5 text-[#787d99]" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M5 2.5L9.5 7 5 11.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </div>

    <div v-if="images.length > 1" class="self-stretch inline-flex justify-start items-start gap-2">
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