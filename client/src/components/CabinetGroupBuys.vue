<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { useGroupBuyStore } from '../state/groupBuyStore'
import type { GroupBuySession } from '../state/groupBuyStore'

// ─── Store ────────────────────────────────────────────────────────────────────

const groupBuyStore = useGroupBuyStore()
const router = useRouter()

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmt = (n: number) => '₴' + n.toLocaleString('uk-UA')

const statusMap: Record<string, string> = {
  ACTIVE:    'Очікування учасників',
  PENDING:   'Очікування учасників',
  COMPLETED: 'Завершено',
  EXPIRED:   'Час вичерпано',
  CANCELLED: 'Скасовано',
}

function localizeStatus(status: string): string {
  return statusMap[status] || status
}

function statusColor(status: string): { bg: string; outline: string; dot: string; text: string } {
  switch (status) {
    case 'COMPLETED':
      return { bg: 'bg-green-500/10', outline: 'outline-green-500/20', dot: 'bg-green-400', text: 'text-green-400' }
    case 'EXPIRED':
    case 'CANCELLED':
      return { bg: 'bg-red-500/10', outline: 'outline-red-500/20', dot: 'bg-red-400', text: 'text-red-400' }
    default:
      return { bg: 'bg-yellow-500/10', outline: 'outline-yellow-500/20', dot: 'bg-yellow-400', text: 'text-yellow-400' }
  }
}

// ─── Countdown timers ─────────────────────────────────────────────────────────

const countdowns = ref<Record<string, string>>({})

function updateCountdowns() {
  const now = dayjs()
  for (const s of groupBuyStore.mySessions) {
    const end = dayjs(s.expiresAt)
    const diff = end.diff(now, 'second')
    if (diff <= 0) {
      countdowns.value[s.uuid] = 'Час вичерпано'
    } else {
      const h = Math.floor(diff / 3600)
      const m = Math.floor((diff % 3600) / 60)
      const sec = diff % 60
      const pad = (n: number) => String(n).padStart(2, '0')
      countdowns.value[s.uuid] = `${pad(h)}:${pad(m)}:${pad(sec)}`
    }
  }
}

let timerInterval: ReturnType<typeof setInterval>

onMounted(async () => {
  await groupBuyStore.fetchMySessions()
  updateCountdowns()
  timerInterval = setInterval(updateCountdowns, 1000)
})

onUnmounted(() => clearInterval(timerInterval))

// ─── Mapped sessions ─────────────────────────────────────────────────────────

interface MappedSession {
  uuid: string
  productId: number
  productImage: string
  name: string
  status: string
  rawStatus: string
  price: number
  membersCurrent: number
  membersTotal: number
  inviteLink: string
  countdown: string
}

const mappedSessions = computed<MappedSession[]>(() =>
  groupBuyStore.mySessions.map((s) => ({
    uuid: s.uuid,
    productId: s.productId,
    productImage: s.productImage,
    name: s.productTitle,
    status: localizeStatus(s.status),
    rawStatus: s.status,
    price: s.price,
    membersCurrent: s.currentMembersCount,
    membersTotal: s.targetSize,
    inviteLink: window.location.origin + '/product/' + s.productId + '?session=' + s.uuid,
    countdown: countdowns.value[s.uuid] || '—',
  }))
)

// ─── Progress helpers ─────────────────────────────────────────────────────────

function progressPercent(buy: MappedSession): number {
  return Math.round((buy.membersCurrent / buy.membersTotal) * 100)
}

function emptySlots(buy: MappedSession): number[] {
  const count = buy.membersTotal - buy.membersCurrent
  return count > 0 ? Array.from({ length: count }, (_, i) => i) : []
}

// ─── Clipboard ───────────────────────────────────────────────────────────────

const copiedId = ref<string | null>(null)

async function copyLink(buy: MappedSession) {
  try {
    await navigator.clipboard.writeText(buy.inviteLink)
  } catch {
    const el = document.createElement('input')
    el.value = buy.inviteLink
    document.body.appendChild(el)
    el.select()
    document.execCommand('copy')
    document.body.removeChild(el)
  }
  copiedId.value = buy.uuid
  setTimeout(() => (copiedId.value = null), 2000)
}

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'cancel', uuid: string): void
}>()

function cancelBuy(uuid: string) {
  if (confirm('Скасувати участь у груповій покупці?')) {
    emit('cancel', uuid)
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
            {{ mappedSessions.length }} активних
          </span>
        </div>
      </div>
    </div>

    <!-- ── Loading state ───────────────────────────────────────────────── -->
    <div
      v-if="groupBuyStore.isLoading && mappedSessions.length === 0"
      class="self-stretch py-16 bg-gray-900 rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] flex flex-col items-center justify-center gap-3"
    >
      <svg class="w-8 h-8 text-orange-400 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 2a10 10 0 0110 10" stroke-linecap="round"/>
      </svg>
      <span class="text-gray-500 text-sm font-normal font-['Onest'] leading-5">
        Завантаження...
      </span>
    </div>

    <!-- ── Cards ──────────────────────────────────────────────────────────── -->
    <div
      v-for="buy in mappedSessions"
      :key="buy.uuid"
      class="self-stretch px-6 pt-7 pb-6 bg-gray-900 rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] flex flex-col gap-2 transition-all duration-200 hover:outline-[#2a3a52]"
    >
      <!-- Top row: image + meta -->
      <div class="self-stretch pb-2 inline-flex justify-start items-start gap-4">
        <!-- Product image thumbnail -->
        <div
          class="w-16 h-16 shrink-0 bg-[#1a2235] rounded-[10.4px] outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] flex justify-center items-center overflow-hidden cursor-pointer hover:opacity-80 transition-opacity"
          @click="router.push('/product/' + buy.productId + '?session=' + buy.uuid)"
        >
          <img
            v-if="buy.productImage"
            :src="buy.productImage"
            :alt="buy.name"
            class="w-full h-full object-cover"
          />
        </div>

        <!-- Info -->
        <div class="flex-1 min-w-0 flex flex-col gap-2.5">
          <!-- Name row -->
          <div class="self-stretch inline-flex justify-between items-start gap-2 flex-wrap">
            <div class="flex flex-col gap-[2.6px] min-w-0">
              <span
                class="text-gray-100 text-base font-bold font-['Unbounded'] leading-5 truncate cursor-pointer hover:text-orange-400 transition-colors"
                @click="router.push('/product/' + buy.productId + '?session=' + buy.uuid)"
              >
                {{ buy.name }}
              </span>
            </div>
            <!-- Status badge -->
            <div
              class="shrink-0 px-2.5 py-[3px] rounded-full outline outline-1 outline-offset-[-1px] flex items-center gap-1.5"
              :class="[statusColor(buy.rawStatus).bg, statusColor(buy.rawStatus).outline]"
            >
              <span class="w-1.5 h-1.5 rounded-sm shrink-0" :class="statusColor(buy.rawStatus).dot" />
              <span class="text-xs font-normal font-['Onest'] leading-4" :class="statusColor(buy.rawStatus).text">{{ buy.status }}</span>
            </div>
          </div>

          <!-- Price -->
          <div class="inline-flex items-center gap-2.5 flex-wrap">
            <span class="text-gray-100 text-lg font-extrabold font-['Unbounded'] leading-7">
              {{ fmt(buy.price) }}
            </span>
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
        <!-- Empty slots -->
        <div
          v-for="i in emptySlots(buy)"
          :key="i"
          class="w-9 h-9 p-[0.6px] bg-[#1a2235] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#2a3a52] flex justify-center items-center shrink-0 transition-colors duration-150 hover:outline-orange-500/30 hover:bg-[#1e2d3d] cursor-pointer"
        >
          <span class="text-gray-600 text-lg font-normal font-['Onest'] leading-7">+</span>
        </div>

        <!-- Empty slots label -->
        <span v-if="emptySlots(buy).length > 0" class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">
          Ще {{ emptySlots(buy).length }} {{ emptySlots(buy).length === 1 ? 'місце' : 'місця' }}
        </span>

        <!-- Countdown -->
        <div class="ml-auto shrink-0 px-3 py-1.5 bg-[#0d1117] rounded-[9.6px] outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] inline-flex items-center gap-2">
          <!-- Clock icon -->
          <svg class="w-3 h-3 text-gray-400 shrink-0" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.1">
            <circle cx="6" cy="6" r="4.5"/>
            <path d="M6 3.5v2.5l1.5 1" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">До скасування:</span>
          <span
            class="text-sm font-normal font-['Unbounded'] leading-5 tracking-wide tabular-nums"
            :class="buy.countdown === 'Час вичерпано' ? 'text-red-400' : 'text-slate-200'"
          >
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
          <svg v-if="copiedId !== buy.uuid" class="w-3.5 h-3.5 text-white shrink-0" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.2">
            <rect x="4.5" y="4.5" width="7" height="8" rx="1.2"/>
            <path d="M4.5 9H3a1 1 0 01-1-1V3a1 1 0 011-1h5a1 1 0 011 1v1.5" stroke-linecap="round"/>
          </svg>
          <svg v-else class="w-3.5 h-3.5 text-white shrink-0" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M2 7l4 4 6-6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-white text-sm font-bold font-['Onest'] leading-5 whitespace-nowrap">
            {{ copiedId === buy.uuid ? 'Скопійовано!' : 'Скопіювати посилання для друзів' }}
          </span>
        </button>

        <!-- Cancel button -->
        <button
          class="w-40 h-11 shrink-0 px-4 bg-[#1a2235] rounded-[10.4px] outline outline-1 outline-offset-[-1px] outline-[#2a3a52] inline-flex justify-center items-center gap-2 transition-all duration-150 hover:bg-red-500/10 hover:outline-red-500/50 hover:text-red-400 active:scale-[0.98] focus:outline-none focus:ring-2 focus:ring-red-500/30 group"
          @click="cancelBuy(buy.uuid)"
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
      v-if="!groupBuyStore.isLoading && mappedSessions.length === 0"
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
