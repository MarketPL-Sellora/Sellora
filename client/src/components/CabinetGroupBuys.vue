<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { useGroupBuyStore } from '../state/groupBuyStore'
import type { GroupBuySession } from '../state/groupBuyStore'
import { apiClient } from '../api/axios'

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
  ogShareLink: string
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
    ogShareLink: apiClient.defaults.baseURL + '/share/group-buy/' + s.uuid,
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
            {{ copiedId === buy.uuid ? 'Скопійовано!' : 'Копіювати посилання' }}
          </span>
        </button>

        <!-- Telegram -->
        <a
          :href="'https://t.me/share/url?url=' + encodeURIComponent(buy.ogShareLink) + '&text=' + encodeURIComponent('Приєднуйся до моєї групової покупки на Sellora!')"
          target="_blank"
          rel="noopener"
          class="w-11 h-11 shrink-0 rounded-[10.4px] bg-[#2AABEE] text-white flex justify-center items-center hover:bg-[#2298D6] transition-colors active:scale-[0.98]"
          title="Поділитися в Telegram"
        >
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M11.944 0A12 12 0 1 0 24 12.056A12.014 12.014 0 0 0 11.944 0Zm5.654 8.153l-1.636 7.706c-.122.546-.443.678-.898.422l-2.482-1.83l-1.198 1.153a.624.624 0 0 1-.499.243l.178-2.527l4.6-4.157c.2-.178-.043-.277-.31-.1l-5.688 3.583l-2.45-.764c-.534-.167-.544-.534.112-.791l9.573-3.69c.445-.16.834.108.698.752Z"/>
          </svg>
        </a>

        <!-- Viber -->
        <a
          :href="'viber://forward?text=' + encodeURIComponent('Приєднуйся до моєї групової покупки на Sellora! ' + buy.ogShareLink)"
          target="_blank"
          rel="noopener"
          class="w-11 h-11 shrink-0 rounded-[10.4px] bg-[#7360F2] text-white flex justify-center items-center hover:bg-[#5E4CE0] transition-colors active:scale-[0.98]"
          title="Поділитися у Viber"
        >
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20.812 2.343A12.727 12.727 0 0 0 12.29.006h-.092C10.478.033 6.168.402 3.68 2.785 1.916 4.55 1.082 7.288.916 10.773c-.163 3.484-.233 10.027 6.104 11.903h.012l-.01 2.727s-.042 1.104.685 1.328c.88.274 1.398-.57 2.24-1.484.46-.5 1.094-1.237 1.572-1.8 4.334.366 7.668-.469 8.045-.598.872-.3 5.804-.916 6.61-7.475.832-6.763-.394-11.033-2.362-12.031ZM18.132 16.2c-.55 1.844-2.766 3.378-2.766 3.378l-.018.012c-.732.478-1.486.67-2.14.67-.845 0-1.535-.318-1.958-.524a12.428 12.428 0 0 1-3.61-2.753l-.024-.026-.012-.018-.018-.018a12.493 12.493 0 0 1-2.652-3.714c-.46-1.022-.678-1.897-.648-2.628.03-.73.258-1.297.65-1.66.007-.005.012-.012.018-.018.676-.642 1.389-.738 1.828-.738.1 0 .186.006.26.012a1.14 1.14 0 0 1 .785.32c.394.376.97 1.16 1.296 1.77.2.374.294.672.284.91-.018.379-.21.623-.384.79l-.354.39c-.168.174-.053.522-.053.522 1.07 2.545 3.089 3.48 3.089 3.48l.072.032s.33.126.51-.053l.42-.487c.16-.186.397-.39.784-.39.1 0 .21.016.332.056.61.198 1.407.76 1.8 1.122.33.302.44.532.354.8ZM12.156 5.963a5.126 5.126 0 0 1 3.622 1.472c.987.988 1.568 2.348 1.644 3.836a.474.474 0 0 1-.456.492h-.018a.474.474 0 0 1-.474-.456c-.066-1.27-.564-2.406-1.404-3.2-.84-.794-1.95-1.216-3.218-1.228a.474.474 0 0 1-.47-.478.474.474 0 0 1 .478-.47l.296.032Zm.758 2.048c.812.07 1.5.42 1.992.988.464.536.746 1.248.794 2.004a.474.474 0 0 1-.448.498h-.024a.474.474 0 0 1-.474-.45c-.034-.54-.234-1.026-.57-1.414-.348-.4-.842-.66-1.396-.708a.474.474 0 0 1-.426-.518.474.474 0 0 1 .516-.426l.036.026Zm.272 1.994c.846.108 1.313.77 1.37 1.406a.474.474 0 0 1-.436.51.474.474 0 0 1-.51-.436c-.03-.323-.27-.672-.726-.73a.474.474 0 0 1-.408-.534.474.474 0 0 1 .534-.408l.176.192Z"/>
          </svg>
        </a>

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
