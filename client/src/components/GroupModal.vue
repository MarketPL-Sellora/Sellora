<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useGroupBuyStore }         from '../state/groupBuyStore'
import { useUserStore }             from '../state/userStore'
import { apiClient }                from '../api/axios'

// ─── Props & Emits ────────────────────────────────────────────────────────────
const props = defineProps<{
  isOpen?:   boolean
  productId: number
}>()

const emit = defineEmits<{ (e: 'close'): void }>()
const store     = useGroupBuyStore()
const userStore = useUserStore()

// ─── User initials (перші 2 літери email) ─────────────────────────────────────
const userInitials = computed(() => {
  const email = userStore.user?.email ?? ''
  return email.slice(0, 2).toUpperCase()
})

// ─── Invite link (локальне, для інпута) ───────────────────────────────────────
const inviteLink = computed(() =>
  store.session
    ? `${window.location.origin}/product/${props.productId}?session=${store.session.uuid}`
    : ''
)

// ─── OG Share link (бекенд-роут для генерації OG тегів) ──────────────────────
const ogShareLink = computed(() =>
  store.session
    ? `${apiClient.defaults.baseURL}/share/group-buy/${store.session.uuid}`
    : ''
)

// ─── Copy logic ───────────────────────────────────────────────────────────────
const copied = ref(false)

async function copyLink() {
  if (!inviteLink.value) return
  try {
    await navigator.clipboard.writeText(inviteLink.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    // fallback для старих браузерів
    const el = document.createElement('textarea')
    el.value = inviteLink.value
    document.body.appendChild(el)
    el.select()
    document.execCommand('copy')
    document.body.removeChild(el)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  }
}

// ─── Web Share API ────────────────────────────────────────────────────────────
async function shareNative() {
  try {
    await navigator.share({
      title: 'Групова покупка',
      text:  'Приєднуйся до моєї групової покупки!',
      url:   ogShareLink.value,
    })
  } catch (err) {
    // Користувач скасував або браузер не підтримує Web Share API — ігноруємо
    console.warn('Web Share API:', err)
  }
}

// ─── Шарінг у Telegram / Viber ────────────────────────────────────────────────
const shareText = 'Приєднуйся до моєї групової покупки на Sellora!'

function shareToTelegram() {
  const url = `https://t.me/share/url?url=${encodeURIComponent(ogShareLink.value)}&text=${encodeURIComponent(shareText)}`
  window.open(url, '_blank', 'noopener')
}

function shareToViber() {
  const url = `viber://forward?text=${encodeURIComponent(shareText + ' ' + ogShareLink.value)}`
  window.open(url, '_blank', 'noopener')
}

// ─── Participants helpers ─────────────────────────────────────────────────────
// Масив слотів: true = зайнятий, false = вільний
const slots = computed(() => {
  const target  = store.session?.targetSize          ?? 3
  const current = store.session?.currentMembersCount ?? 1
  return Array.from({ length: target }, (_, i) => i < current)
})

// ─── Init: створити сесію при відкритті модалки ───────────────────────────────
onMounted(() => {
  store.createSession(props.productId)
})
</script>

<template>
  <Teleport to="body">
    <div
      class="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4"
      @click.self="emit('close')"
    >
      <div
        class="relative w-full max-w-md bg-[#1a1c23] rounded-2xl p-6 flex flex-col gap-5 shadow-[0px_32px_80px_0px_rgba(0,0,0,0.70)]"
        @click.stop
      >
        <div class="absolute top-0 left-0 right-0 h-[3px] bg-gradient-to-r from-orange-500 via-amber-400 to-orange-500 rounded-t-2xl" />

        <button
          aria-label="Закрити"
          class="absolute right-4 top-4 w-8 h-8 bg-white/5 rounded-xl outline outline-1 outline-white/10 flex items-center justify-center transition-all duration-150 hover:bg-white/10 active:scale-95"
          @click="emit('close')"
        >
          <svg class="w-4 h-4 text-slate-400" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.67" stroke-linecap="round">
            <path d="M4 4l8 8M12 4l-8 8"/>
          </svg>
        </button>

        <div class="flex flex-col items-center gap-3 pt-4">
          <div class="w-14 h-14 bg-gradient-to-br from-orange-500 to-orange-600 rounded-full flex items-center justify-center shadow-[0px_8px_32px_0px_rgba(249,115,22,0.45)]">
            <svg class="w-7 h-7 text-white" viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="10" cy="10" r="4"/>
              <path d="M2 24a8 8 0 0116 0"/>
              <circle cx="21" cy="10" r="3"/>
              <path d="M24 24c0-3-1.5-5.5-4-6.5"/>
            </svg>
          </div>

          <h2 class="text-white text-xl font-black font-['Unbounded'] leading-7 text-center">
            Групова покупка
          </h2>

          <p class="text-slate-400 text-sm font-normal font-['Onest'] leading-6 text-center max-w-xs">
            Зберіть групу з
            <span class="text-orange-500 font-semibold">{{ store.session?.targetSize ?? 3 }} учасників</span>
            протягом
            <span class="text-white font-semibold">24 годин</span>
            та отримайте знижку на цей товар.
          </p>
        </div>

        <div class="h-px bg-white/5" />

        <div class="flex flex-col gap-2">
          <span class="text-slate-600 text-[9px] font-semibold font-['Unbounded'] uppercase leading-4 tracking-wide text-center">
            Учасники групи
          </span>

          <div v-if="store.isLoading" class="flex justify-center py-2">
            <span class="text-slate-400 text-xs font-['Onest'] animate-pulse">Створюємо сесію...</span>
          </div>

          <div v-else-if="store.error" class="flex justify-center py-2">
            <span class="text-red-400 text-xs font-['Onest']">{{ store.error }}</span>
          </div>

          <div v-else class="flex justify-center gap-3">
            <div
              v-for="(filled, i) in slots"
              :key="i"
              class="w-12 h-12 rounded-full flex items-center justify-center"
              :class="
                filled
                  ? 'bg-gradient-to-br from-orange-500 to-red-600 shadow-[0_0_0_3px_rgba(249,115,22,0.3)]'
                  : 'bg-white/5 outline outline-2 outline-white/20'
              "
            >
              <span v-if="i === 0 && filled" class="text-white text-sm font-bold font-['Unbounded']">{{ userInitials }}</span>
              <svg
                v-else-if="filled"
                class="w-5 h-5 text-white"
                viewBox="0 0 20 20" fill="currentColor"
              >
                <path d="M10 10a4 4 0 100-8 4 4 0 000 8zm-7 8a7 7 0 1114 0H3z"/>
              </svg>
              <span v-else class="text-gray-600 text-xl">+</span>
            </div>
          </div>

          <p v-if="!store.isLoading && !store.error" class="text-center text-xs font-['Onest'] text-slate-600">
            <span class="text-orange-500 font-bold">{{ store.session?.currentMembersCount ?? 1 }}</span>
            <span> з </span>
            <span class="text-gray-300 font-semibold">{{ store.session?.targetSize ?? 3 }}</span>
            <span> учасників приєдналось</span>
          </p>
        </div>

        <div class="h-px bg-white/5" />

        <!-- ─── Посилання + кнопки шарінгу ─────────────────────────────────── -->
        <div class="flex flex-col gap-2">
          <span class="text-slate-600 text-[9px] font-semibold font-['Unbounded'] uppercase leading-4 tracking-wide">
            Посилання для запрошення
          </span>
          <div class="flex items-center gap-2">
            <div class="flex-1 px-3 py-2.5 bg-white/5 rounded-xl outline outline-1 outline-white/10 text-slate-200 text-xs font-['Onest'] truncate">
              <span v-if="store.isLoading" class="animate-pulse text-slate-500">Генеруємо посилання...</span>
              <span v-else>{{ inviteLink }}</span>
            </div>

            <!-- Кнопка: Копіювати -->
            <button
              :disabled="store.isLoading || !inviteLink"
              class="w-10 h-10 flex-shrink-0 bg-white/5 rounded-xl outline outline-1 outline-white/10 flex items-center justify-center transition-all duration-150 hover:bg-white/10 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed"
              :class="{ '!bg-green-500/20 !outline-green-500/30': copied }"
              title="Копіювати посилання"
              @click="copyLink"
            >
              <!-- Checkmark icon when copied -->
              <svg v-if="copied" class="w-4.5 h-4.5 text-green-400" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 10.5l4 4L16 6"/>
              </svg>
              <!-- Clipboard icon -->
              <svg v-else class="w-4.5 h-4.5 text-slate-300" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="6" y="6" width="10" height="12" rx="2"/>
                <path d="M4 14V5a2 2 0 012-2h7"/>
              </svg>
            </button>

            <!-- Кнопка: Telegram -->
            <button
              :disabled="store.isLoading || !ogShareLink"
              class="w-10 h-10 flex-shrink-0 rounded-xl flex items-center justify-center transition-all duration-150 hover:brightness-110 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed"
              style="background-color: #2AABEE;"
              title="Поділитися в Telegram"
              @click="shareToTelegram"
            >
              <svg class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M11.944 0A12 12 0 1 0 24 12.056A12.014 12.014 0 0 0 11.944 0Zm5.654 8.153l-1.636 7.706c-.122.546-.443.678-.898.422l-2.482-1.83l-1.198 1.153a.624.624 0 0 1-.499.243l.178-2.527l4.6-4.157c.2-.178-.043-.277-.31-.1l-5.688 3.583l-2.45-.764c-.534-.167-.544-.534.112-.791l9.573-3.69c.445-.16.834.108.698.752Z"/>
              </svg>
            </button>

            <!-- Кнопка: Viber -->
            <button
              :disabled="store.isLoading || !ogShareLink"
              class="w-10 h-10 flex-shrink-0 rounded-xl flex items-center justify-center transition-all duration-150 hover:brightness-110 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed"
              style="background-color: #7360F2;"
              title="Поділитися у Viber"
              @click="shareToViber"
            >
              <svg class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.812 2.343A12.727 12.727 0 0 0 12.29.006h-.092C10.478.033 6.168.402 3.68 2.785 1.916 4.55 1.082 7.288.916 10.773c-.163 3.484-.233 10.027 6.104 11.903h.012l-.01 2.727s-.042 1.104.685 1.328c.88.274 1.398-.57 2.24-1.484.46-.5 1.094-1.237 1.572-1.8 4.334.366 7.668-.469 8.045-.598.872-.3 5.804-.916 6.61-7.475.832-6.763-.394-11.033-2.362-12.031ZM18.132 16.2c-.55 1.844-2.766 3.378-2.766 3.378l-.018.012c-.732.478-1.486.67-2.14.67-.845 0-1.535-.318-1.958-.524a12.428 12.428 0 0 1-3.61-2.753l-.024-.026-.012-.018-.018-.018a12.493 12.493 0 0 1-2.652-3.714c-.46-1.022-.678-1.897-.648-2.628.03-.73.258-1.297.65-1.66.007-.005.012-.012.018-.018.676-.642 1.389-.738 1.828-.738.1 0 .186.006.26.012a1.14 1.14 0 0 1 .785.32c.394.376.97 1.16 1.296 1.77.2.374.294.672.284.91-.018.379-.21.623-.384.79l-.354.39c-.168.174-.053.522-.053.522 1.07 2.545 3.089 3.48 3.089 3.48l.072.032s.33.126.51-.053l.42-.487c.16-.186.397-.39.784-.39.1 0 .21.016.332.056.61.198 1.407.76 1.8 1.122.33.302.44.532.354.8ZM12.156 5.963a5.126 5.126 0 0 1 3.622 1.472c.987.988 1.568 2.348 1.644 3.836a.474.474 0 0 1-.456.492h-.018a.474.474 0 0 1-.474-.456c-.066-1.27-.564-2.406-1.404-3.2-.84-.794-1.95-1.216-3.218-1.228a.474.474 0 0 1-.47-.478.474.474 0 0 1 .478-.47l.296.032Zm.758 2.048c.812.07 1.5.42 1.992.988.464.536.746 1.248.794 2.004a.474.474 0 0 1-.448.498h-.024a.474.474 0 0 1-.474-.45c-.034-.54-.234-1.026-.57-1.414-.348-.4-.842-.66-1.396-.708a.474.474 0 0 1-.426-.518.474.474 0 0 1 .516-.426l.036.026Zm.272 1.994c.846.108 1.313.77 1.37 1.406a.474.474 0 0 1-.436.51.474.474 0 0 1-.51-.436c-.03-.323-.27-.672-.726-.73a.474.474 0 0 1-.408-.534.474.474 0 0 1 .534-.408l.176.192Z"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- ─── Велика кнопка "Поділитися" (Web Share API) ─────────────────── -->
        <button
          :disabled="store.isLoading || !ogShareLink"
          class="w-full py-3.5 bg-orange-500 rounded-xl text-white text-sm font-bold font-['Unbounded'] tracking-wide transition-all hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
          @click="shareNative"
        >
          ПОДІЛИТИСЯ З ДРУЗЯМИ
        </button>

      </div>
    </div>
  </Teleport>
</template>
