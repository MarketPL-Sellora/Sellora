<script setup lang="ts">
import { ref, computed } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface GroupBuy {
  id: number
  emoji: string
  name: string
  sku: string
  seller: string
  price: number
  oldPrice: number
  discount: number
  status: string
  membersCurrent: number
  membersTotal: number
  userInitials: string
  inviteLink: string
  countdown: string
}

// ─── Data ─────────────────────────────────────────────────────────────────────

const activeBuys = ref<GroupBuy[]>([
  {
    id:             1,
    emoji:          '💻',
    name:           'Ноутбук AeroBook Pro 14',
    sku:            '#NB-2847',
    seller:         'TechZone UA',
    price:          34999,
    oldPrice:       39999,
    discount:       13,
    status:         'Очікування учасників',
    membersCurrent: 1,
    membersTotal:   3,
    userInitials:   'МК',
    inviteLink:     'sellora.com/group/nb-2847',
    countdown:      '14:21:44',
  },
  {
    id:             2,
    emoji:          '🎧',
    name:           'Навушники SoundCore Elite X5',
    sku:            '#HP-5521',
    seller:         'AudioHub',
    price:          4299,
    oldPrice:       5499,
    discount:       22,
    status:         'Очікування учасників',
    membersCurrent: 1,
    membersTotal:   3,
    userInitials:   'МК',
    inviteLink:     'sellora.com/group/hp-5521',
    countdown:      '07:45:07',
  },
])

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmt = (n: number) => '₴' + n.toLocaleString('uk-UA')

function progressPercent(buy: GroupBuy): number {
  return Math.round((buy.membersCurrent / buy.membersTotal) * 100)
}

function emptySlots(buy: GroupBuy): number[] {
  return Array.from({ length: buy.membersTotal - buy.membersCurrent }, (_, i) => i)
}

// ─── Clipboard ───────────────────────────────────────────────────────────────

const copiedId = ref<number | null>(null)

async function copyLink(buy: GroupBuy) {
  try {
    await navigator.clipboard.writeText('https://' + buy.inviteLink)
  } catch {
    const el = document.createElement('input')
    el.value = buy.inviteLink
    document.body.appendChild(el)
    el.select()
    document.execCommand('copy')
    document.body.removeChild(el)
  }
  copiedId.value = buy.id
  setTimeout(() => (copiedId.value = null), 2000)
}

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'cancel', id: number): void
}>()

function cancelBuy(id: number) {
  if (confirm('Скасувати участь у груповій покупці?')) {
    activeBuys.value = activeBuys.value.filter(b => b.id !== id)
    emit('cancel', id)
  }
}
</script>

<template>
  <div class="w-full inline-flex flex-col gap-4">

    <!-- ── Header ─────────────────────────────────────────────────────────── -->
    <div class="self-stretch inline-flex justify-between items-start flex-wrap gap-3">
      <div class="flex flex-col gap-[2.4px]">
        <div class="inline-flex gap-0 flex-wrap">
          <span class="text-gray-100 text-2xl font-black font-['Unbounded'] leading-8">Активні збори&nbsp;</span>
          <span class="text-gray-600 text-2xl font-normal font-['Unbounded'] leading-8">/ Групові покупки</span>
        </div>
        <span class="text-gray-600 text-sm font-normal font-['Onest'] leading-5">
          Запросіть друзів та отримайте знижку разом
        </span>
      </div>

      <!-- Active count badge -->
      <div class="pt-1 shrink-0">
        <div class="px-2.5 py-[3px] bg-yellow-500/10 rounded-full outline outline-1 outline-offset-[-1px] outline-yellow-500/20 inline-flex items-center gap-1.5">
          <span class="w-1.5 h-1.5 bg-yellow-400 rounded-sm shrink-0" />
          <span class="text-yellow-400 text-xs font-normal font-['Onest'] leading-4">
            {{ activeBuys.length }} активних
          </span>
        </div>
      </div>
    </div>

    <!-- ── Cards ──────────────────────────────────────────────────────────── -->
    <div
      v-for="buy in activeBuys"
      :key="buy.id"
      class="self-stretch px-6 pt-7 pb-6 bg-gray-900 rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] flex flex-col gap-2 transition-all duration-200 hover:outline-[#2a3a52]"
    >
      <!-- Top row: image + meta -->
      <div class="self-stretch pb-2 inline-flex justify-start items-start gap-4">
        <!-- Emoji thumbnail -->
        <div class="w-16 h-16 shrink-0 bg-[#1a2235] rounded-[10.4px] outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] flex justify-center items-center">
          <span class="text-3xl leading-[48px]">{{ buy.emoji }}</span>
        </div>

        <!-- Info -->
        <div class="flex-1 min-w-0 flex flex-col gap-2.5">
          <!-- Name row -->
          <div class="self-stretch inline-flex justify-between items-start gap-2 flex-wrap">
            <div class="flex flex-col gap-[2.6px] min-w-0">
              <span class="text-gray-100 text-base font-bold font-['Unbounded'] leading-5 truncate">
                {{ buy.name }}
              </span>
              <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">
                Арт. {{ buy.sku }} · Продавець: {{ buy.seller }}
              </span>
            </div>
            <!-- Status badge -->
            <div class="shrink-0 px-2.5 py-[3px] bg-yellow-500/10 rounded-full outline outline-1 outline-offset-[-1px] outline-yellow-500/20 flex items-center gap-1.5">
              <span class="w-1.5 h-1.5 bg-yellow-400 rounded-sm shrink-0" />
              <span class="text-yellow-400 text-xs font-normal font-['Onest'] leading-4">{{ buy.status }}</span>
            </div>
          </div>

          <!-- Prices -->
          <div class="inline-flex items-center gap-2.5 flex-wrap">
            <span class="text-gray-100 text-lg font-extrabold font-['Unbounded'] leading-7">
              {{ fmt(buy.price) }}
            </span>
            <span class="text-gray-700 text-xs font-normal font-['Onest'] line-through leading-4">
              {{ fmt(buy.oldPrice) }}
            </span>
            <div class="px-2 py-0.5 bg-orange-500/10 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/20">
              <span class="text-orange-500 text-xs font-normal font-['Onest'] leading-4">−{{ buy.discount }}%</span>
            </div>
          </div>
        </div>
      </div>

      <div class="self-stretch h-px bg-[#1e2d3d]" />

      <!-- Members count -->
      <div class="self-stretch pt-2 inline-flex justify-between items-center">
        <span class="text-gray-400 text-sm font-normal font-['Onest'] leading-5">Зібрано учасників</span>
        <span class="text-sm font-['Onest'] leading-5">
          <span class="text-slate-200 font-medium">{{ buy.membersCurrent }}</span>
          <span class="text-gray-700"> з </span>
          <span class="text-slate-200 font-medium">{{ buy.membersTotal }}</span>
        </span>
      </div>

      <!-- Progress bar -->
      <div class="self-stretch h-[5px] relative bg-[#1a2235] rounded-full overflow-hidden">
        <div
          class="h-[5px] absolute left-0 top-0 bg-gradient-to-r from-orange-500 to-orange-400 rounded-full transition-all duration-500"
          :style="{ width: progressPercent(buy) + '%' }"
        />
      </div>

      <!-- Avatars + countdown -->
      <div class="self-stretch pt-1.5 inline-flex justify-start items-center gap-2.5 flex-wrap">
        <!-- Owner avatar -->
        <div class="w-9 h-9 p-[0.6px] bg-gradient-to-br from-orange-500 to-[#ea6c0a] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#0d1117] flex justify-center items-center shrink-0">
          <span class="text-white text-[10.4px] font-bold font-['Unbounded'] leading-4">
            {{ buy.userInitials }}
          </span>
        </div>

        <!-- Empty slots -->
        <div
          v-for="i in emptySlots(buy)"
          :key="i"
          class="w-9 h-9 p-[0.6px] bg-[#1a2235] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#2a3a52] flex justify-center items-center shrink-0 transition-colors duration-150 hover:outline-orange-500/30 hover:bg-[#1e2d3d] cursor-pointer"
        >
          <span class="text-gray-600 text-lg font-normal font-['Onest'] leading-7">+</span>
        </div>

        <!-- Empty slots label -->
        <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">
          Ще {{ emptySlots(buy).length }} місця
        </span>

        <!-- Countdown -->
        <div class="ml-auto shrink-0 px-3 py-1.5 bg-[#0d1117] rounded-[9.6px] outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] inline-flex items-center gap-2">
          <!-- Clock icon -->
          <svg class="w-3 h-3 text-gray-400 shrink-0" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.1">
            <circle cx="6" cy="6" r="4.5"/>
            <path d="M6 3.5v2.5l1.5 1" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">До скасування:</span>
          <span class="text-slate-200 text-sm font-normal font-['Unbounded'] leading-5 tracking-wide tabular-nums">
            {{ buy.countdown }}
          </span>
        </div>
      </div>

      <!-- Action buttons -->
      <div class="self-stretch pt-2 inline-flex justify-start items-start gap-2.5 flex-wrap">
        <!-- Copy link -->
        <button
          class="flex-1 min-w-[180px] px-5 py-2.5 bg-orange-500 rounded-[10.4px] flex justify-center items-center gap-2 transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] active:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-orange-500/40"
          @click="copyLink(buy)"
        >
          <!-- Copy / check icon -->
          <svg v-if="copiedId !== buy.id" class="w-3.5 h-3.5 text-white shrink-0" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.2">
            <rect x="4.5" y="4.5" width="7" height="8" rx="1.2"/>
            <path d="M4.5 9H3a1 1 0 01-1-1V3a1 1 0 011-1h5a1 1 0 011 1v1.5" stroke-linecap="round"/>
          </svg>
          <svg v-else class="w-3.5 h-3.5 text-white shrink-0" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M2 7l4 4 6-6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-white text-sm font-bold font-['Onest'] leading-5 whitespace-nowrap">
            {{ copiedId === buy.id ? 'Скопійовано!' : 'Скопіювати посилання для друзів' }}
          </span>
        </button>

        <!-- Cancel button -->
        <button
          class="w-40 h-11 shrink-0 px-4 bg-[#1a2235] rounded-[10.4px] outline outline-1 outline-offset-[-1px] outline-[#2a3a52] inline-flex justify-center items-center gap-2 transition-all duration-150 hover:bg-red-500/10 hover:outline-red-500/50 hover:text-red-400 active:scale-[0.98] focus:outline-none focus:ring-2 focus:ring-red-500/30 group"
          @click="cancelBuy(buy.id)"
        >
          <!-- X circle icon -->
          <svg class="w-3 h-3 shrink-0 text-gray-400 group-hover:text-red-400 transition-colors duration-150" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.2">
            <circle cx="6" cy="6" r="4.5"/>
            <path d="M4 4l4 4M8 4l-4 4" stroke-linecap="round"/>
          </svg>
          <span class="text-gray-400 group-hover:text-red-400 text-sm font-normal font-['Onest'] leading-5 transition-colors duration-150">
            Скасувати участь
          </span>
        </button>
      </div>
    </div>

    <!-- ── Empty state ─────────────────────────────────────────────────────── -->
    <div
      v-if="activeBuys.length === 0"
      class="self-stretch py-16 bg-gray-900 rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] flex flex-col items-center justify-center gap-3"
    >
      <svg class="w-12 h-12 text-gray-700" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="20" cy="20" r="12"/>
        <path d="M32 32l8 8" stroke-linecap="round"/>
        <path d="M20 15v5l3 3" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span class="text-gray-600 text-sm font-normal font-['Onest'] leading-5">
        Активних групових покупок немає
      </span>
    </div>

  </div>
</template>
