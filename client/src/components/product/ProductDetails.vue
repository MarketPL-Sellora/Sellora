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
const joinSuccess = computed(() => {
  if (!props.sessionData?.uuid) return false;
  return groupBuyStore.mySessions.some(s => s.uuid === props.sessionData!.uuid);
});
const localMembersCount = ref(0)

watch(() => props.sessionData, (newData) => {
  if (newData) {
    localMembersCount.value = newData.currentMembersCount
  }
}, { immediate: true })

async function handleJoin() {
  if (!props.sessionData?.uuid) return

  const ok = await groupBuyStore.joinSession(props.sessionData.uuid)

  if (ok) {
    localMembersCount.value += 1
  }
}

// ─── ПЕРЕВІРКА НА ЗАПОВНЕНІСТЬ ГРУПИ ─────────────────────────────────────────
const isGroupFull = computed(() => {
  if (!props.sessionData) return false
  return localMembersCount.value >= props.sessionData.targetSize
})

// ─── ПЕРЕВІРКА НА ВИЧЕРПАННЯ ЧАСУ (синхронно через dayjs) ─────────────────
const isExpired = computed(() => {
  if (!props.sessionData?.expiresAt) return false
  return dayjs.utc(props.sessionData.expiresAt).diff(dayjs(), 'second') <= 0
})

interface MemoryOption { label: string }
interface ColorOption  { label: string; hex: string }
interface DeliveryRow  { icon: 'truck' | 'home' | 'shield' | 'store'; title: string; subtitle: string; badge: string; badgeGreen: boolean }

// ─── COMPUTED: маппінг з apiProduct ──────────────────────────────────────────
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
    rating: 0,
    reviewCount: 0,
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
  { icon: 'truck',  title: 'Нова Пошта',          subtitle: 'Доставка 1–2 дні · від 50 ₴',     badge: 'Безкоштовно', badgeGreen: true  },
  { icon: 'home',   title: "Кур'єр додому",        subtitle: 'Доставка в день замовлення',       badge: 'від 80 ₴',    badgeGreen: false },
  { icon: 'shield', title: 'Гарантія виробника',   subtitle: '24 місяці офіційна гарантія',      badge: '24 міс',      badgeGreen: true  },
  { icon: 'store',  title: 'Самовивіз',            subtitle: 'Більше 500 пунктів видачі',        badge: 'Безкоштовно', badgeGreen: true  },
]

const groupPercent = computed(() => {
  const current = props.sessionData ? localMembersCount.value : product.value.groupCurrent;
  const total = props.sessionData ? props.sessionData.targetSize : product.value.groupTotal;
  if (total === 0) return 0;
  return Math.round((current / total) * 100);
})

const countdownText = ref('—')
let timerInterval: ReturnType<typeof setInterval>

function updateCountdown() {
  if (!props.sessionData?.expiresAt) {
    countdownText.value = '—'
    return
  }
  const now = dayjs()
  const end = dayjs.utc(props.sessionData.expiresAt)
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

// Синхронно оновлюємо таймер при зміні sessionData (усуває flicker — не чекаємо onMounted)
watch(() => props.sessionData, () => {
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

    <div class="self-stretch pt-[5px] pb-[3px] flex items-center gap-3">
      <router-link
        v-if="apiProduct?.merchantId"
        :to="{ path: '/', query: { storeId: apiProduct.merchantId } }"
        class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-orange-500/10 hover:bg-orange-500/20 text-orange-400 text-xs font-semibold font-['Onest'] uppercase leading-4 tracking-wider rounded-lg transition-colors duration-150"
      >
        <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path></svg>
        {{ DICT.product.sellerProducts }}
      </router-link>
      <span v-else class="text-orange-400 text-xs font-normal font-['Onest'] uppercase leading-4 tracking-wider">
        {{ product.brand }}
      </span>
    </div>

    <div class="self-stretch pb-2">
      <h1 class="text-white text-2xl font-normal font-['Unbounded'] leading-8">
        {{ product.name }}
      </h1>
    </div>

    <div class="self-stretch pb-5 border-b border-[#1c1f2a] inline-flex justify-start items-center gap-4 flex-wrap">

      <button
        class="flex items-center gap-1.5 transition-opacity duration-150 hover:opacity-80"
        @click="emit('open-reviews')"
      >
        <div class="flex gap-0.5">
          <svg v-for="i in stars.full" :key="'f' + i" class="w-4 h-4 text-yellow-400" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 1l1.8 3.6L14 5.3l-3 2.9.7 4.1L8 10.4l-3.7 1.9.7-4.1-3-2.9 4.2-.7z"/>
          </svg>
          <svg v-for="i in stars.empty" :key="'e' + i" class="w-4 h-4 text-[#3d4158]" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 1l1.8 3.6L14 5.3l-3 2.9.7 4.1L8 10.4l-3.7 1.9.7-4.1-3-2.9 4.2-.7z"/>
          </svg>
        </div>
        <span class="text-[#787d99] text-sm font-normal font-['Onest'] leading-5">{{ product.rating }}</span>
        <span class="text-orange-400 text-xs font-normal font-['Onest'] leading-4 hover:text-orange-300 transition-colors duration-150">
          ({{ product.reviewCount }} {{ DICT.product.reviews }})
        </span>
      </button>

      <div class="flex items-center gap-1.5">
        <span class="w-2 h-2 rounded-full" :class="product.inStock ? 'bg-green-400' : 'bg-red-400'" />
        <span class="text-xs font-normal font-['Onest'] leading-4" :class="product.inStock ? 'text-green-400' : 'text-red-400'">
          {{ product.inStock ? DICT.product.inStock : DICT.product.outOfStock }}
        </span>
      </div>
    </div>

    <!-- Об'єм пам'яті (статична заглушка) -->
    <div v-if="false" class="self-stretch pt-4 flex flex-col gap-2">
      <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] uppercase leading-4 tracking-wide">Об'єм пам'яті</span>
      <div class="inline-flex flex-wrap gap-2">
        <button
          v-for="opt in memoryOptions" :key="opt.label"
          class="px-4 py-2 rounded-xl outline outline-1 outline-offset-[-1px] text-sm font-normal font-['Onest'] leading-5 transition-all duration-150 active:scale-95"
          :class="selectedMemory === opt.label ? 'bg-orange-500/10 outline-orange-500 text-orange-500' : 'outline-[#2a2d3e] text-[#a0a5bf] hover:outline-[#3d4158] hover:text-gray-300'"
          @click="selectedMemory = opt.label"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- Колір (статична заглушка) -->
    <div v-if="false" class="self-stretch h-20 pt-3 flex flex-col gap-2">
      <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] uppercase leading-4 tracking-wide">
        Колір: <span class="text-[#a0a5bf] normal-case tracking-normal">{{ selectedColor.charAt(0) + selectedColor.slice(1).toLowerCase() }}</span>
      </span>
      <div class="inline-flex gap-3">
        <button
          v-for="col in colorOptions" :key="col.label" :title="col.label.charAt(0) + col.label.slice(1).toLowerCase()"
          class="w-8 h-8 rounded-full transition-all duration-150 hover:scale-110 active:scale-95 focus:outline-none"
          :style="{ backgroundColor: col.hex }"
          :class="selectedColor === col.label ? 'ring-2 ring-orange-500 ring-offset-2 ring-offset-[#111319]' : 'ring-2 ring-transparent ring-offset-2 ring-offset-[#111319]'"
          @click="selectedColor = col.label"
        />
      </div>
    </div>

    <!-- ─── БЛОК ГРУПОВОЇ ПОКУПКИ (рендериться тільки якщо доступна) ─── -->
    <template v-if="product.isGroupBuyAvailable">
      <div class="self-stretch px-5 py-8 bg-gradient-to-br from-[#1a0f05] via-[#1c1209] to-[#160d04] rounded-2xl outline outline-2 outline-offset-[-2px] outline-orange-500 flex flex-col gap-2">

        <div class="self-stretch inline-flex justify-between items-center">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 bg-orange-500 rounded-lg flex justify-center items-center shrink-0">
              <svg class="w-4 h-4 text-white" viewBox="0 0 20 20" fill="currentColor">
                <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zm5 2a2 2 0 11-4 0 2 2 0 014 0zm-4 7a4 4 0 00-8 0v1h8v-1zM2 8a2 2 0 114 0 2 2 0 01-4 0zm13 7v-1a6 6 0 00-1.05-3.38A4 4 0 0118 15h1v1h-4zm-10-1v1H1v-1h1a4 4 0 013.05-3.88A6 6 0 005 14z"/>
              </svg>
            </div>
            <span class="text-orange-300 text-base font-normal font-['Onest'] leading-6">
              {{ sessionData ? DICT.product.groupBuyInvitation : DICT.product.groupBuy }}
            </span>
          </div>
        <div class="px-3 py-1.5 bg-gradient-to-r from-orange-500/20 to-amber-500/10 rounded-lg outline outline-1 outline-offset-[-1px] outline-orange-500/40">
            <span class="text-orange-300 text-sm font-semibold font-['Onest'] leading-5">{{ DICT.product.savings }} {{ fmt(product.savings) }}</span>
          </div>
        </div>

        <div class="w-full overflow-hidden flex flex-wrap items-baseline gap-x-3 gap-y-1 py-1">
          <span class="text-orange-400 text-3xl md:text-4xl font-normal font-['Unbounded'] leading-tight">{{ fmt(product.groupPrice) }}</span>
          <div class="flex items-center gap-2">
            <span class="text-[#5a5f7a] text-lg md:text-xl font-normal font-['Onest'] line-through leading-tight">{{ fmt(product.standardPrice) }}</span>
            <span class="px-2 py-0.5 bg-red-600/80 rounded-lg text-white text-xs md:text-sm font-normal font-['Onest']">−{{ product.discount }}%</span>
          </div>
        </div>

        <p class="self-stretch text-sm font-normal font-['Onest'] leading-6">
          <span class="text-orange-200/70">Збери групу з </span>
          <span class="text-orange-300 font-bold">{{ sessionData ? sessionData.targetSize : product.groupMinPeople }} людей</span>
          <span class="text-orange-200/70"> за {{ product.groupHours }} години та отримай знижку на весь порядок!</span>
        </p>

        <div v-if="sessionData" class="self-stretch pt-2 inline-flex items-center gap-2">
          <svg class="w-4 h-4 text-orange-400 shrink-0" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="10" cy="10" r="8"/><path d="M10 6v4l2.5 2.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-[#787d99] text-xs font-normal font-['Onest'] leading-4">До кінця збору:</span>
          <span
            class="text-sm font-normal font-['Unbounded'] leading-5 tabular-nums"
            :class="countdownFormatted === DICT.product.timeExpired ? 'text-red-400' : 'text-white'"
          >{{ countdownFormatted }}</span>
        </div>

        <div class="self-stretch py-2 flex flex-col gap-1.5">
          <div class="self-stretch inline-flex justify-between items-center pb-0.5">
            <span class="text-[#787d99] text-xs font-normal font-['Onest'] leading-4">Учасники групи</span>
            <span class="text-orange-300 text-xs font-normal font-['Onest'] leading-4">
              {{ sessionData ? localMembersCount : product.groupCurrent }} / {{ sessionData ? sessionData.targetSize : product.groupTotal }} учасників
            </span>
          </div>
          <div class="self-stretch h-2 relative bg-[#1c1f2a]/80 rounded-full overflow-hidden">
            <div class="h-2 absolute left-0 top-0 bg-gradient-to-r from-orange-500 to-amber-400 rounded-full transition-all duration-500" :style="{ width: groupPercent + '%' }" />
          </div>
          <div class="self-stretch inline-flex justify-between items-start">
            <span
              v-for="step in (sessionData ? sessionData.targetSize : product.groupTotal) + 1" :key="step"
              class="text-xs font-normal font-['Onest'] leading-4"
              :class="step - 1 === (sessionData ? sessionData.targetSize : product.groupTotal) ? 'text-green-400' : 'text-[#3d4158]'"
            >
              {{ step - 1 }}{{ step - 1 === (sessionData ? sessionData.targetSize : product.groupTotal) ? ' ✓' : '' }}
            </span>
          </div>
        </div>

        <template v-if="sessionData">
          <div v-if="isExpired" class="w-full h-14 py-4 bg-[#2a2d3e] rounded-xl text-[#787d99] text-[11px] sm:text-xs md:text-base font-normal font-['Unbounded'] leading-6 tracking-wide text-center flex items-center justify-center cursor-not-allowed">
            ЗБІР ЗАВЕРШЕНО (ЧАС ВИЧЕРПАНО)
          </div>

          <div v-else-if="joinSuccess" class="w-full h-14 py-4 bg-green-600 rounded-xl text-white text-[11px] sm:text-xs md:text-base font-normal font-['Unbounded'] leading-6 tracking-wide text-center flex items-center justify-center">
            ВИ ВЖЕ БЕРЕТЕ УЧАСТЬ ✓
          </div>

          <div v-else-if="isGroupFull" class="w-full h-14 py-4 bg-[#2a2d3e] rounded-xl text-[#787d99] text-[11px] sm:text-xs md:text-base font-normal font-['Unbounded'] leading-6 tracking-wide text-center flex items-center justify-center cursor-not-allowed">
            ЗБІР ЗАВЕРШЕНО (ГРУПА ПОВНА)
          </div>

          <button
            v-else
            class="w-full h-14 py-4 bg-gradient-to-r from-orange-500 to-amber-500 rounded-xl text-white text-[11px] sm:text-xs md:text-base font-semibold font-['Unbounded'] leading-6 tracking-wide uppercase transition-all duration-200 hover:from-orange-400 hover:to-amber-400 hover:shadow-[0px_4px_28px_0px_rgba(249,115,22,0.6)] active:scale-[0.99] focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 focus:ring-offset-[#160d04] disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="groupBuyStore.isLoading"
            @click="handleJoin"
          >
            {{ groupBuyStore.isLoading ? 'ПРИЄДНУЄМОСЯ...' : DICT.actions.join }}
          </button>
        </template>

        <template v-else>
          <button
            class="w-full h-14 py-4 bg-gradient-to-r from-orange-500 to-amber-500 rounded-xl text-white text-[11px] sm:text-xs md:text-base font-semibold font-['Unbounded'] leading-6 tracking-wide uppercase transition-all duration-200 hover:from-orange-400 hover:to-amber-400 hover:shadow-[0px_4px_28px_0px_rgba(249,115,22,0.6)] active:scale-[0.99] focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 focus:ring-offset-[#160d04]"
            @click="emit('start-group')"
          >
            ПОЧАТИ ГРУПОВУ ПОКУПКУ
          </button>
        </template>
      </div>
    </template>

    <!-- Кнопка стандартної ціни під блоком групової покупки -->
    <button
      v-if="product.isGroupBuyAvailable"
      class="self-stretch py-3 rounded-xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] text-[#787d99] text-sm font-normal font-['Onest'] leading-5 text-center transition-all duration-150 hover:outline-[#3d4158] hover:text-gray-400 hover:bg-white/5 active:scale-[0.99] focus:outline-none focus:ring-2 focus:ring-white/10 disabled:opacity-50 disabled:cursor-not-allowed"
      :disabled="cartStore.isLoading"
      @click="handleAddToCart"
    >
      {{ cartStore.isLoading ? 'Додаємо...' : `${DICT.product.buyStandard} (${fmt(product.standardPrice)})` }}
    </button>

    <!-- Блок ціни для звичайного товару (без групової покупки) -->
    <div v-if="!product.isGroupBuyAvailable" class="self-stretch mt-4 px-5 py-6 bg-[#161820] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] flex flex-col gap-4">
      <span class="text-white text-3xl md:text-4xl font-semibold font-['Unbounded'] leading-tight">{{ fmt(product.standardPrice) }}</span>
      <button
        class="w-full py-4 bg-gradient-to-r from-orange-500 to-amber-500 rounded-xl text-white text-base font-semibold font-['Unbounded'] leading-6 text-center uppercase tracking-wide transition-all duration-200 hover:from-orange-400 hover:to-amber-400 hover:shadow-[0px_6px_32px_0px_rgba(249,115,22,0.55)] active:scale-[0.99] focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 focus:ring-offset-[#0f1117] disabled:opacity-50 disabled:cursor-not-allowed"
        :disabled="cartStore.isLoading"
        @click="handleAddToCart"
      >
        {{ cartStore.isLoading ? 'Додаємо...' : DICT.actions.buy }}
      </button>
    </div>

    <div class="self-stretch pt-5 rounded-xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] flex flex-col overflow-hidden">
      <div class="self-stretch px-4 py-3 bg-[#161820] border-b border-[#1c1f2a]">
        <span class="text-[#787d99] text-xs font-normal font-['Onest'] uppercase leading-4 tracking-wide">{{ DICT.product.deliveryAndWarranty }}</span>
      </div>

      <div class="self-stretch flex flex-col">
        <div v-for="(row, i) in deliveryRows" :key="row.title" class="self-stretch px-4 py-3 inline-flex justify-start items-center gap-3 transition-colors duration-150 hover:bg-white/[0.03]" :class="i > 0 ? 'border-t border-[#1c1f2a]/60' : ''">
          <div class="w-8 h-8 bg-[#1c1f2a] rounded-lg flex justify-center items-center shrink-0">
            <svg v-if="row.icon === 'truck'" class="w-4 h-4 text-orange-400" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M1 4h11v9H1zm10 0h3l3 4v5h-6V4z" stroke-linecap="round" stroke-linejoin="round"/><circle cx="4.5" cy="14.5" r="1.5"/><circle cx="13.5" cy="14.5" r="1.5"/></svg>
            <svg v-else-if="row.icon === 'home'" class="w-4 h-4 text-orange-400" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M3 9.5L10 3l7 6.5V17a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z" stroke-linecap="round" stroke-linejoin="round"/><path d="M7 18v-7h6v7" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <svg v-else-if="row.icon === 'shield'" class="w-4 h-4 text-orange-400" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M10 2l7 2.8v5.7C17 14 14 17.4 10 18c-4-0.6-7-4-7-7.5V4.8L10 2z" stroke-linecap="round" stroke-linejoin="round"/><path d="M7 10l2 2 4-4" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <svg v-else-if="row.icon === 'store'" class="w-4 h-4 text-orange-400" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M2 7l1.5-4h13L18 7H2z" stroke-linecap="round" stroke-linejoin="round"/><path d="M3 7v10a1 1 0 001 1h12a1 1 0 001-1V7" stroke-linecap="round"/><path d="M8 17v-5h4v5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="flex flex-col gap-0.5 min-w-0">
            <span class="text-[#c8ccdf] text-sm font-normal font-['Onest'] leading-5">{{ row.title }}</span>
            <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">{{ row.subtitle }}</span>
          </div>
          <span class="ml-auto shrink-0 text-xs font-normal font-['Onest'] leading-4" :class="row.badgeGreen ? 'text-green-400' : 'text-[#787d99]'">{{ row.badge }}</span>
        </div>
      </div>
    </div>

    <!-- Toast: товар додано до кошика -->
    <Transition name="fade">
      <div v-if="showCartToast" class="fixed bottom-8 left-1/2 -translate-x-1/2 z-50 flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-emerald-600 to-emerald-700 rounded-xl shadow-[0_10px_30px_rgba(16,185,129,0.3)] text-white text-sm font-semibold font-['Onest'] tracking-wide">
        <svg class="w-5 h-5" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
        Товар додано до кошика!
      </div>
    </Transition>

  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translate(-50%, 20px); }
</style>
