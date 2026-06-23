<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { ProductApiItem } from '../../state/productStore'
import { useProductStore } from '../../state/productStore'
import { useUserStore } from '../../state/userStore'
import { toast } from 'vue3-toastify'

const props = defineProps<{
  apiProduct?: ProductApiItem | null
}>()

interface GalleryImage {
  src: string
  alt: string
}

const images = computed<GalleryImage[]>(() => {
  const raw = props.apiProduct?.images
  if (!raw || raw.length === 0) return []
  return raw.map((url, i) => ({
    src: url,
    alt: `Фото товару ${i + 1}`,
  }))
})

const hasImages = computed(() => images.value.length > 0)
const activeImageIndex = ref(0)

const isZoomMode = ref(false)
const isHovering = ref(false)
const zoomX      = ref(0)
const zoomY      = ref(0)

const LENS_SIZE = 180
const ZOOM_FACTOR = 2.5

const mainContainerRef = ref<HTMLDivElement | null>(null)

function onMouseEnter() {
  if (!hasImages.value) return
  isHovering.value = true
}

function onMouseMove(e: MouseEvent) {
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

const lensStyle = computed(() => {
  const half = LENS_SIZE / 2
  return {
    width:  `${LENS_SIZE}px`,
    height: `${LENS_SIZE}px`,
    left:   `calc(${zoomX.value * 100}% - ${half}px)`,
    top:    `calc(${zoomY.value * 100}% - ${half}px)`,
  }
})

const lensImageStyle = computed(() => {
  if (!hasImages.value) return {}
  return {
    backgroundImage:    `url("${images.value[activeImageIndex.value].src}")`,
    backgroundSize:     `${ZOOM_FACTOR * 100}%`,
    backgroundPosition: `${zoomX.value * 100}% ${zoomY.value * 100}%`,
  }
})

const productStore = useProductStore()
const userStore = useUserStore()
const localIsFavorite = ref(props.apiProduct?.isFavorite || false)

watch(() => props.apiProduct?.isFavorite, (newVal) => {
  localIsFavorite.value = newVal || false
})

watch(() => userStore.isAuthenticated, (isAuth) => {
  if (!isAuth) {
    localIsFavorite.value = false
  } else {
    const pending = localStorage.getItem('pendingFavorite')
    if (props.apiProduct && pending === String(props.apiProduct.id)) {
      localIsFavorite.value = true
    }
  }
})

async function toggleWishlist() {
  if (!props.apiProduct) return
  if (!userStore.isAuthenticated) {
    localStorage.setItem('pendingFavorite', String(props.apiProduct.id))
    userStore.isAuthModalOpen = true
    return
  }
  const originalState = localIsFavorite.value
  localIsFavorite.value = !localIsFavorite.value
  try {
    if (localIsFavorite.value) {
      await productStore.addToFavorites(props.apiProduct.id)
    } else {
      await productStore.removeFromFavorites(props.apiProduct.id)
    }
  } catch (error: any) {
    localIsFavorite.value = originalState
    if (error?.isHandled) return
    toast.error('Помилка збереження. Спробуйте пізніше.')
  }
}
</script>

<template>
  <div class="w-full self-stretch flex flex-col gap-2">

    <!-- ── Головний контейнер ──────────────────────────────────────────────── -->
    <div
      ref="mainContainerRef"
      class="w-full aspect-square relative rounded-2xl overflow-hidden group"
      :class="isZoomMode ? 'cursor-crosshair' : 'cursor-default'"
      @mouseenter="onMouseEnter"
      @mousemove="onMouseMove"
      @mouseleave="onMouseLeave"
    >

      <!-- Темний фон -->
      <div class="absolute inset-0 bg-[#0d0f14]" />

      <!-- Розмите фонове зображення (колірний ambient) -->
      <img
        v-if="hasImages"
        :src="images[activeImageIndex].src"
        :alt="''"
        aria-hidden="true"
        class="absolute inset-0 w-full h-full object-cover scale-125 blur-[60px] opacity-25 saturate-[1.4] pointer-events-none select-none"
      />

      <!-- Vignette — затемнення по краях -->
      <div
        class="absolute inset-0 pointer-events-none z-[1]"
        style="background: radial-gradient(ellipse 80% 80% at 50% 50%, transparent 40%, rgba(13,15,20,0.95) 100%);"
      />

      <!-- Порожній стан -->
      <div v-if="!hasImages" class="absolute inset-0 flex flex-col items-center justify-center gap-3 z-10">
        <svg class="w-14 h-14 text-[#1e2030]" viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="8" y="12" width="48" height="40" rx="4" />
          <circle cx="24" cy="28" r="6" />
          <path d="M8 44l12-10 8 6 12-14 16 18" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span class="text-[#2d3148] text-sm font-['Onest']">Фото відсутнє</span>
      </div>

      <!-- Головне зображення — flex центрування без absolute конфлікту -->
      <div v-if="hasImages" class="absolute inset-0 z-10 flex items-center justify-center p-6">
        <img
          :src="images[activeImageIndex].src"
          :alt="images[activeImageIndex].alt"
          class="max-w-full max-h-full object-contain select-none transition-all duration-500 drop-shadow-[0_16px_40px_rgba(0,0,0,0.5)]"
          draggable="false"
          loading="lazy"
        />
      </div>

      <!-- Лупа -->
      <div
        v-if="isZoomMode && isHovering && hasImages"
        class="absolute z-30 rounded-full pointer-events-none overflow-hidden"
        :style="lensStyle"
        style="border: 1px solid rgba(255,255,255,0.15); box-shadow: 0 8px 32px rgba(0,0,0,0.7);"
      >
        <div class="w-full h-full rounded-full bg-no-repeat" :style="lensImageStyle" />
      </div>

      <!-- Кнопка "Обране" -->
      <button
        :aria-label="localIsFavorite ? 'Прибрати з обраного' : 'Додати до обраного'"
        class="absolute right-3 top-3 z-20 w-9 h-9 rounded-xl flex items-center justify-center transition-all duration-200 hover:scale-110 active:scale-95 focus:outline-none"
        style="background: rgba(13,15,20,0.75); border: 1px solid rgba(255,255,255,0.08); backdrop-filter: blur(10px);"
        @click="toggleWishlist"
      >
        <svg
          class="w-4 h-4 transition-all duration-200"
          :class="localIsFavorite ? 'text-orange-500' : 'text-[#4b5563]'"
          viewBox="0 0 16 16" fill="none"
        >
          <path
            d="M8 13.5S2 9.5 2 5.5A3 3 0 018 3.8 3 3 0 0114 5.5C14 9.5 8 13.5 8 13.5Z"
            :fill="localIsFavorite ? 'currentColor' : 'none'"
            stroke="currentColor" stroke-width="1.2"
            stroke-linecap="round" stroke-linejoin="round"
          />
        </svg>
      </button>

      <!-- Кнопка зуму -->
      <button
        v-if="hasImages"
        aria-label="Збільшення"
        class="absolute right-3 bottom-3 z-20 w-9 h-9 rounded-xl flex items-center justify-center transition-all duration-200 active:scale-95 focus:outline-none"
        :class="isZoomMode ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'"
        :style="isZoomMode
          ? 'background: rgba(249,115,22,0.18); border: 1px solid rgba(249,115,22,0.45); color: #f97316; backdrop-filter: blur(10px);'
          : 'background: rgba(13,15,20,0.75); border: 1px solid rgba(255,255,255,0.08); color: #4b5563; backdrop-filter: blur(10px);'"
        @click.prevent.stop="toggleZoomMode"
      >
        <svg class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
          <circle cx="7" cy="7" r="4.5"/>
          <path d="M10.5 10.5L14 14" stroke-linecap="round"/>
          <path d="M5 7h4M7 5v4" stroke-linecap="round"/>
        </svg>
      </button>

      <!-- Стрілка ліво -->
      <button
        v-if="images.length > 1"
        aria-label="Попереднє фото"
        class="absolute left-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-200 hover:scale-110 active:scale-95"
        style="background: rgba(13,15,20,0.75); border: 1px solid rgba(255,255,255,0.08); backdrop-filter: blur(10px);"
        @click.prevent.stop="activeImageIndex = (activeImageIndex - 1 + images.length) % images.length"
      >
        <svg class="w-3.5 h-3.5 text-[#9ca3af]" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M9 11.5L4.5 7 9 2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>

      <!-- Стрілка право -->
      <button
        v-if="images.length > 1"
        aria-label="Наступне фото"
        class="absolute right-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-200 hover:scale-110 active:scale-95"
        style="background: rgba(13,15,20,0.75); border: 1px solid rgba(255,255,255,0.08); backdrop-filter: blur(10px);"
        @click.prevent.stop="activeImageIndex = (activeImageIndex + 1) % images.length"
      >
        <svg class="w-3.5 h-3.5 text-[#9ca3af]" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M5 2.5L9.5 7 5 11.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </div>

    <!-- ── Мініатюри ───────────────────────────────────────────────────────── -->
    <div v-if="images.length > 1" class="w-full flex gap-2">
      <button
        v-for="(img, i) in images"
        :key="img.src"
        :aria-label="`Фото ${i + 1}`"
        class="flex-1 aspect-square rounded-xl overflow-hidden relative transition-all duration-200 active:scale-95 focus:outline-none"
        :style="activeImageIndex === i
          ? 'box-shadow: 0 0 0 2px #f97316;'
          : 'box-shadow: 0 0 0 1px rgba(255,255,255,0.07);'"
        @click="activeImageIndex = i"
      >
        <!-- Темний фон під мініатюру -->
        <div class="absolute inset-0 bg-[#0d0f14]" />

        <img
          :src="img.src"
          :alt="img.alt"
          class="absolute inset-0 w-full h-full object-contain p-1 transition-all duration-200"
          :class="activeImageIndex === i ? 'opacity-100' : 'opacity-45 hover:opacity-75'"
          loading="lazy"
        />

        <!-- Активний dot -->
        <div
          v-if="activeImageIndex === i"
          class="absolute bottom-1.5 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-orange-500 z-10"
        />
      </button>
    </div>

  </div>
</template>