<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

// ─── Props & Emits ────────────────────────────────────────────────────────────

const props = defineProps<{ isOpen: boolean }>()
const emit  = defineEmits<{ (e: 'close'): void }>()

// ─── Types ────────────────────────────────────────────────────────────────────

interface Member {
  id: number
  name: string
  initials: string
  gradient: string
  isOwner: boolean
  slot: number   // position badge number shown on empty slot
}

interface ShareOption {
  id: 'telegram' | 'viber' | 'more'
  label: string
  color: string
}

// ─── State ────────────────────────────────────────────────────────────────────

const inviteLink  = ref('sellora.com/group/12345')
const copied      = ref(false)
const totalSlots  = 3

const members: Member[] = [
  { id: 1, name: 'Михайло', initials: 'М', gradient: 'from-orange-500 to-red-600', isOwner: true,  slot: 0 },
]

// Computed empty slots
const emptySlots = computed(() =>
  Array.from({ length: totalSlots - members.length }, (_, i) => ({
    id: i,
    badgeNum: members.length + i + 1,
  }))
)

const shareOptions: ShareOption[] = [
  { id: 'telegram', label: 'Telegram', color: '#2aabee' },
  { id: 'viber',    label: 'Viber',    color: '#7360f2' },
  { id: 'more',     label: 'Ще...',    color: '' },
]

// ─── Clipboard ───────────────────────────────────────────────────────────────

async function copyLink() {
  try {
    await navigator.clipboard.writeText('https://' + inviteLink.value)
    copied.value = true
    setTimeout(() => (copied.value = false), 2000)
  } catch {
    // fallback for older browsers
    const el = document.createElement('input')
    el.value = inviteLink.value
    document.body.appendChild(el)
    el.select()
    document.execCommand('copy')
    document.body.removeChild(el)
    copied.value = true
    setTimeout(() => (copied.value = false), 2000)
  }
}

// ─── Countdown timer ─────────────────────────────────────────────────────────

const countdown = ref({ h: 23, m: 58, s: 59 })
let timerInterval: ReturnType<typeof setInterval> | null = null

function startTimer() {
  timerInterval = setInterval(() => {
    const { h, m, s } = countdown.value
    if (s > 0)       { countdown.value.s-- }
    else if (m > 0)  { countdown.value.m--; countdown.value.s = 59 }
    else if (h > 0)  { countdown.value.h--; countdown.value.m = 59; countdown.value.s = 59 }
    else             { stopTimer() }
  }, 1000)
}

function stopTimer() {
  if (timerInterval) { clearInterval(timerInterval); timerInterval = null }
}

// Timer progress: percentage of 24h elapsed
const timerProgress = computed(() => {
  const total   = 24 * 3600
  const elapsed = (23 - countdown.value.h) * 3600 + (59 - countdown.value.m) * 60 + (59 - countdown.value.s)
  return Math.min((elapsed / total) * 100, 100)
})

const pad = (n: number) => String(n).padStart(2, '0')

// Group progress bar
const groupProgress = computed(() => (members.length / totalSlots) * 100)

// ─── Lifecycle ────────────────────────────────────────────────────────────────

watch(() => props.isOpen, (val) => {
  if (val) {
    startTimer()
    document.body.style.overflow = 'hidden'
  } else {
    stopTimer()
    document.body.style.overflow = ''
  }
}, { immediate: true })

onUnmounted(() => {
  stopTimer()
  document.body.style.overflow = ''
})

// ─── Share handlers ───────────────────────────────────────────────────────────

function share(id: ShareOption['id']) {
  const url = encodeURIComponent('https://' + inviteLink.value)
  const text = encodeURIComponent('Приєднуйся до групової покупки на Sellora та заощади!')
  const map: Record<string, string> = {
    telegram: `https://t.me/share/url?url=${url}&text=${text}`,
    viber:    `viber://forward?text=${text}%20${url}`,
  }
  if (map[id]) window.open(map[id], '_blank', 'noopener')
  else if (id === 'more' && navigator.share) {
    navigator.share({ title: 'Sellora', text: 'Групова покупка', url: 'https://' + inviteLink.value })
  }
}
</script>

<template>
  <!-- ── Overlay ─────────────────────────────────────────────────────────── -->
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      leave-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      leave-to-class="opacity-0"
    >
      <div
        v-if="isOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
        @click.self="emit('close')"
      >
        <!-- ── Modal card ─────────────────────────────────────────────────── -->
        <Transition
          enter-active-class="transition-all duration-300"
          leave-active-class="transition-all duration-200"
          enter-from-class="opacity-0 scale-95 translate-y-4"
          leave-to-class="opacity-0 scale-95"
        >
          <div
            v-if="isOpen"
            class="relative w-full max-w-[512px] bg-gradient-to-br from-[#252b3b] to-[#1a1f2e] rounded-2xl shadow-[0px_0px_60px_0px_rgba(249,115,22,0.06),0px_32px_80px_0px_rgba(0,0,0,0.70),0px_0px_0px_1px_rgba(249,115,22,0.08)] outline outline-1 outline-offset-[-1px] outline-white/10 flex flex-col overflow-hidden max-h-[90vh] overflow-y-auto"
            @click.stop
          >
            <!-- Top accent bar -->
            <div class="w-full h-[3px] shrink-0 bg-gradient-to-r from-orange-500 via-amber-400 to-orange-500" />

            <!-- Close button -->
            <button
              aria-label="Закрити"
              class="absolute right-5 top-5 z-10 w-8 h-8 bg-white/5 rounded-xl outline outline-1 outline-offset-[-1px] outline-white/10 flex justify-center items-center transition-all duration-150 hover:bg-white/10 hover:outline-white/20 active:scale-95"
              @click="emit('close')"
            >
              <svg class="w-4 h-4 text-slate-400" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.67" stroke-linecap="round">
                <path d="M4 4l8 8M12 4l-8 8"/>
              </svg>
            </button>

            <!-- ── Body ──────────────────────────────────────────────────── -->
            <div class="p-8 flex flex-col gap-5">

              <!-- Hero section -->
              <div class="self-stretch pb-1 flex flex-col items-center gap-0">
                <!-- Icon -->
                <div class="w-24 h-24 mb-0 flex items-center justify-center">
                  <div class="w-24 h-24 bg-[radial-gradient(ellipse_70.71%_70.71%_at_50%_50%,rgba(249,115,22,0.18)_0%,rgba(249,115,22,0)_70%)] rounded-full flex justify-center items-center">
                    <div class="w-16 h-16 bg-gradient-to-br from-orange-500 to-orange-600 rounded-full shadow-[0px_8px_32px_0px_rgba(249,115,22,0.55),inset_0px_1px_0px_0px_rgba(255,255,255,0.20)] flex justify-center items-center">
                      <!-- Users icon -->
                      <svg class="w-8 h-8 text-white" viewBox="0 0 32 32" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="11" r="4"/>
                        <path d="M4 27a8 8 0 0116 0"/>
                        <circle cx="23" cy="11" r="3"/>
                        <path d="M27 27c0-3.5-2-6.5-5-7.5"/>
                      </svg>
                    </div>
                  </div>
                </div>

                <!-- Badge -->
                <div class="pb-4 pt-1">
                  <span class="px-3 py-1 bg-orange-500/10 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/25 text-orange-500 text-xs font-bold font-['Onest'] uppercase leading-4 tracking-wide">
                    🔥 Групова покупка
                  </span>
                </div>

                <!-- Title -->
                <h2 class="pb-3 text-center text-white text-2xl font-black font-['Unbounded'] leading-8">
                  Групу успішно відкрито!
                </h2>

                <!-- Subtitle -->
                <p class="max-w-80 px-1.5 text-center text-sm font-normal font-['Onest'] leading-6 text-slate-400">
                  Ви зафіксували акційну ціну
                  <span class="text-orange-500 font-bold">25 000 ₴</span>.
                  Запросіть ще
                  <span class="text-white font-semibold">2 друзів</span>
                  протягом 24 годин — і замовлення буде відправлено!
                </p>
              </div>

              <div class="self-stretch h-px bg-white/5" />

              <!-- ── Members section ──────────────────────────────────────── -->
              <div class="self-stretch py-1 flex flex-col items-center gap-3">
                <span class="text-center text-slate-600 text-[9px] font-semibold font-['Unbounded'] uppercase leading-4 tracking-wide">
                  Учасники групи
                </span>

                <!-- Progress bar -->
                <div class="w-full h-1.5 relative bg-white/5 rounded-full overflow-hidden">
                  <div
                    class="h-1.5 absolute left-0 top-0 bg-gradient-to-r from-orange-500 to-amber-400 rounded-full shadow-[0px_0px_8px_0px_rgba(249,115,22,0.50)] transition-all duration-500"
                    :style="{ width: groupProgress + '%' }"
                  />
                </div>

                <!-- Avatar row -->
                <div class="self-stretch pt-1 inline-flex justify-center items-center">

                  <!-- Filled member slot -->
                  <div
                    v-for="member in members"
                    :key="member.id"
                    class="w-16 h-24 relative flex flex-col items-center"
                  >
                    <!-- Avatar -->
                    <div
                      class="w-16 h-16 bg-gradient-to-br rounded-full flex justify-center items-center shadow-[0px_4px_16px_0px_rgba(249,115,22,0.35),0px_0px_0px_3px_rgba(249,115,22,0.40)] relative"
                      :class="member.gradient"
                    >
                      <span class="text-white text-xl font-bold font-['Unbounded'] leading-7">{{ member.initials }}</span>
                      <!-- Online dot -->
                      <div class="w-5 h-5 absolute right-0 bottom-0 bg-green-500 rounded-full outline outline-2 outline-offset-[-2px] outline-[#1a1f2e] flex justify-center items-center">
                        <svg class="w-2.5 h-2.5 text-white" viewBox="0 0 10 10" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M2 5.5l2 2 4-4"/>
                        </svg>
                      </div>
                    </div>
                    <span class="mt-2 text-white text-xs font-semibold font-['Onest'] leading-4">{{ member.name }}</span>
                    <span class="text-green-500 text-xs font-normal font-['Onest'] leading-4">Ви</span>
                  </div>

                  <!-- Separator dots -->
                  <template v-if="emptySlots.length > 0">
                    <div class="px-3 pb-5 flex items-center gap-1">
                      <span v-for="i in 3" :key="i" class="w-1 h-1 bg-white/10 rounded-sm" />
                    </div>
                  </template>

                  <!-- Empty slots -->
                  <template v-for="(slot, si) in emptySlots" :key="slot.id">
                    <div class="w-16 h-24 relative flex flex-col items-center">
                      <!-- Empty avatar circle -->
                      <div class="w-16 h-16 relative bg-white/5 rounded-full outline outline-2 outline-offset-[-2px] outline-white/20 flex justify-center items-center">
                        <!-- Person placeholder -->
                        <svg class="w-5 h-5 text-white/20" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.25" stroke-linecap="round" stroke-linejoin="round">
                          <circle cx="10" cy="7" r="3.5"/>
                          <path d="M3 18a7 7 0 0114 0"/>
                        </svg>
                        <!-- Slot number badge -->
                        <div class="w-5 h-5 absolute right-0 top-0 bg-orange-500/20 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/40 flex justify-center items-center">
                          <span class="text-orange-500 text-[9px] font-bold font-['Onest'] leading-3">{{ slot.badgeNum }}</span>
                        </div>
                      </div>
                      <span class="mt-2 text-white/30 text-[9px] font-semibold font-['Onest'] uppercase leading-3 tracking-tight">Вільно</span>
                      <span class="text-orange-500/50 text-[9px] font-semibold font-['Onest'] uppercase leading-3 tracking-tight">Чекаємо...</span>
                    </div>

                    <!-- Dots between empty slots -->
                    <div v-if="si < emptySlots.length - 1" class="px-3 pb-5 flex items-center gap-1">
                      <span v-for="i in 3" :key="i" class="w-1 h-1 bg-white/10 rounded-sm" />
                    </div>
                  </template>
                </div>

                <!-- Counter label -->
                <p class="text-center text-xs font-normal font-['Onest'] leading-4">
                  <span class="text-orange-500 font-bold">{{ members.length }}</span>
                  <span class="text-slate-600"> з </span>
                  <span class="text-gray-300 font-semibold">{{ totalSlots }}</span>
                  <span class="text-slate-600"> учасників приєдналось</span>
                </p>
              </div>

              <div class="self-stretch h-px bg-white/5" />

              <!-- ── Invite link ───────────────────────────────────────────── -->
              <div class="self-stretch flex flex-col gap-2.5">
                <span class="text-slate-600 text-[9px] font-semibold font-['Unbounded'] uppercase leading-4 tracking-wide">
                  Посилання для запрошення
                </span>
                <div class="self-stretch inline-flex items-center gap-2">
                  <!-- Input-like field -->
                  <div class="flex-1 relative">
                    <div class="self-stretch pl-9 pr-3 py-2.5 bg-white/5 rounded-xl outline outline-1 outline-offset-[-1px] outline-white/10 flex items-center overflow-hidden transition-all duration-150 hover:bg-white/[0.07] hover:outline-white/20">
                      <!-- Link icon -->
                      <svg class="absolute left-3 w-3.5 h-3.5 text-slate-600 shrink-0" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.2">
                        <path d="M5.5 8.5a3 3 0 004.24 0l2-2a3 3 0 00-4.24-4.24l-1.1 1.1" stroke-linecap="round"/>
                        <path d="M8.5 5.5a3 3 0 00-4.24 0l-2 2a3 3 0 004.24 4.24l1.1-1.1" stroke-linecap="round"/>
                      </svg>
                      <span class="flex-1 text-slate-200 text-xs font-normal font-['Onest'] leading-5 truncate">
                        {{ inviteLink }}
                      </span>
                    </div>
                  </div>
                  <!-- Copy button -->
                  <button
                    class="px-4 py-2.5 bg-gradient-to-b from-orange-500 to-orange-600 rounded-xl shadow-[0px_4px_14px_0px_rgba(249,115,22,0.40)] flex items-center gap-1.5 transition-all duration-150 hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.55)] active:scale-95 focus:outline-none focus:ring-2 focus:ring-orange-500/50 whitespace-nowrap"
                    @click="copyLink"
                  >
                    <!-- Clipboard / check icon -->
                    <svg v-if="!copied" class="w-4 h-4 text-white" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="5" y="4" width="8" height="10" rx="1.5"/>
                      <path d="M5 6H4a1 1 0 01-1-1V3a1 1 0 011-1h5a1 1 0 011 1v1" stroke-linecap="round"/>
                    </svg>
                    <svg v-else class="w-4 h-4 text-white" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M3 8l4 4 6-6" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <span class="text-white text-sm font-semibold font-['Onest'] leading-5">
                      {{ copied ? 'Скопійовано!' : 'Копіювати' }}
                    </span>
                  </button>
                </div>
              </div>

              <!-- ── Share via ─────────────────────────────────────────────── -->
              <div class="self-stretch flex flex-col gap-2.5">
                <span class="text-slate-600 text-[9px] font-semibold font-['Unbounded'] uppercase leading-4 tracking-wide">
                  Поширити через
                </span>
                <div class="self-stretch inline-flex gap-2">
                  <button
                    v-for="opt in shareOptions"
                    :key="opt.id"
                    class="flex-1 px-3 py-2.5 bg-white/5 rounded-xl outline outline-1 outline-offset-[-1px] outline-white/10 flex justify-center items-center gap-2 transition-all duration-150 active:scale-95 focus:outline-none focus:ring-2 focus:ring-white/10"
                    :class="
                      opt.id === 'telegram' ? 'hover:bg-[#2aabee]/10 hover:outline-[#2aabee]/30'
                      : opt.id === 'viber'  ? 'hover:bg-[#7360f2]/10 hover:outline-[#7360f2]/30'
                      : 'hover:bg-white/10 hover:outline-white/20'
                    "
                    @click="share(opt.id)"
                  >
                    <!-- Telegram -->
                    <svg v-if="opt.id === 'telegram'" class="w-4 h-4 shrink-0" viewBox="0 0 16 16" fill="#2aabee">
                      <path d="M2.27 7.7 13.2 3.4a.5.5 0 0 1 .64.65l-1.9 9a.5.5 0 0 1-.77.3L8.3 11.1l-1.7 1.6a.5.5 0 0 1-.84-.37V9.98L2.13 8.63a.5.5 0 0 1 .14-.93z"/>
                    </svg>
                    <!-- Viber -->
                    <svg v-else-if="opt.id === 'viber'" class="w-4 h-4 shrink-0" viewBox="0 0 16 16" fill="none" stroke="#7360f2" stroke-width="1.3">
                      <path d="M8 1.5C4.96 1.5 2.5 3.76 2.5 6.6c0 1.7.86 3.2 2.2 4.18v2.22l2.1-1.06c.38.07.78.1 1.2.1 3.04 0 5.5-2.26 5.5-5.06C13.5 3.76 11.04 1.5 8 1.5z" stroke-linecap="round"/>
                      <path d="M6.2 5.5c.1.5.3.97.6 1.38.3.4.7.74 1.2.98" stroke-linecap="round"/>
                    </svg>
                    <!-- More -->
                    <svg v-else class="w-4 h-4 shrink-0 text-slate-400" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.33">
                      <circle cx="4" cy="8" r="1"/><circle cx="8" cy="8" r="1"/><circle cx="12" cy="8" r="1"/>
                    </svg>
                    <span class="text-gray-300 text-xs font-medium font-['Onest'] leading-4">{{ opt.label }}</span>
                  </button>
                </div>
              </div>

              <div class="self-stretch h-px bg-white/5" />

              <!-- ── Countdown timer ──────────────────────────────────────── -->
              <div class="self-stretch p-4 bg-white/5 rounded-2xl outline outline-1 outline-offset-[-1px] outline-white/5 flex flex-col gap-3">
                <!-- Label -->
                <div class="inline-flex justify-center items-center gap-2">
                  <svg class="w-3.5 h-3.5 text-orange-500 shrink-0" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.2">
                    <circle cx="7" cy="7" r="5.5"/>
                    <path d="M7 4v3l2 1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  <span class="text-orange-500 text-[9px] font-bold font-['Unbounded'] uppercase leading-4 tracking-wide">
                    Залишилось часу
                  </span>
                </div>

                <!-- Digit blocks -->
                <div class="self-stretch inline-flex justify-center items-center gap-1.5">
                  <template v-for="(unit, idx) in [
                    { val: countdown.h, label: 'год' },
                    { val: countdown.m, label: 'хв'  },
                    { val: countdown.s, label: 'сек' },
                  ]" :key="unit.label">
                    <div class="min-w-12 px-3 py-2 bg-black/30 rounded-[10px] shadow-[inset_0px_1px_0px_1px_rgba(255,255,255,0.05)] outline outline-1 outline-offset-[-1px] outline-white/10 flex flex-col items-center gap-0.5">
                      <span class="text-slate-100 text-xl font-black font-['Unbounded'] leading-7 tabular-nums">
                        {{ pad(unit.val) }}
                      </span>
                      <span class="text-white/30 text-[9px] font-semibold font-['Unbounded'] uppercase leading-3 tracking-wide">
                        {{ unit.label }}
                      </span>
                    </div>
                    <span v-if="idx < 2" class="pb-4 text-orange-500/70 text-xl font-normal font-['Unbounded'] leading-5">:</span>
                  </template>
                </div>

                <!-- Time progress bar -->
                <div class="self-stretch h-1 relative bg-white/5 rounded-full overflow-hidden">
                  <div
                    class="h-1 absolute left-0 top-0 bg-gradient-to-r from-orange-500 to-amber-400 rounded-full shadow-[0px_0px_8px_0px_rgba(249,115,22,0.50)] transition-all duration-1000"
                    :style="{ width: (100 - timerProgress) + '%' }"
                  />
                </div>
              </div>

            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
