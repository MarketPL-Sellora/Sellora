<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface Slide {
  badge: string
  titleWhite: string
  titleAccent: string
  description: string
  price: string
  oldPrice: string
  discount: string
  image: string
  imageAlt: string
}

interface Stat {
  value: string
  label: string
  color: string
}

// ─── Slides data ──────────────────────────────────────────────────────────────

const slides: Slide[] = [
  {
    badge: '🔥 Гаряча пропозиція',
    titleWhite: 'iPhone 15 Pro',
    titleAccent: 'Max 256GB',
    description: 'Titanium. So strong. So light. So Pro.\nКамера 48Мп · A17 Pro · USB-C',
    price: '45 999 ₴',
    oldPrice: '52 000 ₴',
    discount: '-12%',
    image: '../assets/iphone.png',
    imageAlt: 'iPhone 15 Pro',
  },
  {
    badge: '⚡ Новинка',
    titleWhite: 'Samsung Galaxy',
    titleAccent: 'S24 Ultra',
    description: 'AI-можливості нового рівня.\nS Pen · 200Мп · Snapdragon 8 Gen 3',
    price: '42 499 ₴',
    oldPrice: '49 000 ₴',
    discount: '-13%',
    image: '../assets/samsung.png',
    imageAlt: 'Samsung Galaxy S24 Ultra',
  },
  {
    badge: '🎯 Топ продажів',
    titleWhite: 'MacBook Pro',
    titleAccent: 'M3 Pro 14"',
    description: 'Неймовірна продуктивність.\nChip M3 Pro · 18GB RAM · 512GB SSD',
    price: '89 999 ₴',
    oldPrice: '102 000 ₴',
    discount: '-12%',
    image: '../assets/macbook.png',
    imageAlt: 'MacBook Pro M3',
  },
]

// ─── Stats data ────────────────────────────────────────────────────────────────

const stats: Stat[] = [
  { value: '2.4M+',  label: 'Товарів',     color: 'text-orange-500' },
  { value: '320K',   label: 'Продавців',   color: 'text-white'      },
  { value: '98%',    label: 'Позитивних',  color: 'text-green-400'  },
  { value: '1-3 дні',label: 'Доставка',   color: 'text-blue-400'   },
]

// ─── Carousel logic ───────────────────────────────────────────────────────────

const current = ref(0)
const isAnimating = ref(false)

function goTo(index: number) {
  if (isAnimating.value || index === current.value) return
  isAnimating.value = true
  current.value = (index + slides.length) % slides.length
  setTimeout(() => (isAnimating.value = false), 400)
}

function prev() { goTo(current.value - 1) }
function next() { goTo(current.value + 1) }

// Auto-play
let timer: ReturnType<typeof setInterval>

onMounted(() => {
  timer = setInterval(next, 5000)
})

onUnmounted(() => {
  clearInterval(timer)
})

function pauseAutoPlay() { clearInterval(timer) }
function resumeAutoPlay() {
  clearInterval(timer)
  timer = setInterval(next, 5000)
}

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'buy', slide: Slide): void
  (e: 'details', slide: Slide): void
}>()
</script>

<template>
  <section class="self-stretch flex flex-col gap-2">

    <!-- ── Hero banner ─────────────────────────────────────────────────────── -->
    <div
      class="self-stretch h-72 relative rounded-2xl overflow-hidden"
      @mouseenter="pauseAutoPlay"
      @mouseleave="resumeAutoPlay"
    >
      <!-- Background -->
      <div class="absolute inset-0 bg-gradient-to-b from-[#0f2027] via-[#1c1f2a] to-[#16213e]" />

      <!-- Glow orbs -->
      <div class="pointer-events-none absolute right-0 top-0 w-80 h-80 -translate-y-16 translate-x-8 rounded-full bg-[radial-gradient(ellipse_70.71%_70.71%_at_50%_50%,rgba(249,115,22,0.12)_0%,rgba(249,115,22,0)_70%)]" />
      <div class="pointer-events-none absolute right-0 bottom-0 w-64 h-64 translate-y-16 rounded-full bg-[radial-gradient(ellipse_70.71%_70.71%_at_50%_50%,rgba(99,102,241,0.08)_0%,rgba(99,102,241,0)_70%)]" />

      <!-- Slides -->
      <transition-group name="slide-fade" tag="div" class="absolute inset-0">
        <div
          v-for="(slide, i) in slides"
          v-show="i === current"
          :key="slide.titleAccent"
          class="absolute inset-0 flex items-center overflow-hidden"
        >
          <!-- Phone / product mockup -->
          <div class="absolute right-8 top-1/2 -translate-y-1/2 w-40 h-64 shrink-0">
            <!-- Decorative card behind image -->
            <div class="absolute inset-0 bg-gradient-to-br from-slate-800 to-[#0f2027] rounded-3xl shadow-[0px_30px_80px_0px_rgba(0,0,0,0.60)] shadow-[inset_0px_1px_0px_1px_rgba(255,255,255,0.08)] outline outline-1 outline-offset-[-1px] outline-white/10">
              <div class="w-28 h-1.5 absolute left-[21px] top-[11px] bg-white/10 rounded" />
              <div class="w-32 h-24 absolute left-[16px] top-[25px] opacity-80 bg-gradient-to-br from-orange-500 to-purple-500 rounded-xl" />
              <div class="w-32 h-2 absolute left-[16px] top-[133px] bg-white/5 rounded" />
              <div class="w-28 h-2 absolute left-[16px] top-[145px] bg-white/5 rounded" />
            </div>
            <!-- Actual product image — swap src per-slide -->
            <img
              :src="slide.image"
              :alt="slide.imageAlt"
              class="absolute inset-0 w-full h-full object-contain drop-shadow-2xl z-10"
              loading="lazy"
            />
          </div>

          <!-- Text content -->
          <div class="relative z-10 max-w-[512px] px-12 flex flex-col justify-start items-start gap-2">
            <!-- Badge -->
            <span class="px-2.5 py-1 bg-orange-500/10 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/20 text-orange-400 text-[10px] font-semibold font-['Onest'] uppercase leading-4 tracking-wide">
              {{ slide.badge }}
            </span>

            <!-- Title -->
            <div class="pt-1">
              <span class="text-white text-4xl font-black font-['Unbounded'] leading-10">{{ slide.titleWhite }}<br /></span>
              <span class="text-orange-500 text-4xl font-black font-['Unbounded'] leading-10">{{ slide.titleAccent }}</span>
            </div>

            <!-- Description -->
            <p class="text-gray-400 text-sm font-normal font-['Onest'] leading-6 whitespace-pre-line">
              {{ slide.description }}
            </p>

            <!-- Prices -->
            <div class="self-stretch h-11 relative">
              <span class="absolute left-0 bottom-0 text-orange-500 text-3xl font-bold font-['Onest'] leading-9">
                {{ slide.price }}
              </span>
              <span class="absolute left-[140px] bottom-[4px] text-gray-600 text-lg font-normal font-['Onest'] line-through leading-7">
                {{ slide.oldPrice }}
              </span>
              <span class="absolute left-[232px] bottom-[4px] px-2 py-0.5 bg-green-500/20 rounded-lg text-green-400 text-sm font-semibold font-['Onest'] leading-5">
                {{ slide.discount }}
              </span>
            </div>

            <!-- CTA buttons -->
            <div class="self-stretch pt-3 inline-flex justify-start items-start gap-3">
              <button
                class="px-6 py-2.5 bg-gradient-to-b from-orange-500 to-orange-600 rounded-xl shadow-[0px_4px_16px_0px_rgba(249,115,22,0.35)] text-white text-sm font-semibold font-['Onest'] leading-5 transition-all duration-150 hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_4px_24px_0px_rgba(249,115,22,0.55)] active:scale-[0.97]"
                @click="emit('buy', slide)"
              >
                Купити зараз
              </button>
              <button
                class="px-5 py-2.5 bg-white/5 rounded-xl outline outline-1 outline-offset-[-1px] outline-white/10 text-gray-300 text-sm font-normal font-['Onest'] leading-5 transition-all duration-150 hover:bg-white/10 hover:outline-white/20 hover:text-white active:scale-[0.97]"
                @click="emit('details', slide)"
              >
                Детальніше
              </button>
            </div>
          </div>
        </div>
      </transition-group>

      <!-- Prev arrow -->
      <button
        aria-label="Назад"
        class="absolute left-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-[#1a1f2e] rounded-full outline outline-1 outline-offset-[-1px] outline-white/5 flex justify-center items-center transition-all duration-150 hover:bg-[#252b3d] hover:outline-white/15 hover:scale-110 active:scale-95"
        @click="prev"
      >
        <svg class="w-4 h-4 text-gray-400" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M10 12L6 8L10 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>

      <!-- Next arrow -->
      <button
        aria-label="Вперед"
        class="absolute right-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-[#1a1f2e] rounded-full outline outline-1 outline-offset-[-1px] outline-white/5 flex justify-center items-center transition-all duration-150 hover:bg-[#252b3d] hover:outline-white/15 hover:scale-110 active:scale-95"
        @click="next"
      >
        <svg class="w-4 h-4 text-gray-400" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M6 12L10 8L6 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>

      <!-- Dot indicators -->
      <div class="absolute bottom-4 left-1/2 -translate-x-1/2 z-20 flex items-center gap-2">
        <button
          v-for="(_, i) in slides"
          :key="i"
          :aria-label="`Слайд ${i + 1}`"
          class="h-2 rounded transition-all duration-300"
          :class="i === current ? 'w-6 bg-orange-500' : 'w-2 bg-white/20 hover:bg-white/40'"
          @click="goTo(i)"
        />
      </div>
    </div>

    <!-- ── Stats row ────────────────────────────────────────────────────────── -->
    <div class="self-stretch pt-2 inline-flex justify-center items-start gap-3">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="flex-1 self-stretch p-3 bg-[#1c1f2a] rounded-xl outline outline-1 outline-offset-[-1px] outline-white/5 inline-flex flex-col justify-start items-start gap-0.5 transition-all duration-150 hover:bg-[#222637] hover:outline-white/10"
      >
        <div class="self-stretch text-center text-xl font-bold font-['Unbounded'] leading-7" :class="stat.color">
          {{ stat.value }}
        </div>
        <div class="self-stretch text-center text-[#7c6e7e] text-xs font-normal font-['Onest'] leading-4">
          {{ stat.label }}
        </div>
      </div>
    </div>

  </section>
</template>

<style scoped>
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
  position: absolute;
  inset: 0;
}
.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(24px);
}
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-24px);
}
</style>
