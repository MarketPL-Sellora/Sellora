<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
dayjs.extend(utc)
import type { GroupBuySession } from '../../state/groupBuyStore'
import { useGroupBuyStore }     from '../../state/groupBuyStore'
import type { ProductApiItem }  from '../../state/productStore'
import { useCartStore }         from '../../state/cartStore'
import { useUserStore }         from '../../state/userStore'
import { DICT }                 from '../../constants/dictionary'

interface ReviewItem {
  id: number;
  user_id?: number;
  email?: string;
  rating: number;
  comment: string;
  created_at?: string;
}

const props = defineProps<{
  apiProduct?: ProductApiItem | null
  sessionData?: GroupBuySession | null
}>()

const groupBuyStore = useGroupBuyStore()
const cartStore = useCartStore()
const userStore = useUserStore()
const showCartToast = ref(false)

async function handleAddToCart() {
  if (!props.apiProduct?.id) return

  if (!userStore.isAuthenticated) {
    userStore.isAuthModalOpen = true
    return
  }

  const success = await cartStore.addToCart(props.apiProduct.id)
  if (success) {
    showCartToast.value = true
    setTimeout(() => { showCartToast.value = false }, 3000)
  }
}
const activeSessionForProduct = computed(() => {
  if (!props.apiProduct?.id) return null
  
  return groupBuyStore.mySessions.find(s => {
    const isMatch = s.productId === props.apiProduct!.id
    const isNotExpired = s.expiresAt ? dayjs.utc(s.expiresAt).diff(dayjs(), 'second') > 0 : true
    return isMatch && isNotExpired
  })
})

const isUserInGroup = computed(() => !!activeSessionForProduct.value)

const resolvedSession = computed(() => {
  return props.sessionData || activeSessionForProduct.value
})

const localMembersCount = ref(0)

watch(resolvedSession, (newData) => {
  if (newData) {
    localMembersCount.value = newData.currentMembersCount
  }
}, { immediate: true })

function handleJoin() {
  emit('start-group')
}

const isGroupFull = computed(() => {
  if (!resolvedSession.value) return false
  return localMembersCount.value >= resolvedSession.value.targetSize
})

const isExpired = computed(() => {
  if (!resolvedSession.value?.expiresAt) return false
  return dayjs.utc(resolvedSession.value.expiresAt).diff(dayjs(), 'second') <= 0
})

interface MemoryOption { label: string }
interface ColorOption  { label: string; hex: string }
interface DeliveryRow  { icon: 'truck' | 'home' | 'shield' | 'store'; title: string; subtitle: string; badge: string; badgeGreen: boolean }

const product = computed(() => {
  const ap = props.apiProduct
  if (!ap) {
    return {
      brand: 'Sellora',
      name: 'Завантаження...',
      sku: '—',
      rating: 0,
      reviewCount: 0,
      inStock: false,
      groupPrice: 0,
      standardPrice: 0,
      savings: 0,
      discount: 0,
      groupMinPeople: 0,
      groupHours: 24,
      groupCurrent: 0,
      groupTotal: 0,
      isGroupBuyAvailable: false,
    }
  }

  const standardPrice = ap.standardPrice || 0
  const groupPrice    = ap.groupPrice || 0
  const groupTotal    = ap.groupTargetSize || 0
  const isGroupBuyAvailable = groupPrice > 0 && groupTotal > 0
  const savings  = isGroupBuyAvailable ? standardPrice - groupPrice : 0
  const discount = isGroupBuyAvailable && standardPrice > 0
    ? Math.round((savings / standardPrice) * 100)
    : 0

  return {
    brand: ap.attributes?.['Бренд'] || 'Sellora',
    name: ap.title,
    sku: String(ap.id),
    rating: (ap as any).rating || 0,
    reviewCount: (ap as any).reviews_count || 0,
    reviews: ((ap as any).reviews as ReviewItem[]) || [],
    inStock: (ap.stockQuantity ?? 0) > 0,
    groupPrice,
    standardPrice,
    savings,
    discount,
    groupMinPeople: groupTotal,
    groupHours: 24,
    groupCurrent: 0,
    groupTotal,
    isGroupBuyAvailable,
  }
})

const memoryOptions: MemoryOption[] = [
  { label: '128 ГБ' }, { label: '256 ГБ' }, { label: '512 ГБ' }, { label: '1 ТБ' },
]
const selectedMemory = ref('256 ГБ')

const colorOptions: ColorOption[] = [
  { label: 'TITANIUM BLACK',  hex: '#1c1c1e' },
  { label: 'TITANIUM GRAY',   hex: '#c9c5bc' },
  { label: 'TITANIUM BLUE',   hex: '#a8b9cc' },
  { label: 'TITANIUM YELLOW', hex: '#d4c48a' },
  { label: 'TITANIUM VIOLET', hex: '#7c6e7e' },
]
const selectedColor = ref('TITANIUM BLACK')

const stars = computed(() => {
  const full  = Math.floor(product.value.rating)
  const empty = 5 - full
  return { full, empty }
})

const deliveryRows: DeliveryRow[] = [
  { icon: 'truck',  title: 'Нова Пошта',        subtitle: 'Доставка 1–2 дні · від 50 ₴',   badge: 'Безкоштовно', badgeGreen: true  },
  { icon: 'home',   title: "Кур'єр додому",      subtitle: 'Доставка в день замовлення',     badge: 'від 80 ₴',    badgeGreen: false },
  { icon: 'shield', title: 'Гарантія виробника', subtitle: '24 місяці офіційна гарантія',    badge: '24 міс',      badgeGreen: true  },
  { icon: 'store',  title: 'Самовивіз',          subtitle: 'Більше 500 пунктів видачі',      badge: 'Безкоштовно', badgeGreen: true  },
]

const groupPercent = computed(() => {
  const current = resolvedSession.value ? localMembersCount.value : product.value.groupCurrent;
  const total = resolvedSession.value ? resolvedSession.value.targetSize : product.value.groupTotal;
  if (total === 0) return 0;
  return Math.round((current / total) * 100);
})

const countdownText = ref('—')
let timerInterval: ReturnType<typeof setInterval>

function updateCountdown() {
  if (!resolvedSession.value?.expiresAt) {
    countdownText.value = '—'
    return
  }
  const now = dayjs()
  const end = dayjs.utc(resolvedSession.value.expiresAt)
  const diff = end.diff(now, 'second')
  if (diff <= 0) {
    countdownText.value = DICT.product.timeExpired
    return
  }
  const h = Math.floor(diff / 3600)
  const m = Math.floor((diff % 3600) / 60)
  const s = diff % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  countdownText.value = `${pad(h)}:${pad(m)}:${pad(s)}`
}

watch(resolvedSession, () => {
  updateCountdown()
}, { immediate: true })

onMounted(async () => {
  await groupBuyStore.fetchMySessions()
  timerInterval = setInterval(updateCountdown, 1000)
})
onUnmounted(() => clearInterval(timerInterval))

const countdownFormatted = computed(() => countdownText.value)

const fmt = (n: number) => n.toLocaleString('uk-UA') + ' ₴'

const emit = defineEmits<{
  (e: 'start-group'): void
  (e: 'buy-standard'): void
  (e: 'open-reviews'): void
}>()
</script>

<template>
  <div class="w-full inline-flex flex-col justify-start items-start gap-1">

    <!-- Brand / seller link -->
    <div class="self-stretch pt-1 pb-1 flex items-center gap-3">
      <router-link
        v-if="apiProduct?.merchantId"
        :to="{ path: '/', query: { storeId: apiProduct.merchantId } }"
        class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-orange-400 text-xs font-semibold font-['Onest'] uppercase tracking-wider transition-colors duration-150"
        style="background: rgba(249,115,22,0.08);"
        @mouseenter="(e: MouseEvent) => (e.currentTarget as HTMLElement).style.background = 'rgba(249,115,22,0.14)'"
        @mouseleave="(e: MouseEvent) => (e.currentTarget as HTMLElement).style.background = 'rgba(249,115,22,0.08)'"
      >
        <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
        </svg>
        {{ DICT.product.sellerProducts }}
      </router-link>
      <span v-else class="text-orange-400/70 text-xs font-['Onest'] uppercase tracking-wider">
        {{ product.brand }}
      </span>
    </div>

    <!-- Title -->
    <div class="self-stretch pb-3">
      <h1 class="text-white text-2xl font-normal font-['Unbounded'] leading-8">
        {{ product.name }}
      </h1>
    </div>

    <!-- Rating + stock -->
    <div class="self-stretch pb-5 flex justify-start items-center gap-4 flex-wrap" style="border-bottom: 1px solid rgba(255,255,255,0.05);">

      <button
        class="flex items-center gap-1.5 transition-opacity duration-150 hover:opacity-80"
        @click="emit('open-reviews')"
      >
        <div class="flex gap-0.5">
          <svg v-for="i in stars.full"  :key="'f' + i" class="w-3.5 h-3.5 text-yellow-400" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 1l1.8 3.6L14 5.3l-3 2.9.7 4.1L8 10.4l-3.7 1.9.7-4.1-3-2.9 4.2-.7z"/>
          </svg>
          <svg v-for="i in stars.empty" :key="'e' + i" class="w-3.5 h-3.5 text-[#1f2333]" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 1l1.8 3.6L14 5.3l-3 2.9.7 4.1L8 10.4l-3.7 1.9.7-4.1-3-2.9 4.2-.7z"/>
          </svg>
        </div>
        <span class="text-[#4b5563] text-xs font-['Onest']">{{ product.rating }}</span>
        <span class="text-orange-400/80 text-xs font-['Onest'] hover:text-orange-300 transition-colors">({{ product.reviewCount }} {{ DICT.product.reviews }})</span>
      </button>

      <div class="flex items-center gap-1.5">
        <span class="w-1.5 h-1.5 rounded-full" :class="product.inStock ? 'bg-green-400' : 'bg-red-400'" />
        <span class="text-xs font-['Onest']" :class="product.inStock ? 'text-green-400' : 'text-red-400'">
          {{ product.inStock ? DICT.product.inStock : DICT.product.outOfStock }}
        </span>
      </div>
    </div>

    <!-- Memory options (static stub) -->
    <div v-if="false" class="self-stretch pt-4 flex flex-col gap-2">
      <span class="text-[#374151] text-xs font-['Onest'] uppercase tracking-wide">Об'єм пам'яті</span>
      <div class="inline-flex flex-wrap gap-2">
        <button
          v-for="opt in memoryOptions" :key="opt.label"
          class="px-4 py-2 rounded-xl text-sm font-['Onest'] transition-all duration-150 active:scale-95"
          :style="selectedMemory === opt.label
            ? 'background: rgba(249,115,22,0.1); border: 1px solid rgba(249,115,22,0.4); color: #f97316;'
            : 'background: transparent; border: 1px solid rgba(255,255,255,0.07); color: #6b7280;'"
          @click="selectedMemory = opt.label"
        >{{ opt.label }}</button>
      </div>
    </div>

    <!-- Color options (static stub) -->
    <div v-if="false" class="self-stretch pt-3 flex flex-col gap-2">
      <span class="text-[#374151] text-xs font-['Onest'] uppercase tracking-wide">
        Колір: <span class="text-[#6b7280] normal-case tracking-normal">{{ selectedColor.charAt(0) + selectedColor.slice(1).toLowerCase() }}</span>
      </span>
      <div class="inline-flex gap-3">
        <button
          v-for="col in colorOptions" :key="col.label" :title="col.label"
          class="w-7 h-7 rounded-full transition-all duration-150 hover:scale-110 active:scale-95 focus:outline-none"
          :style="{ backgroundColor: col.hex }"
          :class="selectedColor === col.label ? 'ring-2 ring-orange-500 ring-offset-2 ring-offset-[#0d0f14]' : 'ring-1 ring-transparent'"
          @click="selectedColor = col.label"
        />
      </div>
    </div>

    <!-- ── Group buy block ─────────────────────────────────────────────────── -->
    <template v-if="product.isGroupBuyAvailable">
      <div
        class="self-stretch mt-4 px-5 py-6 rounded-2xl flex flex-col gap-3"
        style="background: linear-gradient(135deg, #130a00 0%, #1a0e00 50%, #110800 100%); border: 1px solid rgba(249,115,22,0.2);"
      >
        <!-- Header -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-xl flex justify-center items-center shrink-0" style="background: #f97316;">
              <svg class="w-4 h-4 text-white" viewBox="0 0 20 20" fill="currentColor">
                <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zm5 2a2 2 0 11-4 0 2 2 0 014 0zm-4 7a4 4 0 00-8 0v1h8v-1zM2 8a2 2 0 114 0 2 2 0 01-4 0zm13 7v-1a6 6 0 00-1.05-3.38A4 4 0 0118 15h1v1h-4zm-10-1v1H1v-1h1a4 4 0 013.05-3.88A6 6 0 005 14z"/>
              </svg>
            </div>
            <span class="text-orange-300 text-sm font-['Onest']">
              {{ resolvedSession ? DICT.product.groupBuyInvitation : DICT.product.groupBuy }}
            </span>
          </div>
          <span
            class="px-2.5 py-1 rounded-lg text-xs font-semibold font-['Onest']"
            style="background: rgba(249,115,22,0.12); border: 1px solid rgba(249,115,22,0.2); color: #fb923c;"
          >{{ DICT.product.savings }} {{ fmt(product.savings) }}</span>
        </div>

        <!-- Price -->
        <div class="flex flex-wrap items-baseline gap-3">
          <span class="text-orange-400 text-3xl font-normal font-['Unbounded']">{{ fmt(product.groupPrice) }}</span>
          <span class="text-[#374151] text-lg font-['Onest'] line-through">{{ fmt(product.standardPrice) }}</span>
          <span
            class="px-2 py-0.5 rounded-md text-xs font-bold font-['Onest']"
            style="background: rgba(239,68,68,0.12); border: 1px solid rgba(239,68,68,0.2); color: #f87171;"
          >−{{ product.discount }}%</span>
        </div>

        <!-- Description -->
        <p class="text-sm font-['Onest'] leading-6">
          <span class="text-orange-200/50">Збери групу з </span>
          <span class="text-orange-300 font-semibold">{{ resolvedSession ? resolvedSession.targetSize : product.groupMinPeople }} людей</span>
          <span class="text-orange-200/50"> за {{ product.groupHours }} години та отримай знижку!</span>
        </p>

        <!-- Countdown -->
        <div v-if="resolvedSession" class="flex items-center gap-2">
          <svg class="w-4 h-4 text-orange-500/60 shrink-0" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="10" cy="10" r="8"/><path d="M10 6v4l2.5 2.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-[#4b5563] text-xs font-['Onest']">До кінця збору:</span>
          <span
            class="text-sm font-['Unbounded'] tabular-nums"
            :class="countdownFormatted === DICT.product.timeExpired ? 'text-red-400' : 'text-white'"
          >{{ countdownFormatted }}</span>
        </div>

        <!-- Progress -->
        <div class="flex flex-col gap-2">
          <div class="flex justify-between items-center">
            <span class="text-[#4b5563] text-xs font-['Onest']">Учасники групи</span>
            <span class="text-orange-400/80 text-xs font-['Onest']">
              {{ resolvedSession ? localMembersCount : product.groupCurrent }} / {{ resolvedSession ? resolvedSession.targetSize : product.groupTotal }}
            </span>
          </div>
          <!-- Progress bar -->
          <div class="self-stretch h-1.5 rounded-full overflow-hidden" style="background: rgba(255,255,255,0.05);">
            <div
              class="h-full rounded-full transition-all duration-500"
              style="background: linear-gradient(90deg, #f97316, #fb923c);"
              :style="{ width: groupPercent + '%' }"
            />
          </div>
          <div class="flex justify-between">
            <span
              v-for="step in (resolvedSession ? resolvedSession.targetSize : product.groupTotal) + 1"
              :key="step"
              class="text-xs font-['Onest']"
              :class="step - 1 === (resolvedSession ? resolvedSession.targetSize : product.groupTotal) ? 'text-green-400' : 'text-[#1f2333]'"
            >{{ step - 1 }}{{ step - 1 === (resolvedSession ? resolvedSession.targetSize : product.groupTotal) ? ' ✓' : '' }}</span>
          </div>
        </div>

        <!-- Action buttons -->
        <template v-if="resolvedSession">
          <div v-if="isExpired" class="w-full py-4 rounded-xl text-[#374151] text-xs font-['Unbounded'] text-center cursor-not-allowed" style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05);">
            ЗБІР ЗАВЕРШЕНО (ЧАС ВИЧЕРПАНО)
          </div>
          <div v-else-if="isUserInGroup" class="w-full py-4 rounded-xl text-white text-xs font-['Unbounded'] text-center" style="background: rgba(34,197,94,0.15); border: 1px solid rgba(34,197,94,0.2); color: #4ade80;">
            ВИ ВЖЕ БЕРЕТЕ УЧАСТЬ ✓
          </div>
          <div v-else-if="isGroupFull" class="w-full py-4 rounded-xl text-xs font-['Unbounded'] text-center cursor-not-allowed" style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05); color: #374151;">
            ЗБІР ЗАВЕРШЕНО (ГРУПА ПОВНА)
          </div>
          <button
            v-else
            class="w-full py-4 rounded-xl text-white text-xs font-semibold font-['Unbounded'] uppercase tracking-wide transition-all duration-200 active:scale-[0.99] focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed"
            style="background: #f97316; box-shadow: 0 4px 24px rgba(249,115,22,0.25);"
            :disabled="groupBuyStore.isLoading"
            @click="handleJoin"
          >
            {{ groupBuyStore.isLoading ? 'ПРИЄДНУЄМОСЯ...' : DICT.actions.join }}
          </button>
        </template>

        <template v-else>
          <button
            class="w-full py-4 rounded-xl text-white text-xs font-semibold font-['Unbounded'] uppercase tracking-wide transition-all duration-200 active:scale-[0.99] focus:outline-none"
            style="background: #f97316; box-shadow: 0 4px 24px rgba(249,115,22,0.25);"
            @click="emit('start-group')"
          >
            ПОЧАТИ ГРУПОВУ ПОКУПКУ
          </button>
        </template>
      </div>
    </template>

    <!-- Standard price button (below group buy) -->
    <button
      v-if="product.isGroupBuyAvailable"
      class="self-stretch py-3 rounded-xl text-[#4b5563] text-sm font-['Onest'] text-center transition-all duration-150 hover:text-[#6b7280] active:scale-[0.99] focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed"
      style="border: 1px solid rgba(255,255,255,0.06);"
      :disabled="cartStore.isLoading"
      @click="handleAddToCart"
    >
      {{ cartStore.isLoading ? 'Додаємо...' : `${DICT.product.buyStandard} (${fmt(product.standardPrice)})` }}
    </button>

    <!-- Price block (no group buy) -->
    <div
      v-if="!product.isGroupBuyAvailable"
      class="self-stretch mt-4 px-5 py-6 rounded-2xl flex flex-col gap-4"
      style="background: #0d0f14; border: 1px solid rgba(255,255,255,0.06);"
    >
      <span class="text-white text-3xl font-semibold font-['Unbounded']">{{ fmt(product.standardPrice) }}</span>
      <button
        class="w-full py-4 rounded-xl text-white text-sm font-semibold font-['Unbounded'] uppercase tracking-wide text-center transition-all duration-200 active:scale-[0.99] focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed"
        style="background: #f97316; box-shadow: 0 4px 28px rgba(249,115,22,0.25);"
        :disabled="cartStore.isLoading"
        @click="handleAddToCart"
      >
        {{ cartStore.isLoading ? 'Додаємо...' : DICT.actions.buy }}
      </button>
    </div>

    <!-- Delivery & warranty block -->
    <div
      class="self-stretch mt-2 rounded-xl overflow-hidden"
      style="border: 1px solid rgba(255,255,255,0.06);"
    >
      <!-- Header -->
      <div class="px-4 py-3" style="background: rgba(255,255,255,0.02); border-bottom: 1px solid rgba(255,255,255,0.05);">
        <span class="text-[#374151] text-xs font-['Onest'] uppercase tracking-wider">{{ DICT.product.deliveryAndWarranty }}</span>
      </div>

      <!-- Rows -->
      <div>
        <div
          v-for="(row, i) in deliveryRows"
          :key="row.title"
          class="px-4 py-3 flex items-center gap-3 transition-colors duration-150 hover:bg-white/[0.02]"
          :style="i > 0 ? 'border-top: 1px solid rgba(255,255,255,0.04);' : ''"
        >
          <!-- Icon -->
          <div class="w-8 h-8 rounded-lg flex justify-center items-center shrink-0" style="background: rgba(249,115,22,0.08);">
            <svg v-if="row.icon === 'truck'"  class="w-4 h-4 text-orange-500/70" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M1 4h11v9H1zm10 0h3l3 4v5h-6V4z" stroke-linecap="round" stroke-linejoin="round"/><circle cx="4.5" cy="14.5" r="1.5"/><circle cx="13.5" cy="14.5" r="1.5"/></svg>
            <svg v-else-if="row.icon === 'home'"   class="w-4 h-4 text-orange-500/70" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M3 9.5L10 3l7 6.5V17a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z" stroke-linecap="round" stroke-linejoin="round"/><path d="M7 18v-7h6v7" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <svg v-else-if="row.icon === 'shield'" class="w-4 h-4 text-orange-500/70" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M10 2l7 2.8v5.7C17 14 14 17.4 10 18c-4-0.6-7-4-7-7.5V4.8L10 2z" stroke-linecap="round" stroke-linejoin="round"/><path d="M7 10l2 2 4-4" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <svg v-else-if="row.icon === 'store'"  class="w-4 h-4 text-orange-500/70" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M2 7l1.5-4h13L18 7H2z" stroke-linecap="round" stroke-linejoin="round"/><path d="M3 7v10a1 1 0 001 1h12a1 1 0 001-1V7" stroke-linecap="round"/><path d="M8 17v-5h4v5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>

          <!-- Text -->
          <div class="flex flex-col gap-0.5 min-w-0">
            <span class="text-[#9ca3af] text-sm font-['Onest']">{{ row.title }}</span>
            <span class="text-[#374151] text-xs font-['Onest']">{{ row.subtitle }}</span>
          </div>

          <!-- Badge -->
          <span
            class="ml-auto shrink-0 text-xs font-['Onest'] px-2 py-0.5 rounded-md"
            :style="row.badgeGreen
              ? 'background: rgba(34,197,94,0.08); color: #4ade80; border: 1px solid rgba(34,197,94,0.15);'
              : 'background: rgba(255,255,255,0.04); color: #4b5563; border: 1px solid rgba(255,255,255,0.06);'"
          >{{ row.badge }}</span>
        </div>
      </div>
    </div>

    <!-- Cart toast -->
    <Transition name="fade">
      <div
        v-if="showCartToast"
        class="fixed bottom-8 left-1/2 -translate-x-1/2 z-50 flex items-center gap-2 px-5 py-3 rounded-xl text-white text-sm font-semibold font-['Onest'] tracking-wide"
        style="background: #059669; box-shadow: 0 8px 24px rgba(5,150,105,0.3);"
      >
        <svg class="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
        </svg>
        Товар додано до кошика!
      </div>
    </Transition>

  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translate(-50%, 20px); }
</style>