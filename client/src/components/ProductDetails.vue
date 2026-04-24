<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface MemoryOption { label: string }
interface ColorOption  { label: string; hex: string }
interface DeliveryRow  { icon: 'truck' | 'home' | 'shield' | 'store'; title: string; subtitle: string; badge: string; badgeGreen: boolean }

// ─── Product data (replace with props later) ──────────────────────────────────

const product = ref({
  brand:       'Samsung',
  name:        'Samsung Galaxy S24 Ultra 12/256GB Titanium Black',
  sku:         'SM-S928B-256-BK',
  rating:      4.1,
  reviewCount: 214,
  inStock:     true,
  groupPrice:  25000,
  oldPrice:    34999,
  savings:     9999,
  discount:    29,
  standardPrice: 34999,
  groupMinPeople: 3,
  groupHours:  24,
  groupCurrent: 0,
  groupTotal:  3,
})

// ─── Memory options ───────────────────────────────────────────────────────────

const memoryOptions: MemoryOption[] = [
  { label: '128 ГБ' },
  { label: '256 ГБ' },
  { label: '512 ГБ' },
  { label: '1 ТБ'   },
]
const selectedMemory = ref('256 ГБ')

// ─── Color options ────────────────────────────────────────────────────────────

const colorOptions: ColorOption[] = [
  { label: 'TITANIUM BLACK',  hex: '#1c1c1e' },
  { label: 'TITANIUM GRAY',   hex: '#c9c5bc' },
  { label: 'TITANIUM BLUE',   hex: '#a8b9cc' },
  { label: 'TITANIUM YELLOW', hex: '#d4c48a' },
  { label: 'TITANIUM VIOLET', hex: '#7c6e7e' },
]
const selectedColor = ref('TITANIUM BLACK')

const selectedColorHex = computed(
  () => colorOptions.find(c => c.label === selectedColor.value)?.hex ?? '#1c1c1e'
)

// ─── Star rendering ───────────────────────────────────────────────────────────

const stars = computed(() => {
  const full  = Math.floor(product.value.rating)
  const empty = 5 - full
  return { full, empty }
})

// ─── Delivery rows ────────────────────────────────────────────────────────────

const deliveryRows: DeliveryRow[] = [
  { icon: 'truck',  title: 'Нова Пошта',          subtitle: 'Доставка 1–2 дні · від 50 ₴',     badge: 'Безкоштовно', badgeGreen: true  },
  { icon: 'home',   title: "Кур'єр додому",        subtitle: 'Доставка в день замовлення',       badge: 'від 80 ₴',    badgeGreen: false },
  { icon: 'shield', title: 'Гарантія виробника',   subtitle: '24 місяці офіційна гарантія',      badge: '24 міс',      badgeGreen: true  },
  { icon: 'store',  title: 'Самовивіз',            subtitle: 'Більше 500 пунктів видачі',        badge: 'Безкоштовно', badgeGreen: true  },
]

// ─── Group progress ───────────────────────────────────────────────────────────

const groupPercent = computed(() =>
  Math.round((product.value.groupCurrent / product.value.groupTotal) * 100)
)

// ─── Countdown timer ─────────────────────────────────────────────────────────

const countdown = ref({ h: 23, m: 58, s: 36 })
let timerInterval: ReturnType<typeof setInterval>

onMounted(() => {
  timerInterval = setInterval(() => {
    const { h, m, s } = countdown.value
    if (s > 0) {
      countdown.value.s--
    } else if (m > 0) {
      countdown.value.m--
      countdown.value.s = 59
    } else if (h > 0) {
      countdown.value.h--
      countdown.value.m = 59
      countdown.value.s = 59
    }
  }, 1000)
})
onUnmounted(() => clearInterval(timerInterval))

const countdownFormatted = computed(() => {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(countdown.value.h)}:${pad(countdown.value.m)}:${pad(countdown.value.s)}`
})

// ─── Price formatting ─────────────────────────────────────────────────────────

const fmt = (n: number) => n.toLocaleString('uk-UA') + ' ₴'

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'start-group'): void
  (e: 'buy-standard'): void
  (e: 'open-reviews'): void
}>()
</script>

<template>
  <div class="w-full inline-flex flex-col justify-start items-start gap-1">

    <!-- ── Brand ─────────────────────────────────────────────────────────── -->
    <div class="self-stretch pt-[5px] pb-[3px]">
      <span class="text-orange-400 text-xs font-normal font-['Onest'] uppercase leading-4 tracking-wider">
        {{ product.brand }}
      </span>
    </div>

    <!-- ── Name ──────────────────────────────────────────────────────────── -->
    <div class="self-stretch pb-2">
      <h1 class="text-white text-2xl font-normal font-['Unbounded'] leading-8">
        {{ product.name }}
      </h1>
    </div>

    <!-- ── Meta row ───────────────────────────────────────────────────────── -->
    <div class="self-stretch pb-5 border-b border-[#1c1f2a] inline-flex justify-start items-center gap-4 flex-wrap">
      <!-- SKU -->
      <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">
        Арт.: <span class="text-[#787d99]">{{ product.sku }}</span>
      </span>

      <!-- Stars -->
      <button
        class="flex items-center gap-1.5 transition-opacity duration-150 hover:opacity-80"
        @click="emit('open-reviews')"
      >
        <div class="flex gap-0.5">
          <svg
            v-for="i in stars.full"
            :key="'f' + i"
            class="w-4 h-4 text-yellow-400"
            viewBox="0 0 16 16" fill="currentColor"
          >
            <path d="M8 1l1.8 3.6L14 5.3l-3 2.9.7 4.1L8 10.4l-3.7 1.9.7-4.1-3-2.9 4.2-.7z"/>
          </svg>
          <svg
            v-for="i in stars.empty"
            :key="'e' + i"
            class="w-4 h-4 text-[#3d4158]"
            viewBox="0 0 16 16" fill="currentColor"
          >
            <path d="M8 1l1.8 3.6L14 5.3l-3 2.9.7 4.1L8 10.4l-3.7 1.9.7-4.1-3-2.9 4.2-.7z"/>
          </svg>
        </div>
        <span class="text-[#787d99] text-sm font-normal font-['Onest'] leading-5">{{ product.rating }}</span>
        <span class="text-orange-400 text-xs font-normal font-['Onest'] leading-4 hover:text-orange-300 transition-colors duration-150">
          ({{ product.reviewCount }} відгуки)
        </span>
      </button>

      <!-- Stock -->
      <div class="flex items-center gap-1.5">
        <span class="w-2 h-2 rounded-full" :class="product.inStock ? 'bg-green-400' : 'bg-red-400'" />
        <span class="text-xs font-normal font-['Onest'] leading-4" :class="product.inStock ? 'text-green-400' : 'text-red-400'">
          {{ product.inStock ? 'В наявності' : 'Немає в наявності' }}
        </span>
      </div>
    </div>

    <!-- ── Memory ─────────────────────────────────────────────────────────── -->
    <div class="self-stretch pt-4 flex flex-col gap-2">
      <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] uppercase leading-4 tracking-wide">
        Об'єм пам'яті
      </span>
      <div class="inline-flex flex-wrap gap-2">
        <button
          v-for="opt in memoryOptions"
          :key="opt.label"
          class="px-4 py-2 rounded-xl outline outline-1 outline-offset-[-1px] text-sm font-normal font-['Onest'] leading-5 transition-all duration-150 active:scale-95"
          :class="
            selectedMemory === opt.label
              ? 'bg-orange-500/10 outline-orange-500 text-orange-500'
              : 'outline-[#2a2d3e] text-[#a0a5bf] hover:outline-[#3d4158] hover:text-gray-300'
          "
          @click="selectedMemory = opt.label"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- ── Color ──────────────────────────────────────────────────────────── -->
    <div class="self-stretch h-20 pt-3 flex flex-col gap-2">
      <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] uppercase leading-4 tracking-wide">
        Колір: <span class="text-[#a0a5bf] normal-case tracking-normal">{{ selectedColor.charAt(0) + selectedColor.slice(1).toLowerCase() }}</span>
      </span>
      <div class="inline-flex gap-3">
        <button
          v-for="col in colorOptions"
          :key="col.label"
          :title="col.label.charAt(0) + col.label.slice(1).toLowerCase()"
          class="w-8 h-8 rounded-full transition-all duration-150 hover:scale-110 active:scale-95 focus:outline-none"
          :style="{ backgroundColor: col.hex }"
          :class="
            selectedColor === col.label
              ? 'ring-2 ring-orange-500 ring-offset-2 ring-offset-[#111319]'
              : 'ring-2 ring-transparent ring-offset-2 ring-offset-[#111319]'
          "
          @click="selectedColor = col.label"
        />
      </div>
    </div>

    <!-- ── Group purchase block ───────────────────────────────────────────── -->
    <div class="self-stretch px-5 py-8 bg-gradient-to-br from-[#1a0f05] via-[#1c1209] to-[#160d04] rounded-2xl outline outline-2 outline-offset-[-2px] outline-orange-500 flex flex-col gap-2">

      <!-- Header row -->
      <div class="self-stretch inline-flex justify-between items-center">
        <div class="flex items-center gap-2.5">
          <div class="w-8 h-8 bg-orange-500 rounded-lg flex justify-center items-center shrink-0">
            <!-- Heroicons: user-group -->
            <svg class="w-4 h-4 text-white" viewBox="0 0 20 20" fill="currentColor">
              <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zm5 2a2 2 0 11-4 0 2 2 0 014 0zm-4 7a4 4 0 00-8 0v1h8v-1zM2 8a2 2 0 114 0 2 2 0 01-4 0zm13 7v-1a6 6 0 00-1.05-3.38A4 4 0 0118 15h1v1h-4zm-10-1v1H1v-1h1a4 4 0 013.05-3.88A6 6 0 005 14z"/>
            </svg>
          </div>
          <span class="text-orange-300 text-base font-normal font-['Onest'] leading-6">Групова покупка</span>
        </div>
        <div class="px-2.5 py-1 bg-orange-500/10 rounded-lg outline outline-1 outline-offset-[-1px] outline-orange-500/30">
          <span class="text-orange-400 text-xs font-normal font-['Onest'] leading-4">
            Економія {{ fmt(product.savings) }}
          </span>
        </div>
      </div>

      <!-- Prices -->
      <div class="w-full overflow-hidden flex flex-wrap items-baseline gap-x-3 gap-y-1 py-1">
        <span class="text-orange-400 text-3xl md:text-4xl font-normal font-['Unbounded'] leading-tight">
          {{ fmt(product.groupPrice) }}
        </span>
        <div class="flex items-center gap-2">
          <span class="text-[#5a5f7a] text-lg md:text-xl font-normal font-['Onest'] line-through leading-tight">
            {{ fmt(product.oldPrice) }}
          </span>
          <span class="px-2 py-0.5 bg-red-600/80 rounded-lg text-white text-xs md:text-sm font-normal font-['Onest']">
            −{{ product.discount }}%
          </span>
        </div>
      </div>

      <!-- Description -->
      <p class="self-stretch text-sm font-normal font-['Onest'] leading-6">
        <span class="text-orange-200/70">Збери групу з </span>
        <span class="text-orange-300 font-bold">{{ product.groupMinPeople }} людей</span>
        <span class="text-orange-200/70"> за {{ product.groupHours }} години та отримай знижку на весь порядок!</span>
      </p>

      <!-- Timer -->
      <div class="self-stretch pt-2 inline-flex items-center gap-2">
        <!-- Heroicons: clock -->
        <svg class="w-4 h-4 text-orange-400 shrink-0" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="10" cy="10" r="8"/>
          <path d="M10 6v4l2.5 2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span class="text-[#787d99] text-xs font-normal font-['Onest'] leading-4">До кінця збору:</span>
        <span class="text-white text-sm font-normal font-['Unbounded'] leading-5 tabular-nums">{{ countdownFormatted }}</span>
      </div>

      <!-- Progress -->
      <div class="self-stretch py-2 flex flex-col gap-1.5">
        <div class="self-stretch inline-flex justify-between items-center pb-0.5">
          <span class="text-[#787d99] text-xs font-normal font-['Onest'] leading-4">Учасники групи</span>
          <span class="text-orange-300 text-xs font-normal font-['Onest'] leading-4">
            {{ product.groupCurrent }} / {{ product.groupTotal }} учасників
          </span>
        </div>
        <!-- Track -->
        <div class="self-stretch h-2 relative bg-[#1c1f2a]/80 rounded-full overflow-hidden">
          <div
            class="h-2 absolute left-0 top-0 bg-gradient-to-r from-orange-500 to-amber-400 rounded-full transition-all duration-500"
            :style="{ width: groupPercent + '%' }"
          />
        </div>
        <!-- Step labels -->
        <div class="self-stretch inline-flex justify-between items-start">
          <span
            v-for="step in product.groupTotal + 1"
            :key="step"
            class="text-xs font-normal font-['Onest'] leading-4"
            :class="step - 1 === product.groupTotal ? 'text-green-400' : 'text-[#3d4158]'"
          >
            {{ step - 1 }}{{ step - 1 === product.groupTotal ? ' ✓' : '' }}
          </span>
        </div>
      </div>

      <!-- CTA -->
      <button
        class="w-full h-14 py-4 bg-orange-500 rounded-xl text-white text-base font-normal font-['Unbounded'] leading-6 tracking-wide transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_24px_0px_rgba(249,115,22,0.55)] active:scale-[0.99] active:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 focus:ring-offset-[#160d04]"
        @click="emit('start-group')"
      >
        ПОЧАТИ ГРУПОВУ ПОКУПКУ
      </button>
    </div>

    <!-- ── Standard price button ──────────────────────────────────────────── -->
    <button
      class="self-stretch py-3 rounded-xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] text-[#787d99] text-sm font-normal font-['Onest'] leading-5 text-center transition-all duration-150 hover:outline-[#3d4158] hover:text-gray-400 hover:bg-white/5 active:scale-[0.99] focus:outline-none focus:ring-2 focus:ring-white/10"
      @click="emit('buy-standard')"
    >
      Купити за стандартною ціною ({{ fmt(product.standardPrice) }})
    </button>

    <!-- ── Delivery & warranty ────────────────────────────────────────────── -->
    <div class="self-stretch pt-5 rounded-xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] flex flex-col overflow-hidden">
      <!-- Section heading -->
      <div class="self-stretch px-4 py-3 bg-[#161820] border-b border-[#1c1f2a]">
        <span class="text-[#787d99] text-xs font-normal font-['Onest'] uppercase leading-4 tracking-wide">
          Доставка та гарантія
        </span>
      </div>

      <!-- Rows -->
      <div class="self-stretch flex flex-col">
        <div
          v-for="(row, i) in deliveryRows"
          :key="row.title"
          class="self-stretch px-4 py-3 inline-flex justify-start items-center gap-3 transition-colors duration-150 hover:bg-white/[0.03]"
          :class="i > 0 ? 'border-t border-[#1c1f2a]/60' : ''"
        >
          <!-- Icon -->
          <div class="w-8 h-8 bg-[#1c1f2a] rounded-lg flex justify-center items-center shrink-0">
            <!-- truck -->
            <svg v-if="row.icon === 'truck'" class="w-4 h-4 text-orange-400" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M1 4h11v9H1zm10 0h3l3 4v5h-6V4z" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="4.5" cy="14.5" r="1.5"/><circle cx="13.5" cy="14.5" r="1.5"/>
            </svg>
            <!-- home -->
            <svg v-else-if="row.icon === 'home'" class="w-4 h-4 text-orange-400" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M3 9.5L10 3l7 6.5V17a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M7 18v-7h6v7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- shield -->
            <svg v-else-if="row.icon === 'shield'" class="w-4 h-4 text-orange-400" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M10 2l7 2.8v5.7C17 14 14 17.4 10 18c-4-0.6-7-4-7-7.5V4.8L10 2z" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M7 10l2 2 4-4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- store -->
            <svg v-else-if="row.icon === 'store'" class="w-4 h-4 text-orange-400" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M2 7l1.5-4h13L18 7H2z" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M3 7v10a1 1 0 001 1h12a1 1 0 001-1V7" stroke-linecap="round"/>
              <path d="M8 17v-5h4v5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>

          <!-- Text -->
          <div class="flex flex-col gap-0.5 min-w-0">
            <span class="text-[#c8ccdf] text-sm font-normal font-['Onest'] leading-5">{{ row.title }}</span>
            <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">{{ row.subtitle }}</span>
          </div>

          <!-- Badge -->
          <span
            class="ml-auto shrink-0 text-xs font-normal font-['Onest'] leading-4"
            :class="row.badgeGreen ? 'text-green-400' : 'text-[#787d99]'"
          >
            {{ row.badge }}
          </span>
        </div>
      </div>
    </div>

  </div>
</template>
