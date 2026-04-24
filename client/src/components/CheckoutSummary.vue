<script setup lang="ts">
import { ref, computed } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface OrderItem {
  id: number
  name: string
  image: string
  badge: string | null
  specs: string
  qty: number
  price: number
}

interface PriceLine {
  label: string
  value: string
  green?: boolean
}

interface TrustBadge {
  icon: 'shield' | 'lock' | 'return'
  label: string
}

// ─── Order data (replace with props later) ───────────────────────────────────

const item = ref<OrderItem>({
  id:    1,
  name:  'Ноутбук ASUS ProArt 16"',
  image: '../assets/product-placeholder.png',
  badge: 'Групова покупка',
  specs: 'OLED · 32 ГБ · 1 ТБ SSD',
  qty:   1,
  price: 25000,
})

// ─── Price breakdown ──────────────────────────────────────────────────────────

const priceLines = ref<PriceLine[]>([
  { label: 'Вартість товарів',  value: '26 500 ₴'  },
  { label: 'Вартість доставки', value: '120 ₴'     },
  { label: 'Знижка (груп.)',    value: '− 1 620 ₴', green: true },
])

const total = ref('25 000 ₴')

// ─── Promo code ───────────────────────────────────────────────────────────────

const promoCode     = ref('')
const promoApplied  = ref(false)
const promoError    = ref(false)

function applyPromo() {
  if (!promoCode.value.trim()) return
  // Mock: accept SELLORA10 as valid
  if (promoCode.value.trim().toUpperCase() === 'SELLORA10') {
    promoApplied.value = true
    promoError.value   = false
  } else {
    promoApplied.value = false
    promoError.value   = true
    setTimeout(() => (promoError.value = false), 2000)
  }
}

// ─── Trust badges ─────────────────────────────────────────────────────────────

const trustBadges: TrustBadge[] = [
  { icon: 'shield', label: 'Захист покупця'    },
  { icon: 'lock',   label: 'SSL-шифрування'    },
  { icon: 'return', label: 'Повернення 30 днів' },
]

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'pay'): void
}>()
</script>

<template>
  <div class="w-full inline-flex flex-col justify-start items-start">
    <div class="self-stretch p-6 bg-gray-800 rounded-2xl flex flex-col gap-3">

      <!-- ── Heading ─────────────────────────────────────────────────────── -->
      <h2 class="text-white text-base font-bold font-['Unbounded'] leading-6 tracking-widest">
        ВАШЕ ЗАМОВЛЕННЯ
      </h2>

      <!-- ── Product row ─────────────────────────────────────────────────── -->
      <div class="self-stretch pt-2 pb-1 inline-flex justify-start items-start gap-3">
        <!-- Thumbnail -->
        <div class="w-16 h-16 shrink-0 bg-gray-700 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 flex justify-center items-center overflow-hidden">
          <img
            :src="item.image"
            :alt="item.name"
            class="w-full h-full object-contain p-1"
            loading="lazy"
          />
        </div>

        <!-- Info -->
        <div class="flex-1 self-stretch inline-flex flex-col justify-center gap-1">
          <div class="flex flex-col gap-2">
            <span class="text-gray-200 text-sm font-semibold font-['Onest'] leading-4">
              {{ item.name }}
            </span>
            <span
              v-if="item.badge"
              class="self-start px-2 py-[3px] bg-gradient-to-b from-orange-500 to-[#ea6c0a] rounded-full text-white text-[10px] font-bold font-['Unbounded'] uppercase leading-4 tracking-wide"
            >
              {{ item.badge }}
            </span>
          </div>
          <span class="text-gray-500 text-xs font-normal font-['Onest'] leading-4">
            {{ item.specs }}
          </span>
          <div class="self-stretch pt-1 flex flex-wrap justify-between items-end gap-2">
            <span class="text-gray-500 text-xs font-normal font-['Onest'] leading-4">
              Кількість: {{ item.qty }}
            </span>
            <span class="text-white text-sm font-bold font-['Onest'] leading-5 whitespace-nowrap">
              {{ item.price.toLocaleString('uk-UA') }} ₴
            </span>
          </div>
        </div>
      </div>

      <div class="self-stretch h-px border-t border-gray-700" />

      <!-- ── Promo code ──────────────────────────────────────────────────── -->
      <div class="self-stretch pb-1 inline-flex justify-start items-stretch gap-2">
        <div class="flex-1 relative">
          <input
            v-model="promoCode"
            type="text"
            placeholder="Промокод"
            maxlength="24"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-2"
            :class="
              promoApplied ? 'outline-green-500/60 focus:ring-green-500/30'
              : promoError ? 'outline-red-500/60 focus:ring-red-500/30 animate-shake'
              : 'outline-gray-700 focus:outline-orange-500 focus:ring-orange-500/30'
            "
            @keydown.enter="applyPromo"
          />
          <!-- Tick if applied -->
          <svg
            v-if="promoApplied"
            class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-green-400"
            viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5"
          >
            <path d="M3 8l4 4 6-6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <button
          class="h-10 px-4 rounded-lg text-sm font-medium font-['Onest'] leading-5 transition-all duration-150 active:scale-95 focus:outline-none"
          :class="
            promoApplied
              ? 'bg-green-500/20 text-green-400 outline outline-1 outline-green-500/30 cursor-default'
              : 'bg-gray-700 text-gray-300 hover:bg-gray-600 hover:text-white active:bg-gray-700'
          "
          :disabled="promoApplied"
          @click="applyPromo"
        >
          {{ promoApplied ? 'Застосовано ✓' : 'Застосувати' }}
        </button>
      </div>

      <div class="self-stretch h-px border-t border-gray-700" />

      <!-- ── Price breakdown ────────────────────────────────────────────── -->
      <div class="self-stretch pb-1 flex flex-col gap-2">
        <div
          v-for="line in priceLines"
          :key="line.label"
          class="self-stretch inline-flex justify-between items-start"
        >
          <span class="text-gray-400 text-sm font-normal font-['Onest'] leading-5">
            {{ line.label }}
          </span>
          <span
            class="text-sm font-['Onest'] leading-5"
            :class="line.green ? 'text-green-400 font-semibold' : 'text-gray-200 font-normal'"
          >
            {{ line.value }}
          </span>
        </div>
      </div>

      <div class="self-stretch h-px border-t border-gray-700" />

      <!-- ── Total ───────────────────────────────────────────────────────── -->
      <div class="self-stretch flex flex-wrap justify-between items-center gap-2">
        <span class="text-gray-300 text-xs md:text-sm font-bold font-['Unbounded'] leading-5 tracking-tight">
          ДО СПЛАТИ
        </span>
        <span class="text-white text-xl md:text-2xl font-extrabold font-['Unbounded'] leading-8 whitespace-nowrap">
          {{ total }}
        </span>
      </div>

      <!-- ── Status banner ──────────────────────────────────────────────── -->
      <div class="self-stretch px-4 pt-4 pb-5 bg-orange-500/10 rounded-xl outline outline-1 outline-offset-[-1px] outline-orange-500/30 flex flex-col gap-0">
        <div class="inline-flex justify-start items-start gap-2">
          <!-- Warning icon -->
          <svg class="w-4 h-4 mt-0.5 shrink-0 text-orange-400" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M8 2L1.5 13.5h13L8 2z" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M8 6.5v3M8 11v.5" stroke-linecap="round"/>
          </svg>
          <div class="flex flex-col gap-[3px]">
            <span class="text-orange-400 text-sm font-bold font-['Onest'] leading-5">
              Статус: Очікуємо ще 2 учасників
            </span>
            <p class="text-orange-300 text-xs font-normal font-['Onest'] leading-5 opacity-90">
              Якщо група не збереться за
              <span class="font-bold">24 год</span>,
              замовлення буде автоматично скасовано, а кошти повернуті.
            </p>
          </div>
        </div>
      </div>

      <!-- ── Pay button ─────────────────────────────────────────────────── -->
      <button
        class="self-stretch px-6 py-4 bg-gradient-to-b from-orange-500 to-[#ea6c0a] rounded-xl shadow-[0px_4px_20px_0px_rgba(249,115,22,0.35)] inline-flex justify-center items-center transition-all duration-150 hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_6px_28px_0px_rgba(249,115,22,0.55)] active:scale-[0.98] active:shadow-none focus:outline-none focus:ring-2 focus:ring-orange-500/50 focus:ring-offset-2 focus:ring-offset-gray-800"
        @click="emit('pay')"
      >
        <span class="text-white text-base font-bold font-['Unbounded'] leading-6 tracking-wide">
          Оплатити та відкрити збір &nbsp;➔
        </span>
      </button>

      <!-- ── Trust badges ───────────────────────────────────────────────── -->
      <div class="self-stretch pt-1 inline-flex justify-between items-start">
        <div
          v-for="badge in trustBadges"
          :key="badge.label"
          class="flex-1 inline-flex flex-col items-center gap-1"
        >
          <div class="w-8 h-8 bg-orange-500/10 rounded-2xl outline outline-1 outline-offset-[-1px] outline-orange-500/20 flex justify-center items-center">
            <!-- Shield -->
            <svg v-if="badge.icon === 'shield'" class="w-4 h-4 text-gray-200" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M8 1.5l5.5 2v4.7C13.5 11.5 11 14 8 14.5 5 14 2.5 11.5 2.5 8.2V3.5L8 1.5z" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M5.5 8l2 2 3.5-3.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- Lock -->
            <svg v-else-if="badge.icon === 'lock'" class="w-4 h-4 text-gray-200" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
              <rect x="3" y="7" width="10" height="7" rx="1.5"/>
              <path d="M5.5 7V5a2.5 2.5 0 015 0v2" stroke-linecap="round"/>
              <circle cx="8" cy="10.5" r="1" fill="currentColor" stroke="none"/>
            </svg>
            <!-- Return arrow -->
            <svg v-else-if="badge.icon === 'return'" class="w-4 h-4 text-gray-200" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M2.5 8A5.5 5.5 0 108 2.5H5.5" stroke-linecap="round"/>
              <path d="M5.5 2.5L3 5l2.5 2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <span class="text-center text-gray-500 text-xs font-normal font-['Onest'] leading-4">
            {{ badge.label }}
          </span>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%       { transform: translateX(-4px); }
  40%       { transform: translateX(4px); }
  60%       { transform: translateX(-3px); }
  80%       { transform: translateX(3px); }
}
.animate-shake { animation: shake 0.35s ease; }
</style>
