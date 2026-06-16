<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface PromoSlide {
  productId: number
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

const displaySlides = ref<PromoSlide[]>([
  {
    productId: 1, // TODO: Вписати ID iPhone з БД
    badge: '🔥 Гаряча пропозиція',
    titleWhite: 'iPhone 15',
    titleAccent: 'Pro 256GB',
    description: 'Titanium Black. Міцний. Легкий. Професійний.\nКамера 48Мп · A17 Pro · USB-C',
    price: '48 000 ₴',
    oldPrice: '52 000 ₴',
    discount: '-8%',
    image: new URL('../../assets/Pro15.webp', import.meta.url).href,
    imageAlt: 'iPhone 15 Pro'
  },
  {
    productId: 3, // TODO: Вписати ID Samsung з БД
    badge: '⚡ Новинка',
    titleWhite: 'Samsung Galaxy',
    titleAccent: 'S24 Ultra',
    description: 'AI-можливості нового рівня у кольорі Titanium Gray.\nS Pen · 200Мп · Snapdragon 8 Gen 3',
    price: '46 000 ₴',
    oldPrice: '50 000 ₴',
    discount: '-8%',
    image: new URL('../../assets/Sam.webp', import.meta.url).href,
    imageAlt: 'Samsung Galaxy S24 Ultra'
  },
  {
    productId: 2, // TODO: Вписати ID MacBook з БД
    badge: '🎯 Топ продажів',
    titleWhite: 'MacBook Pro',
    titleAccent: 'M3 14"',
    description: 'Неймовірна продуктивність для професіоналів.\nChip M3 · 18GB RAM · 512GB SSD',
    price: '80 000 ₴',
    oldPrice: '85 000 ₴',
    discount: '-6%',
    image: new URL('../../assets/M3.png', import.meta.url).href,
    imageAlt: 'MacBook Pro M3'
  }
])

const stats: Stat[] = [
  { value: '2.4M+',   label: 'Товарів',    color: 'text-orange-500' },
  { value: '320K',    label: 'Продавців',  color: 'text-white'      },
  { value: '98%',     label: 'Позитивних', color: 'text-green-400'  },
  { value: '1-3 дні', label: 'Доставка',  color: 'text-blue-400'   },
]

const current = ref(0)
const isAnimating = ref(false)

function goTo(index: number) {
  if (isAnimating.value || index === current.value) return
  isAnimating.value = true
  current.value = (index + displaySlides.value.length) % displaySlides.value.length
  setTimeout(() => (isAnimating.value = false), 400)
}

function prev() { goTo(current.value - 1) }
function next() { goTo(current.value + 1) }

let timer: ReturnType<typeof setInterval>
onMounted(() => { timer = setInterval(next, 5000) })
onUnmounted(() => { clearInterval(timer) })

function pauseAutoPlay() { clearInterval(timer) }
function resumeAutoPlay() {
  clearInterval(timer)
  timer = setInterval(next, 5000)
}
</script>

<template>
  <section class="self-stretch flex flex-col gap-0">

    <!-- ── Hero слайдер ──────────────────────────────────────────────────── -->
    <div
      class="self-stretch h-[380px] sm:h-[320px] md:h-[300px] relative rounded-t-2xl overflow-hidden"
      @mouseenter="pauseAutoPlay"
      @mouseleave="resumeAutoPlay"
    >
      <!-- Базовий фон -->
      <div class="absolute inset-0 bg-[#0d1017]" />

      <!-- Тонка сітка -->
      <div
        class="pointer-events-none absolute inset-0"
        style="
          background-image:
            linear-gradient(rgba(255,255,255,0.022) 1px, transparent 1px),
            linear-gradient(90deg, rgba(255,255,255,0.022) 1px, transparent 1px);
          background-size: 48px 48px;
        "
      />

      <!-- Slides -->
      <transition-group name="slide-fade" tag="div" class="absolute inset-0">
        <div
          v-for="(slide, i) in displaySlides"
          v-show="i === current"
          :key="slide.imageAlt + i"
          class="absolute inset-0 flex items-center"
        >

          <!-- ── Зображення: Full Bleed — фото заповнює правий бік ── -->
          <div class="hidden md:block absolute right-0 top-0 bottom-0 w-[55%] overflow-hidden">

            <!-- Фото як фон — object-cover заповнює весь правий блок -->
            <img
              :src="slide.image"
              :alt="''"
              aria-hidden="true"
              class="absolute inset-0 w-full h-full object-cover object-center select-none pointer-events-none"
            />

            <!-- Fade зліва — головний перехід до темного фону -->
            <div
              class="absolute inset-0 pointer-events-none"
              style="background: linear-gradient(to right, #0d1017 0%, rgba(13,16,23,0.92) 10%, rgba(13,16,23,0.6) 28%, rgba(13,16,23,0.15) 52%, transparent 68%);"
            />

            <!-- Fade зверху -->
            <div
              class="absolute inset-0 pointer-events-none"
              style="background: linear-gradient(to bottom, rgba(13,16,23,0.75) 0%, transparent 35%);"
            />

            <!-- Fade знизу -->
            <div
              class="absolute inset-0 pointer-events-none"
              style="background: linear-gradient(to top, rgba(13,16,23,0.75) 0%, transparent 35%);"
            />

            <!-- Fade справа -->
            <div
              class="absolute inset-0 pointer-events-none"
              style="background: linear-gradient(to left, rgba(13,16,23,0.4) 0%, transparent 20%);"
            />
          </div>

          <!-- ── Текстовий контент ── -->
          <div class="relative z-10 w-full md:w-[52%] px-10 md:px-12 flex flex-col items-start gap-3 pb-8 md:pb-0">

            <!-- Бейдж -->
            <span
              class="px-3 py-1 rounded-full text-[10px] font-semibold font-['Onest'] uppercase tracking-widest text-orange-400"
              style="background: rgba(249,115,22,0.1); border: 1px solid rgba(249,115,22,0.2);"
            >{{ slide.badge }}</span>

            <!-- Заголовок -->
            <div>
              <div class="text-white font-black font-['Unbounded'] leading-tight" style="font-size: clamp(1.4rem, 2.6vw, 2.1rem);">
                {{ slide.titleWhite }}
              </div>
              <div class="text-orange-500 font-black font-['Unbounded'] leading-tight" style="font-size: clamp(1.4rem, 2.6vw, 2.1rem);">
                {{ slide.titleAccent }}
              </div>
            </div>

            <!-- Опис -->
            <p class="text-[#6b7280] text-sm font-['Onest'] leading-6 max-w-[280px] line-clamp-2">
              {{ slide.description }}
            </p>

            <!-- Ціна -->
            <div class="flex items-baseline gap-2.5">
              <span class="text-white text-2xl font-bold font-['Unbounded']">{{ slide.price }}</span>
              <span v-if="slide.oldPrice" class="text-[#374151] text-sm font-['Onest'] line-through">{{ slide.oldPrice }}</span>
              <span
                v-if="slide.discount"
                class="px-2 py-0.5 rounded-md text-xs font-bold font-['Onest']"
                style="background: rgba(34,197,94,0.1); border: 1px solid rgba(34,197,94,0.2); color: #4ade80;"
              >{{ slide.discount }}</span>
            </div>

            <!-- Кнопки -->
            <div class="flex items-center gap-2.5 pt-1">
              <button
                class="px-5 py-2 rounded-xl text-white text-sm font-semibold font-['Onest'] whitespace-nowrap transition-all duration-150 active:scale-[0.97]"
                style="background: #f97316; box-shadow: 0 0 20px rgba(249,115,22,0.3);"
                @click="router.push('/product/' + slide.productId)"
              >
                Купити зараз
              </button>
              <button
                class="px-4 py-2 rounded-xl text-[#9ca3af] text-sm font-['Onest'] whitespace-nowrap transition-all duration-150 hover:text-white active:scale-[0.97]"
                style="background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08);"
                @click="router.push('/product/' + slide.productId)"
              >
                Детальніше
              </button>
            </div>
          </div>
        </div>
      </transition-group>

      <!-- Стрілка ліво -->
      <button
        aria-label="Назад"
        class="absolute left-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-150 hover:scale-110 active:scale-95"
        style="background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08);"
        @click="prev"
      >
        <svg class="w-4 h-4 text-[#6b7280]" viewBox="0 0 16 16" fill="none">
          <path d="M10 12L6 8L10 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>

      <!-- Стрілка право -->
      <button
        aria-label="Вперед"
        class="absolute right-3 top-1/2 -translate-y-1/2 z-20 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-150 hover:scale-110 active:scale-95"
        style="background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08);"
        @click="next"
      >
        <svg class="w-4 h-4 text-[#6b7280]" viewBox="0 0 16 16" fill="none">
          <path d="M6 12L10 8L6 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>

      <!-- Dots -->
      <div class="absolute bottom-4 left-1/2 -translate-x-1/2 z-20 flex items-center gap-1.5">
        <button
          v-for="(_, i) in displaySlides"
          :key="i"
          :aria-label="`Слайд ${i + 1}`"
          class="h-1 rounded-full transition-all duration-300"
          :class="i === current ? 'w-6 bg-orange-500' : 'w-1.5 bg-white/20 hover:bg-white/40'"
          @click="goTo(i)"
        />
      </div>
    </div>

    <!-- ── Stats ───────────────────────────────────────────────────────────── -->
    <div
      class="self-stretch rounded-b-2xl flex overflow-hidden"
      style="background: #111319; border: 1px solid rgba(255,255,255,0.06); border-top: none;"
    >
      <div
        v-for="(stat, i) in stats"
        :key="stat.label"
        class="flex-1 py-3.5 flex flex-col items-center gap-0.5 relative cursor-default transition-colors duration-150 hover:bg-white/[0.025]"
      >
        <div v-if="i < stats.length - 1" class="absolute right-0 top-[22%] h-[56%] w-px bg-white/[0.06]" />
        <span class="text-lg font-bold font-['Unbounded']" :class="stat.color">{{ stat.value }}</span>
        <span class="text-[11px] font-['Onest'] text-[#4b5563]">{{ stat.label }}</span>
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
  transform: translateX(20px);
}
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>