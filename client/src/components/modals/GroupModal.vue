<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useGroupBuyStore }         from '../../state/groupBuyStore'
import { useUserStore }             from '../../state/userStore'
import { apiClient }                from '../../api/axios'
import { DICT }                     from '../../constants/dictionary'
import type { GroupBuyCheckoutPayload } from '../../state/groupBuyStore'
import type { GroupBuySession }     from '../../state/groupBuyStore'

// ─── Props & Emits ────────────────────────────────────────────────────────────
const props = withDefaults(defineProps<{
  isOpen?:   boolean
  productId: number
  groupPrice: number
  sessionUuid?: string | null   // if set → join mode; otherwise → create mode
  targetSize?: number
}>(), {
  targetSize: 3,
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'open-auth'): void
  (e: 'success'): void
}>()

const store     = useGroupBuyStore()
const userStore = useUserStore()

// ─── Shipping methods (carriers) ──────────────────────────────────────────────

interface ShippingMethod {
  id: number
  name: string
  code: string
  is_active: boolean
}

const shippingMethods = ref<ShippingMethod[]>([])
const selectedCarrierId = ref<number | null>(null)
const shippingMethodsLoading = ref(false)

async function fetchShippingMethods(storeId: number) {
  shippingMethodsLoading.value = true
  try {
    const [methodsResponse, carriersResponse] = await Promise.all([
      apiClient.get(`/stores/${storeId}/shipping_methods`),
      apiClient.get('/shipping_carriers')
    ])

    const rawData = methodsResponse.data
    let methodsArray = []
    if (rawData && rawData.shipping_methods && Array.isArray(rawData.shipping_methods)) {
      methodsArray = rawData.shipping_methods
    } else if (Array.isArray(rawData)) {
      methodsArray = rawData
    }

    const carriersData = Array.isArray(carriersResponse.data) ? carriersResponse.data : carriersResponse.data?.content || []
    const dynamicCarrierNames: Record<number, string> = {}
    carriersData.forEach((c: any) => {
      dynamicCarrierNames[c.id] = c.name
    })

    shippingMethods.value = methodsArray
      .map((m: any) => ({
        id: m.carrier_id ?? m.id,
        name: dynamicCarrierNames[m.carrier_id] || m.carrier_name || m.name || 'Невідома служба',
        code: m.code ?? '',
        is_active: m.is_enabled ?? m.is_active ?? true
      }))
      .filter((m: any) => m.is_active !== false)

    if (shippingMethods.value.length > 0) {
      selectedCarrierId.value = shippingMethods.value[0].id
    }
  } catch (error) {
    console.error('Failed to fetch shipping methods', error)
    shippingMethods.value = []
  } finally {
    shippingMethodsLoading.value = false
  }
}

// ─── Autocomplete: Cities & Branches (mock data) ─────────────────────────────

const mockCities = [
  'Київ', 'Харків', 'Одеса', 'Дніпро', 'Львів', 'Запоріжжя',
  'Чернівці', 'Полтава', 'Вінниця', 'Миколаїв', 'Суми', 'Рівне',
  'Житомир', 'Черкаси', 'Хмельницький', 'Івано-Франківськ', 'Тернопіль',
  'Кропивницький', 'Ужгород', 'Луцьк', 'Херсон',
]

const mockBranches = [
  'Відділення №1', 'Відділення №2', 'Відділення №3', 'Відділення №4',
  'Відділення №5', 'Відділення №10', 'Відділення №15', 'Відділення №20',
  'Поштомат №1', 'Поштомат №5', 'Поштомат №10', 'Поштомат №25',
  'Поштомат №50', 'Поштомат №100',
]

const cityQuery = ref('')
const branchQuery = ref('')
const isCityDropdownOpen = ref(false)
const isBranchDropdownOpen = ref(false)

const filteredCities = computed(() => {
  const q = cityQuery.value.toLowerCase().trim()
  if (!q) return mockCities
  return mockCities.filter(c => c.toLowerCase().includes(q))
})

const filteredBranches = computed(() => {
  const q = branchQuery.value.toLowerCase().trim()
  if (!q) return mockBranches
  return mockBranches.filter(b => b.toLowerCase().includes(q))
})

function selectCity(city: string) {
  form.city = city
  cityQuery.value = city
  isCityDropdownOpen.value = false
}

function selectBranch(branch: string) {
  form.branch = branch
  branchQuery.value = branch
  isBranchDropdownOpen.value = false
}

function onCityInput() {
  form.city = cityQuery.value
  isCityDropdownOpen.value = true
}

function onBranchInput() {
  form.branch = branchQuery.value
  isBranchDropdownOpen.value = true
}

function closeCityDropdown() {
  setTimeout(() => { isCityDropdownOpen.value = false }, 150)
}

function closeBranchDropdown() {
  setTimeout(() => { isBranchDropdownOpen.value = false }, 150)
}

// ─── Steps: 'info' → 'checkout' ───────────────────────────────────────────────
const currentStep = ref<'info' | 'checkout'>('info')

const isJoinMode = computed(() => !!props.sessionUuid)

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
const slots = computed(() => {
  const target  = store.session?.targetSize          ?? props.targetSize
  const current = store.session?.currentMembersCount ?? 1
  return Array.from({ length: target }, (_, i) => i < current)
})

// ─── Checkout form state ──────────────────────────────────────────────────────

type DeliveryType = 'BRANCH' | 'COURIER' | 'PICKUP'
type PaymentMethod = 'CASH_ON_DELIVERY' | 'ONLINE_CARD'

interface DeliveryOption {
  id: DeliveryType
  label: string
  icon: 'branch' | 'courier' | 'pickup'
}

interface PaymentOption {
  id: PaymentMethod
  label: string
  subtitle: string
}

const form = reactive({
  firstName: '',
  lastName:  '',
  phone:     '',
  email:     '',
  city:      '',
  branch:    '',
  street:    '',
  house:     '',
  comment:   '',
})

const quantity = ref(1)

const deliveryOptions: DeliveryOption[] = [
  { id: 'BRANCH',  label: 'Відділення', icon: 'branch'  },
  { id: 'COURIER', label: "Кур'єр",    icon: 'courier'  },
  { id: 'PICKUP',  label: 'Самовивіз', icon: 'pickup'   },
]

const deliveryMethod = ref<DeliveryType>('BRANCH')

const paymentOptions: PaymentOption[] = [
  { id: 'CASH_ON_DELIVERY', label: 'Оплата під час отримання', subtitle: 'Готівкою або карткою' },
  { id: 'ONLINE_CARD',      label: 'Карткою онлайн',          subtitle: 'Visa, Mastercard'     },
]

const paymentMethod = ref<PaymentMethod>('CASH_ON_DELIVERY')

const isSubmitting = ref(false)
const submitError  = ref('')
const orderSuccess = ref(false)

const totalToPay = computed(() => props.groupPrice * quantity.value)

const fmt = (n: number) => (n || 0).toLocaleString('uk-UA') + ' ₴'

// ─── Navigate to checkout step ────────────────────────────────────────────────
function goToCheckout() {
  currentStep.value = 'checkout'
}

function goBackToInfo() {
  currentStep.value = 'info'
}

// ─── Build payload ────────────────────────────────────────────────────────────
function buildDeliveryAddress(): Record<string, string> | null {
  if (deliveryMethod.value === 'BRANCH') {
    return { city: form.city, branch: form.branch }
  }
  if (deliveryMethod.value === 'COURIER') {
    return { city: form.city, street: form.street, house: form.house }
  }
  return null
}

function validateForm(): string | null {
  if (!form.firstName.trim()) return "Введіть ім'я"
  if (!form.lastName.trim()) return 'Введіть прізвище'
  if (!form.phone.trim()) return 'Введіть номер телефону'
  if (!form.email.trim()) return 'Введіть email'

  if (deliveryMethod.value === 'BRANCH') {
    if (!form.city.trim()) return 'Оберіть місто'
    if (!form.branch.trim()) return 'Оберіть відділення'
  }
  if (deliveryMethod.value === 'COURIER') {
    if (!form.city.trim()) return 'Оберіть місто'
    if (!form.street.trim()) return 'Введіть вулицю'
    if (!form.house.trim()) return 'Введіть номер будинку'
  }

  if (quantity.value < 1) return 'Кількість має бути не менше 1'

  return null
}

async function handleSubmitOrder() {
  const validationError = validateForm()
  if (validationError) {
    alert(validationError)
    return
  }

  isSubmitting.value = true
  submitError.value = ''

  const payload: GroupBuyCheckoutPayload = {
    buyer_name: form.firstName.trim(),
    buyer_surname: form.lastName.trim(),
    buyer_phone: form.phone.trim(),
    buyer_email: form.email.trim(),
    delivery_type: deliveryMethod.value,
    carrier_id: selectedCarrierId.value || (deliveryMethod.value !== 'PICKUP' ? 1 : null),
    delivery_address: buildDeliveryAddress(),
    payment_method: paymentMethod.value,
    order_comment: form.comment.trim() || null,
    promo_code: null,
    product_id: props.productId,
    quantity: quantity.value,
  }

  try {
    let response

    if (isJoinMode.value && props.sessionUuid) {
      response = await store.joinSession(props.sessionUuid, payload)
    } else {
      response = await store.createSession(payload)
    }

    if (!response) {
      submitError.value = store.error || 'Помилка оформлення'
      alert(submitError.value)
      return
    }

    // Handle payment redirect
    if (paymentMethod.value === 'ONLINE_CARD' && response.payment_url) {
      window.location.href = response.payment_url as string
      return
    }

    // Success without online payment
    orderSuccess.value = true

    // Auto-close after 2s
    setTimeout(() => {
      emit('close')
    }, 2500)

  } catch (error: any) {
    submitError.value = error.response?.data?.message || 'Помилка оформлення'
    alert(submitError.value)
  } finally {
    isSubmitting.value = false
  }
}

// ─── Init ─────────────────────────────────────────────────────────────────────
onMounted(async () => {
  // Автозаповнення даних юзера
  if (userStore.user) {
    form.firstName = userStore.user.firstName || form.firstName;
    form.lastName = userStore.user.lastName || form.lastName;
    form.phone = userStore.user.phone || form.phone;
    form.email = userStore.user.email || form.email;
  }

  if (isJoinMode.value && props.sessionUuid) {
    await store.fetchSession(props.sessionUuid)
    currentStep.value = 'checkout'
  }

  // Дістаємо магазин товару і тягнемо пошти
  try {
    const prodRes = await apiClient.get(`/products/${props.productId}`)
    const merchantId = prodRes.data.merchant_id || prodRes.data.merchantId
    if (merchantId) {
      await fetchShippingMethods(merchantId)
    }
  } catch (e) {
    console.error('Failed to fetch product for shipping methods', e)
  }
})
</script>

<template>
  <Teleport to="body">
    <div
      class="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4"
      @click.self="emit('close')"
    >
      <div
        class="relative w-full max-w-lg bg-[#1a1c23] rounded-2xl shadow-[0px_32px_80px_0px_rgba(0,0,0,0.70)] flex flex-col max-h-[90vh] overflow-hidden"
        @click.stop
      >
        <div class="absolute top-0 left-0 right-0 h-[3px] bg-gradient-to-r from-orange-500 via-amber-400 to-orange-500 rounded-t-2xl z-10" />

        <button
          aria-label="Закрити"
          class="absolute right-4 top-4 w-8 h-8 bg-white/5 rounded-xl outline outline-1 outline-white/10 flex items-center justify-center transition-all duration-150 hover:bg-white/10 active:scale-95 z-20"
          @click="emit('close')"
        >
          <svg class="w-4 h-4 text-slate-400" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.67" stroke-linecap="round">
            <path d="M4 4l8 8M12 4l-8 8"/>
          </svg>
        </button>

        <!-- ─── Scrollable content ──────────────────────────────────────── -->
        <div class="flex-1 overflow-y-auto p-6 flex flex-col gap-5 scrollbar-thin">

          <!-- ═══════════════════════════════════════════════════════════════ -->
          <!-- STEP: INFO (create mode only — participants + share)           -->
          <!-- ═══════════════════════════════════════════════════════════════ -->
          <template v-if="currentStep === 'info'">
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
                <span class="text-orange-500 font-semibold">{{ store.session?.targetSize ?? props.targetSize }} учасників</span>
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

              <div v-else-if="!userStore.isAuthenticated" class="flex flex-col items-center justify-center py-2 gap-3">
                <span class="text-orange-400 text-xs font-['Onest'] text-center">
                  Щоб брати участь у груповій покупці, потрібно авторизуватись.
                </span>
                <button
                  class="px-5 py-2 bg-[#2a2d3e] rounded-xl outline outline-1 outline-[#3d4158] text-slate-200 text-xs font-semibold font-['Onest'] transition-all hover:bg-white/5 hover:outline-orange-500/50 active:scale-95"
                  @click="emit('open-auth')"
                >
                  Увійти або зареєструватись
                </button>
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
                <span class="text-gray-300 font-semibold">{{ store.session?.targetSize ?? props.targetSize }}</span>
                <span> учасників приєдналось</span>
              </p>
            </div>

            <div class="h-px bg-white/5" />

            <!-- ─── Кнопка переходу до оформлення ────────────────────────── -->
            <button
              v-if="userStore.isAuthenticated && !store.error"
              :disabled="store.isLoading"
              class="w-full py-3.5 bg-gradient-to-b from-orange-500 to-[#ea6c0a] rounded-xl text-white text-sm font-bold font-['Unbounded'] tracking-wide transition-all hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
              @click="goToCheckout"
            >
              ОФОРМИТИ ЗАМОВЛЕННЯ ➔
            </button>
          </template>

          <!-- ═══════════════════════════════════════════════════════════════ -->
          <!-- STEP: CHECKOUT (mini-checkout form)                            -->
          <!-- ═══════════════════════════════════════════════════════════════ -->
          <template v-if="currentStep === 'checkout'">

            <!-- Success screen -->
            <template v-if="orderSuccess">
              <div class="flex flex-col items-center gap-4 py-8">
                <div class="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center">
                  <svg class="w-8 h-8 text-green-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 13l4 4L19 7"/>
                  </svg>
                </div>
                <h3 class="text-white text-lg font-bold font-['Unbounded'] text-center">
                  {{ isJoinMode ? 'Ви приєдналися!' : 'Збір створено!' }}
                </h3>
                <p class="text-slate-400 text-sm font-['Onest'] text-center max-w-xs">
                  {{ isJoinMode
                    ? 'Ви успішно приєдналися до групової покупки. Очікуйте поки група збереться.'
                    : 'Групова покупка створена! Поділіться посиланням із друзями.'
                  }}
                </p>
              </div>
            </template>

            <!-- Checkout form -->
            <template v-else>
              <!-- Back button + title -->
              <div class="flex items-center gap-3 pt-2">
                <button
                  v-if="!isJoinMode"
                  class="w-8 h-8 bg-white/5 rounded-xl flex items-center justify-center hover:bg-white/10 transition-all active:scale-95"
                  @click="goBackToInfo"
                >
                  <svg class="w-4 h-4 text-slate-400" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.67" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M10 4L6 8l4 4"/>
                  </svg>
                </button>
                <h2 class="text-white text-lg font-bold font-['Unbounded'] leading-7">
                  {{ isJoinMode ? 'Приєднатися до збору' : 'Оформлення' }}
                </h2>
              </div>

              <!-- ── Contact info ──────────────────────────────────────────── -->
              <div class="flex flex-col gap-3">
                <div class="flex items-center gap-2">
                  <span class="px-2 py-0.5 bg-orange-500/20 rounded-full text-orange-500 text-[10px] font-bold font-['Unbounded']">1</span>
                  <span class="text-white text-sm font-bold font-['Unbounded']">{{ DICT.checkout.step1 }}</span>
                </div>
                <div class="grid grid-cols-2 gap-3">
                  <input
                    v-model="form.firstName"
                    type="text"
                    :placeholder="DICT.forms.placeholders.firstName"
                    class="col-span-1 px-3 py-2.5 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
                  />
                  <input
                    v-model="form.lastName"
                    type="text"
                    :placeholder="DICT.forms.placeholders.lastName"
                    class="col-span-1 px-3 py-2.5 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
                  />
                  <input
                    v-model="form.phone"
                    type="tel"
                    :placeholder="DICT.forms.placeholders.phone"
                    class="col-span-1 px-3 py-2.5 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
                  />
                  <input
                    v-model="form.email"
                    type="email"
                    :placeholder="DICT.forms.placeholders.email"
                    class="col-span-1 px-3 py-2.5 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
                  />
                </div>
              </div>

              <div class="h-px bg-white/5" />

              <!-- ── Delivery ──────────────────────────────────────────────── -->
              <div class="flex flex-col gap-3">
                <div class="flex items-center gap-2">
                  <span class="px-2 py-0.5 bg-orange-500/20 rounded-full text-orange-500 text-[10px] font-bold font-['Unbounded']">2</span>
                  <span class="text-white text-sm font-bold font-['Unbounded']">{{ DICT.checkout.step2 }}</span>
                </div>

                <!-- Delivery type buttons -->
                <div class="flex gap-2">
                  <button
                    v-for="opt in deliveryOptions"
                    :key="opt.id"
                    class="flex-1 px-3 py-2.5 rounded-lg outline outline-1 outline-offset-[-1px] flex flex-col items-center gap-1.5 transition-all active:scale-[0.98]"
                    :class="
                      deliveryMethod === opt.id
                        ? 'bg-orange-500/10 outline-orange-500'
                        : 'bg-white/5 outline-white/10 hover:outline-white/20'
                    "
                    @click="deliveryMethod = opt.id"
                  >
                    <!-- Branch icon -->
                    <svg v-if="opt.icon === 'branch'" class="w-4 h-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" :class="deliveryMethod === opt.id ? 'text-orange-500' : 'text-gray-400'">
                      <rect x="2.5" y="3" width="10" height="14" rx="1.5"/><circle cx="7.5" cy="10" r="2"/><path d="M16 6l-3 4 3 4" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <!-- Courier icon -->
                    <svg v-else-if="opt.icon === 'courier'" class="w-4 h-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" :class="deliveryMethod === opt.id ? 'text-orange-500' : 'text-gray-400'">
                      <rect x="1" y="6" width="12" height="8" rx="1.5"/><path d="M13 9h3l3 4v3h-6V9z" stroke-linecap="round" stroke-linejoin="round"/><circle cx="4.5" cy="16.5" r="1.5"/><circle cx="14.5" cy="16.5" r="1.5"/>
                    </svg>
                    <!-- Pickup icon -->
                    <svg v-else-if="opt.icon === 'pickup'" class="w-4 h-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" :class="deliveryMethod === opt.id ? 'text-orange-500' : 'text-gray-400'">
                      <rect x="3" y="2" width="14" height="16" rx="1.5"/><path d="M3 6h14" stroke-linecap="round"/><path d="M8 10h4M10 8v4" stroke-linecap="round"/>
                    </svg>
                    <span class="text-xs font-medium font-['Onest']" :class="deliveryMethod === opt.id ? 'text-orange-500' : 'text-gray-400'">
                      {{ opt.label }}
                    </span>
                  </button>
                </div>

                <div v-if="deliveryMethod === 'BRANCH'" class="flex flex-col gap-3 pt-3">
                  <div v-if="shippingMethodsLoading" class="text-slate-500 text-xs font-['Onest'] animate-pulse">Завантаження служб доставки...</div>
                  <template v-else-if="shippingMethods.length > 0">
                    <div class="flex flex-col gap-2">
                      <label v-for="method in shippingMethods" :key="method.id"
                        class="px-3 py-2.5 rounded-lg outline outline-1 outline-offset-[-1px] flex items-center gap-3 cursor-pointer transition-all active:scale-[0.99]"
                        :class="selectedCarrierId === method.id ? 'bg-orange-500/10 outline-orange-500' : 'bg-white/5 outline-white/10 hover:outline-white/20'"
                      >
                        <input type="radio" name="group-shipping-carrier" :value="method.id" v-model="selectedCarrierId" class="sr-only" />
                        <div class="w-4 h-4 rounded-full flex items-center justify-center shrink-0" :class="selectedCarrierId === method.id ? 'outline outline-2 outline-offset-[-2px] outline-orange-500' : 'border-2 border-gray-600'">
                          <div v-if="selectedCarrierId === method.id" class="w-2 h-2 bg-orange-500 rounded-full" />
                        </div>
                        <span class="text-xs font-medium font-['Onest'] transition-colors" :class="selectedCarrierId === method.id ? 'text-orange-500' : 'text-slate-300'">
                          {{ method.name }}
                        </span>
                      </label>
                    </div>
                  </template>
                  <div v-else class="px-3 py-2 bg-red-500/10 rounded-lg text-red-400 text-xs font-['Onest'] text-center">
                    Немає доступних служб доставки
                  </div>
                </div>

                <!-- Dynamic delivery fields -->
                <div v-if="deliveryMethod !== 'PICKUP'" class="grid grid-cols-2 gap-3">
                  <!-- City autocomplete (BRANCH & COURIER) -->
                  <div class="relative col-span-1">
                    <input
                      v-model="cityQuery"
                      type="text"
                      autocomplete="off"
                      :placeholder="DICT.forms.placeholders.selectCity"
                      class="w-full h-10 px-3 pr-8 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
                      @input="onCityInput"
                      @focus="isCityDropdownOpen = true"
                      @blur="closeCityDropdown"
                    />
                    <svg class="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-500 pointer-events-none" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M4 6l4 4 4-4" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <!-- Dropdown -->
                    <ul
                      v-if="isCityDropdownOpen && filteredCities.length > 0"
                      class="absolute z-50 left-0 right-0 top-full mt-1 max-h-48 overflow-y-auto bg-gray-800 rounded-lg border border-gray-700 shadow-xl"
                    >
                      <li
                        v-for="city in filteredCities"
                        :key="city"
                        class="px-3 py-2 text-xs text-gray-200 font-['Onest'] cursor-pointer transition-colors hover:bg-gray-700 hover:text-orange-400"
                        :class="form.city === city ? 'bg-gray-700/60 text-orange-400' : ''"
                        @mousedown.prevent="selectCity(city)"
                      >
                        {{ city }}
                      </li>
                    </ul>
                  </div>

                  <!-- Branch autocomplete (BRANCH only) -->
                  <div v-if="deliveryMethod === 'BRANCH'" class="relative col-span-1">
                    <input
                      v-model="branchQuery"
                      type="text"
                      autocomplete="off"
                      :placeholder="DICT.forms.placeholders.selectBranch"
                      class="w-full h-10 px-3 pr-8 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
                      @input="onBranchInput"
                      @focus="isBranchDropdownOpen = true"
                      @blur="closeBranchDropdown"
                    />
                    <svg class="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-500 pointer-events-none" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M4 6l4 4 4-4" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <!-- Dropdown -->
                    <ul
                      v-if="isBranchDropdownOpen && filteredBranches.length > 0"
                      class="absolute z-50 left-0 right-0 top-full mt-1 max-h-48 overflow-y-auto bg-gray-800 rounded-lg border border-gray-700 shadow-xl"
                    >
                      <li
                        v-for="branch in filteredBranches"
                        :key="branch"
                        class="px-3 py-2 text-xs text-gray-200 font-['Onest'] cursor-pointer transition-colors hover:bg-gray-700 hover:text-orange-400"
                        :class="form.branch === branch ? 'bg-gray-700/60 text-orange-400' : ''"
                        @mousedown.prevent="selectBranch(branch)"
                      >
                        {{ branch }}
                      </li>
                    </ul>
                  </div>

                  <!-- Street (COURIER only) -->
                  <input
                    v-if="deliveryMethod === 'COURIER'"
                    v-model="form.street"
                    type="text"
                    placeholder="Вулиця"
                    class="col-span-1 px-3 py-2.5 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
                  />

                  <!-- House (COURIER only) -->
                  <input
                    v-if="deliveryMethod === 'COURIER'"
                    v-model="form.house"
                    type="text"
                    placeholder="Будинок / кв."
                    class="col-span-2 px-3 py-2.5 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
                  />
                </div>

                <!-- Comment -->
                <textarea
                  v-model="form.comment"
                  rows="2"
                  :placeholder="DICT.forms.placeholders.comment"
                  class="w-full px-3 py-2.5 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] placeholder-gray-600 resize-none transition-all focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
                />
              </div>

              <div class="h-px bg-white/5" />

              <!-- ── Payment ───────────────────────────────────────────────── -->
              <div class="flex flex-col gap-3">
                <div class="flex items-center gap-2">
                  <span class="px-2 py-0.5 bg-orange-500/20 rounded-full text-orange-500 text-[10px] font-bold font-['Unbounded']">3</span>
                  <span class="text-white text-sm font-bold font-['Unbounded']">{{ DICT.checkout.step3 }}</span>
                </div>

                <div class="flex flex-col gap-2">
                  <button
                    v-for="opt in paymentOptions"
                    :key="opt.id"
                    class="px-4 py-3 rounded-lg outline outline-1 outline-offset-[-1px] flex items-center gap-3 transition-all active:scale-[0.99]"
                    :class="
                      paymentMethod === opt.id
                        ? 'bg-orange-500/5 outline-orange-500'
                        : 'bg-white/5 outline-white/10 hover:outline-white/20'
                    "
                    @click="paymentMethod = opt.id"
                  >
                    <div
                      class="w-4 h-4 rounded-full flex items-center justify-center shrink-0"
                      :class="
                        paymentMethod === opt.id
                          ? 'outline outline-2 outline-offset-[-2px] outline-orange-500'
                          : 'border-2 border-gray-600'
                      "
                    >
                      <div v-if="paymentMethod === opt.id" class="w-2 h-2 bg-orange-500 rounded-full" />
                    </div>
                    <div class="flex flex-col text-left">
                      <span class="text-slate-200 text-sm font-semibold font-['Onest']">{{ opt.label }}</span>
                      <span class="text-gray-500 text-xs font-['Onest']">{{ opt.subtitle }}</span>
                    </div>
                  </button>
                </div>
              </div>

              <div class="h-px bg-white/5" />

              <!-- ── Quantity + Total ───────────────────────────────────────── -->
              <div class="flex flex-col gap-3">
                <div class="flex items-center justify-between">
                  <span class="text-slate-400 text-sm font-['Onest']">Кількість</span>
                  <div class="flex items-center bg-white/5 rounded-lg outline outline-1 outline-white/10">
                    <button
                      class="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-white transition-colors"
                      @click="quantity = Math.max(1, quantity - 1)"
                    >−</button>
                    <span class="w-8 text-center text-sm font-medium text-white font-['Onest']">{{ quantity }}</span>
                    <button
                      class="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-white transition-colors"
                      @click="quantity++"
                    >+</button>
                  </div>
                </div>

                <div class="flex items-center justify-between">
                  <span class="text-slate-400 text-sm font-['Onest']">Ціна за одиницю</span>
                  <span class="text-orange-400 text-sm font-semibold font-['Onest']">{{ fmt(props.groupPrice) }}</span>
                </div>

                <div class="h-px bg-white/5" />

                <div class="flex items-center justify-between">
                  <span class="text-white text-sm font-bold font-['Unbounded'] tracking-tight">ДО СПЛАТИ</span>
                  <span class="text-white text-xl font-extrabold font-['Unbounded']">{{ fmt(totalToPay) }}</span>
                </div>
              </div>

              <!-- ── Submit button ─────────────────────────────────────────── -->
              <button
                :disabled="isSubmitting"
                class="w-full py-3.5 bg-gradient-to-b from-orange-500 to-[#ea6c0a] rounded-xl shadow-[0px_4px_20px_0px_rgba(249,115,22,0.35)] text-white text-sm font-bold font-['Unbounded'] tracking-wide transition-all hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_6px_28px_0px_rgba(249,115,22,0.55)] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100"
                @click="handleSubmitOrder"
              >
                {{ isSubmitting ? 'ОБРОБКА...' : (isJoinMode ? 'ПРИЄДНАТИСЯ ДО ЗБОРУ ➔' : 'ОПЛАТИТИ ТА ВІДКРИТИ ЗБІР ➔') }}
              </button>

              <!-- Error -->
              <p v-if="submitError" class="text-red-400 text-xs font-['Onest'] text-center">{{ submitError }}</p>
            </template>
          </template>

        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
select { -webkit-appearance: none; }
textarea::-webkit-scrollbar { width: 3px; }
textarea::-webkit-scrollbar-track { background: transparent; }
textarea::-webkit-scrollbar-thumb { background: #3d4158; border-radius: 2px; }
</style>
