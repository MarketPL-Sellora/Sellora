<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { DICT } from '../../constants/dictionary'
import { useCartStore } from '../../state/cartStore'
import { useUserStore } from '../../state/userStore'
import { apiClient } from '../../api/axios'

const router = useRouter()
const cartStore = useCartStore()
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
    // Робимо два запити паралельно: методи конкретного магазину і всі доступні пошти в системі
    const [methodsResponse, carriersResponse] = await Promise.all([
      apiClient.get(`/stores/${storeId}/shipping_methods`),
      apiClient.get('/shipping_carriers')
    ])

    // 1. Дістаємо методи магазину (масив ID)
    const rawData = methodsResponse.data
    let methodsArray = []
    if (rawData && rawData.shipping_methods && Array.isArray(rawData.shipping_methods)) {
      methodsArray = rawData.shipping_methods
    } else if (Array.isArray(rawData)) {
      methodsArray = rawData
    }

    // 2. Дістаємо глобальні пошти і створюємо динамічний словник { id: name }
    const carriersData = Array.isArray(carriersResponse.data) ? carriersResponse.data : carriersResponse.data?.content || []
    const dynamicCarrierNames: Record<number, string> = {}
    carriersData.forEach((c: any) => {
      dynamicCarrierNames[c.id] = c.name
    })

    // 3. Об'єднуємо дані
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

onMounted(async () => {
  // 1. Завантажуємо кошик, якщо його немає
  if (!cartStore.cart) {
    await cartStore.fetchCart()
  }

  // 2. FIX F5: Якщо вибраних товарів немає, але кошик не пустий — автоматично виділяємо всі товари
  if (cartStore.selectedProductIds.length === 0 && cartStore.cart?.items?.length) {
    cartStore.selectedProductIds = cartStore.cart.items.map((item: any) => item.productId)
  }

  // 3. Автозаповнення Email
  if (userStore.user?.email) {
    form.email = userStore.user.email
  }

  // 4. Отримуємо правильний storeId і тягнемо пошту
  const activeStoreId = cartStore.selectedMerchantId || cartStore.cart?.items?.[0]?.merchantId
  if (activeStoreId) {
    fetchShippingMethods(activeStoreId)
  }
})

// ─── Types ────────────────────────────────────────────────────────────────────

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
  badges: number
}

interface PromoDiscount {
  code: string
  discountType: 'PERCENTAGE' | 'FIXED'
  discountValue: number
}

// ─── Contact form ─────────────────────────────────────────────────────────────

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

// ─── Delivery ─────────────────────────────────────────────────────────────────

const deliveryOptions: DeliveryOption[] = [
  { id: 'BRANCH',  label: 'Відділення',  icon: 'branch'  },
  { id: 'COURIER', label: "Кур'єр",      icon: 'courier'  },
  { id: 'PICKUP',  label: 'Самовивіз',   icon: 'pickup'   },
]

const deliveryMethod = ref<DeliveryType>('BRANCH')

// ─── Payment ──────────────────────────────────────────────────────────────────

const paymentOptions: PaymentOption[] = [
  {
    id:       'CASH_ON_DELIVERY',
    label:    'Оплата під час отримання',
    subtitle: 'Готівкою або карткою на пошті',
    badges:   2,
  },
  {
    id:       'ONLINE_CARD',
    label:    'Карткою онлайн',
    subtitle: 'Visa, Mastercard, Apple Pay, Google Pay',
    badges:   3,
  },
]

const paymentMethod = ref<PaymentMethod>('CASH_ON_DELIVERY')

// ─── Promo code ───────────────────────────────────────────────────────────────

const promoCode       = ref('')
const promoLoading    = ref(false)
const promoApplied    = ref(false)
const promoError      = ref(false)
const promoErrorMsg   = ref('')
const promoDiscount   = ref<PromoDiscount | null>(null)

async function applyPromo() {
  const code = promoCode.value.trim()
  if (!code) return

  promoLoading.value = true
  promoError.value = false
  promoErrorMsg.value = ''

  try {
    const response = await apiClient.get(`/promo_codes/by-code/${encodeURIComponent(code)}`)
    const data = response.data

    // Validate promo is active and not expired
    if (!data.is_active) {
      promoError.value = true
      promoErrorMsg.value = 'Промокод неактивний'
      promoApplied.value = false
      promoDiscount.value = null
      setTimeout(() => { promoError.value = false; promoErrorMsg.value = '' }, 3000)
      return
    }

    if (data.expires_at && new Date(data.expires_at) < new Date()) {
      promoError.value = true
      promoErrorMsg.value = 'Термін дії промокоду вийшов'
      promoApplied.value = false
      promoDiscount.value = null
      setTimeout(() => { promoError.value = false; promoErrorMsg.value = '' }, 3000)
      return
    }

    // Save valid promo
    const type = data.discount_type || data.discountType;
    const val = Number(data.value || data.discount_value);

    promoDiscount.value = {
      code: code,
      discountType: type, // 'PERCENTAGE' or 'FIXED'
      discountValue: val,
    }
    promoApplied.value = true
    promoError.value = false

  } catch (error: any) {
    promoApplied.value = false
    promoDiscount.value = null
    promoError.value = true
    promoErrorMsg.value = error.response?.data?.message || 'Промокод не знайдено'
    setTimeout(() => { promoError.value = false; promoErrorMsg.value = '' }, 3000)
  } finally {
    promoLoading.value = false
  }
}

function clearPromo() {
  promoCode.value = ''
  promoApplied.value = false
  promoError.value = false
  promoErrorMsg.value = ''
  promoDiscount.value = null
}

// ─── Computed totals ──────────────────────────────────────────────────────────

const discountAmount = computed<number>(() => {
  if (!promoDiscount.value) return 0
  const subtotal = cartStore.selectedTotalAmount || 0
  const val = Number(promoDiscount.value.discountValue) || 0

  if (promoDiscount.value.discountType === 'PERCENTAGE') {
    return Math.round((subtotal * val) / 100)
  }
  // FIXED
  return val
})

const serviceTax = computed<number>(() => {
  const subtotal = cartStore.selectedTotalAmount || 0
  const disc = discountAmount.value || 0
  const base = Math.max(0, subtotal - disc)
  return Math.round(base * 0.01)
})

const finalTotalAmount = computed<number>(() => {
  const subtotal = cartStore.selectedTotalAmount || 0
  const disc = discountAmount.value || 0
  const tax = serviceTax.value || 0
  return Math.max(0, subtotal - disc + tax)
})

// ─── Submit ───────────────────────────────────────────────────────────────────

const isSubmitting = ref(false)
const submitError  = ref('')

function buildDeliveryAddress(): Record<string, string> | null {
  if (deliveryMethod.value === 'BRANCH') {
    return { city: form.city, branch: form.branch }
  }
  if (deliveryMethod.value === 'COURIER') {
    return { city: form.city, street: form.street, house: form.house }
  }
  // PICKUP
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

  if (cartStore.selectedProductIds.length === 0) return 'Не обрано жодного товару'
  if (cartStore.selectedMerchantId === null) return 'Не визначено магазин'

  return null
}

async function handleSubmit() {
  const validationError = validateForm()
  if (validationError) {
    alert(validationError)
    return
  }

  isSubmitting.value = true
  submitError.value = ''

  // Build items array from selected products
  const selectedItems = (cartStore.cart?.items ?? [])
    .filter(item => cartStore.selectedProductIds.includes(item.productId))
    .map(item => ({ product_id: item.productId, quantity: item.quantity }))

  // Resolve store_id: prefer selectedMerchantId, fallback to first selected item's merchantId
  const resolvedStoreId = cartStore.selectedMerchantId
    ?? cartStore.cart?.items?.find(i => cartStore.selectedProductIds.includes(i.productId))?.merchantId
    ?? null

  // Resolve carrier_id: use selected carrier, fallback to 1 for delivery methods that require it
  let resolvedCarrierId: number | null = selectedCarrierId.value
  if (!resolvedCarrierId && deliveryMethod.value !== 'PICKUP') {
    resolvedCarrierId = 1 // MVP fallback
  }

  const payload = {
    store_id: resolvedStoreId,
    buyer_name: form.firstName.trim(),
    buyer_surname: form.lastName.trim(),
    buyer_phone: form.phone.trim(),
    buyer_email: form.email.trim(),
    delivery_type: deliveryMethod.value,
    carrier_id: resolvedCarrierId,
    delivery_address: buildDeliveryAddress(),
    payment_method: paymentMethod.value,
    order_comment: form.comment.trim() || null,
    promo_code: promoDiscount.value?.code || null,
    items: selectedItems,
  }

  try {
    const response = await apiClient.post('/orders/cart/checkout', payload)
    const data = response.data

    if (response.status >= 200 && response.status < 300) {
      // Clear selected items on success so they don't persist
      cartStore.selectedProductIds = []
      
      // Refresh cart (backend removes ordered items)
      await cartStore.fetchCart()

      // Handle redirect
      if (paymentMethod.value === 'ONLINE_CARD' && data.payment_url) {
        window.location.href = data.payment_url;
      } else {
        router.push('/order/' + data.id);
      }
    } else {
      throw new Error(data.message || 'Невідома помилка сервера')
    }
  } catch (error: any) {
    submitError.value = error.response?.data?.message || error.message || 'Помилка при оформленні замовлення'
    alert(submitError.value)
  } finally {
    isSubmitting.value = false
  }
}

const fmt = (n: number) => (n || 0).toLocaleString('uk-UA') + ' ₴'

defineExpose({
  discountAmount,
  promoDiscount,
})
</script>

<template>
  <div class="w-full inline-flex flex-col gap-5">

    <!-- ── STEP 1: Contacts ───────────────────────────────────────────────── -->
    <section class="self-stretch p-6 bg-gray-800 rounded-2xl flex flex-col gap-5">

      <!-- Heading -->
      <div class="inline-flex items-center gap-3">
        <span class="px-2.5 py-1 bg-orange-500/20 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/30 text-orange-500 text-xs font-bold font-['Unbounded'] leading-5 tracking-wide whitespace-nowrap">
          КРОК 1
        </span>
        <span class="text-white text-base font-bold font-['Unbounded'] leading-6 tracking-wide">
          {{ DICT.checkout.step1 }}
        </span>
      </div>

      <!-- Fields grid -->
      <div class="self-stretch grid grid-cols-1 sm:grid-cols-2 gap-4">

        <!-- First name -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            {{ DICT.forms.labels.firstName }} <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.firstName"
            type="text"
            :placeholder="DICT.forms.placeholders.firstName"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
          />
        </div>

        <!-- Last name -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            {{ DICT.forms.labels.lastName }} <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.lastName"
            type="text"
            :placeholder="DICT.forms.placeholders.lastName"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
          />
        </div>

        <!-- Phone -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            {{ DICT.forms.labels.phone }} <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.phone"
            type="tel"
            :placeholder="DICT.forms.placeholders.phone"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
          />
        </div>

        <!-- Email -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            {{ DICT.forms.labels.email }} <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.email"
            type="email"
            :placeholder="DICT.forms.placeholders.email"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
          />
        </div>

      </div>
    </section>

    <!-- ── STEP 2: Delivery ───────────────────────────────────────────────── -->
    <section class="self-stretch p-6 bg-gray-800 rounded-2xl flex flex-col gap-4">

      <!-- Heading -->
      <div class="inline-flex items-center gap-3">
        <span class="px-2.5 py-1 bg-orange-500/20 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/30 text-orange-500 text-xs font-bold font-['Unbounded'] leading-5 tracking-wide whitespace-nowrap">
          КРОК 2
        </span>
        <span class="text-white text-base font-bold font-['Unbounded'] leading-6 tracking-wide">
          {{ DICT.checkout.step2 }}
        </span>
      </div>

      <!-- Delivery method cards -->
      <div class="self-stretch pt-1 inline-flex justify-center items-start gap-3">
        <button
          v-for="opt in deliveryOptions"
          :key="opt.id"
          class="flex-1 min-h-20 px-4 py-3.5 rounded-[10px] outline outline-1 outline-offset-[-1px] flex flex-col justify-center items-center gap-2 transition-all duration-150 active:scale-[0.98] focus:outline-none"
          :class="
            deliveryMethod === opt.id
              ? 'bg-orange-500/10 outline-orange-500'
              : 'bg-gray-900 outline-gray-700 hover:outline-gray-500 hover:bg-gray-900/70'
          "
          @click="deliveryMethod = opt.id"
        >
          <!-- Branch icon -->
          <svg v-if="opt.icon === 'branch'" class="w-5 h-5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" :class="deliveryMethod === opt.id ? 'text-orange-500' : 'text-gray-400'">
            <rect x="2.5" y="3" width="10" height="14" rx="1.5"/>
            <circle cx="7.5" cy="10" r="2"/>
            <path d="M16 6l-3 4 3 4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>

          <!-- Courier icon -->
          <svg v-else-if="opt.icon === 'courier'" class="w-5 h-5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" :class="deliveryMethod === opt.id ? 'text-orange-500' : 'text-gray-400'">
            <rect x="1" y="6" width="12" height="8" rx="1.5"/>
            <path d="M13 9h3l3 4v3h-6V9z" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="4.5" cy="16.5" r="1.5"/><circle cx="14.5" cy="16.5" r="1.5"/>
          </svg>

          <!-- Pickup icon -->
          <svg v-else-if="opt.icon === 'pickup'" class="w-5 h-5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" :class="deliveryMethod === opt.id ? 'text-orange-500' : 'text-gray-400'">
            <rect x="3" y="2" width="14" height="16" rx="1.5"/>
            <path d="M3 6h14" stroke-linecap="round"/>
            <path d="M8 10h4M10 8v4" stroke-linecap="round"/>
          </svg>

          <span
            class="text-sm font-medium font-['Onest'] leading-5 transition-colors duration-150"
            :class="deliveryMethod === opt.id ? 'text-orange-500' : 'text-gray-400'"
          >
            {{ opt.label }}
          </span>
        </button>
      </div>

      <!-- Shipping carrier selection (before address fields, only for BRANCH/COURIER) -->
      <div v-if="deliveryMethod === 'BRANCH'" class="self-stretch pt-2 flex flex-col gap-3 mb-4">

        <!-- Shipping carrier cards -->
        <div v-if="shippingMethodsLoading" class="text-gray-500 text-sm font-['Onest'] animate-pulse py-2">
          Завантаження служб доставки...
        </div>

        <template v-else-if="shippingMethods.length > 0">
          <span class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            Оберіть службу доставки <span class="text-orange-500">*</span>
          </span>
          <div class="flex flex-col gap-2">
            <label
              v-for="method in shippingMethods"
              :key="method.id"
              class="flex items-center gap-3 px-4 py-3 rounded-[10px] outline outline-1 outline-offset-[-1px] cursor-pointer transition-all duration-150"
              :class="
                selectedCarrierId === method.id
                  ? 'bg-orange-500/10 outline-orange-500'
                  : 'bg-gray-900 outline-gray-700 hover:outline-gray-500 hover:bg-gray-900/70'
              "
            >
              <input
                type="radio"
                name="shipping-carrier"
                :value="method.id"
                v-model="selectedCarrierId"
                class="sr-only"
              />
              <!-- Radio circle -->
              <div
                class="w-5 h-5 rounded-full flex items-center justify-center transition-all duration-150 shrink-0"
                :class="
                  selectedCarrierId === method.id
                    ? 'outline outline-2 outline-offset-[-2px] outline-orange-500'
                    : 'border-2 border-gray-600'
                "
              >
                <div
                  v-if="selectedCarrierId === method.id"
                  class="w-2.5 h-2.5 bg-orange-500 rounded-full"
                />
              </div>
              <span
                class="text-sm font-medium font-['Onest'] leading-5 transition-colors duration-150"
                :class="selectedCarrierId === method.id ? 'text-orange-500' : 'text-gray-300'"
              >
                {{ method.name }}
              </span>
            </label>
          </div>
        </template>
        <div v-else class="px-4 py-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm font-['Onest'] text-center">
          Немає доступних служб доставки для цього магазину
        </div>
      </div>

      <!-- Dynamic delivery fields -->
      <div v-if="deliveryMethod !== 'PICKUP'" class="self-stretch pt-1 grid grid-cols-1 sm:grid-cols-2 gap-4">

        <!-- City autocomplete (BRANCH & COURIER) -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            {{ DICT.forms.labels.city }} <span class="text-orange-500">*</span>
          </label>
          <div class="relative">
            <input
              v-model="cityQuery"
              type="text"
              autocomplete="off"
              :placeholder="DICT.forms.placeholders.selectCity"
              class="w-full h-11 px-3.5 pr-10 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
              @input="onCityInput"
              @focus="isCityDropdownOpen = true"
              @blur="closeCityDropdown"
            />
            <svg class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
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
                class="px-3.5 py-2.5 text-sm text-gray-200 font-['Onest'] cursor-pointer transition-colors hover:bg-gray-700 hover:text-orange-400"
                :class="form.city === city ? 'bg-gray-700/60 text-orange-400' : ''"
                @mousedown.prevent="selectCity(city)"
              >
                {{ city }}
              </li>
            </ul>
          </div>
        </div>

        <!-- Branch autocomplete (BRANCH only) -->
        <div v-if="deliveryMethod === 'BRANCH'" class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            {{ DICT.forms.labels.branch }} <span class="text-orange-500">*</span>
          </label>
          <div class="relative">
            <input
              v-model="branchQuery"
              type="text"
              autocomplete="off"
              :placeholder="DICT.forms.placeholders.selectBranch"
              class="w-full h-11 px-3.5 pr-10 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none"
              @input="onBranchInput"
              @focus="isBranchDropdownOpen = true"
              @blur="closeBranchDropdown"
            />
            <svg class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
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
                class="px-3.5 py-2.5 text-sm text-gray-200 font-['Onest'] cursor-pointer transition-colors hover:bg-gray-700 hover:text-orange-400"
                :class="form.branch === branch ? 'bg-gray-700/60 text-orange-400' : ''"
                @mousedown.prevent="selectBranch(branch)"
              >
                {{ branch }}
              </li>
            </ul>
          </div>
        </div>

        <!-- Street (COURIER only) -->
        <div v-if="deliveryMethod === 'COURIER'" class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            Адреса (вулиця) <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.street"
            type="text"
            placeholder="Наприклад: вул. Хрещатик"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
          />
        </div>

        <!-- House (COURIER only) — second row -->
        <div v-if="deliveryMethod === 'COURIER'" class="flex flex-col gap-1.5 sm:col-span-2">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            Будинок / квартира <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.house"
            type="text"
            placeholder="Наприклад: 12, кв. 5"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
          />
        </div>
      </div>


      <!-- Comment (always visible) -->
      <div class="self-stretch flex flex-col gap-1.5">
        <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
          {{ DICT.forms.labels.comment }}
        </label>
        <textarea
          v-model="form.comment"
          rows="3"
          :placeholder="DICT.forms.placeholders.comment"
          class="w-full min-h-20 px-3.5 pt-2 pb-3 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 resize-y transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
        />
      </div>
    </section>

    <!-- ── STEP 3: Payment ────────────────────────────────────────────────── -->
    <section class="self-stretch p-6 bg-gray-800 rounded-2xl flex flex-col gap-5">

      <!-- Heading -->
      <div class="inline-flex items-center gap-3">
        <span class="px-2.5 py-1 bg-orange-500/20 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/30 text-orange-500 text-xs font-bold font-['Unbounded'] leading-5 tracking-wide whitespace-nowrap">
          КРОК 3
        </span>
        <span class="text-white text-base font-bold font-['Unbounded'] leading-6 tracking-wide">
          {{ DICT.checkout.step3 }}
        </span>
      </div>

      <!-- Payment options -->
      <div class="self-stretch flex flex-col gap-3">
        <button
          v-for="opt in paymentOptions"
          :key="opt.id"
          class="self-stretch px-4 py-4 rounded-[10px] outline outline-1 outline-offset-[-1px] inline-flex justify-between items-center transition-all duration-150 active:scale-[0.99] focus:outline-none group"
          :class="
            paymentMethod === opt.id
              ? 'bg-orange-500/5 outline-orange-500'
              : 'bg-gray-900 outline-gray-700 hover:outline-gray-500'
          "
          @click="paymentMethod = opt.id"
        >
          <div class="flex items-center gap-3">
            <!-- Radio circle -->
            <div
              class="w-5 h-5 rounded-full flex items-center justify-center transition-all duration-150 shrink-0"
              :class="
                paymentMethod === opt.id
                  ? 'outline outline-2 outline-offset-[-2px] outline-orange-500'
                  : 'border-2 border-gray-600 group-hover:border-gray-400'
              "
            >
              <div
                v-if="paymentMethod === opt.id"
                class="w-2.5 h-2.5 bg-orange-500 rounded-full"
              />
            </div>

            <!-- Text -->
            <div class="flex flex-col gap-0.5 text-left">
              <span class="text-gray-200 text-sm font-bold font-['Onest'] leading-5">{{ opt.label }}</span>
              <span class="text-gray-500 text-xs font-medium font-['Onest'] leading-4">{{ opt.subtitle }}</span>
            </div>
          </div>
        </button>
      </div>

    </section>

    <!-- ── STEP 4: Promo code ─────────────────────────────────────────────── -->
    <section class="self-stretch p-6 bg-gray-800 rounded-2xl flex flex-col gap-4">

      <!-- Heading -->
      <div class="inline-flex items-center gap-3">
        <span class="px-2.5 py-1 bg-orange-500/20 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/30 text-orange-500 text-xs font-bold font-['Unbounded'] leading-5 tracking-wide whitespace-nowrap">
          КРОК 4
        </span>
        <span class="text-white text-base font-bold font-['Unbounded'] leading-6 tracking-wide">
          ПРОМОКОД
        </span>
      </div>

      <div class="self-stretch inline-flex justify-start items-stretch gap-2">
        <div class="flex-1 relative">
          <input
            v-model="promoCode"
            type="text"
            placeholder="Введіть промокод"
            maxlength="24"
            :disabled="promoApplied"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-2 disabled:opacity-60"
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
          v-if="!promoApplied"
          class="h-10 px-4 rounded-lg text-sm font-medium font-['Onest'] leading-5 transition-all duration-150 active:scale-95 focus:outline-none bg-gray-700 text-gray-300 hover:bg-gray-600 hover:text-white active:bg-gray-700 disabled:opacity-50"
          :disabled="promoLoading || !promoCode.trim()"
          @click="applyPromo"
        >
          {{ promoLoading ? '...' : 'Застосувати' }}
        </button>
        <button
          v-else
          class="h-10 px-4 rounded-lg text-sm font-medium font-['Onest'] leading-5 transition-all duration-150 active:scale-95 focus:outline-none bg-green-500/20 text-green-400 outline outline-1 outline-green-500/30 hover:bg-red-500/20 hover:text-red-400 hover:outline-red-500/30"
          @click="clearPromo"
        >
          Скасувати
        </button>
      </div>

      <!-- Error message -->
      <p v-if="promoError && promoErrorMsg" class="text-red-400 text-xs font-medium -mt-2">{{ promoErrorMsg }}</p>

      <!-- Applied promo info -->
      <div v-if="promoApplied && promoDiscount" class="flex items-center gap-2 px-3 py-2 bg-green-500/10 rounded-lg border border-green-500/20">
        <svg class="w-4 h-4 text-green-400 shrink-0" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M3 8l4 4 6-6" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span class="text-green-400 text-sm font-medium font-['Onest']">
          Промокод <span class="font-bold">{{ promoDiscount.code }}</span> застосовано:
          <span v-if="promoDiscount.discountType === 'PERCENTAGE'">−{{ promoDiscount.discountValue }}%</span>
          <span v-else>−{{ fmt(promoDiscount.discountValue) }}</span>
        </span>
      </div>
    </section>

    <!-- ── Summary & Submit ───────────────────────────────────────────────── -->
    <section class="self-stretch p-6 bg-gray-800 rounded-2xl flex flex-col gap-4">

      <!-- Price breakdown -->
      <div class="flex flex-col gap-2">
        <div class="flex justify-between items-center">
          <span class="text-gray-400 text-sm font-normal font-['Onest']">Вартість обраних товарів</span>
          <span class="text-gray-200 text-sm font-normal font-['Onest']">{{ fmt(cartStore.selectedTotalAmount) }}</span>
        </div>
        <div v-if="promoDiscount" class="flex justify-between items-center">
          <span class="text-gray-400 text-sm font-normal font-['Onest']">Знижка за промокодом</span>
          <span class="text-green-400 text-sm font-semibold font-['Onest']">− {{ fmt(discountAmount) }}</span>
        </div>
        <div class="flex justify-between items-center">
          <span class="text-gray-400 text-sm font-normal font-['Onest']">Комісія сервісу (1%)</span>
          <span class="text-gray-200 text-sm font-normal font-['Onest']">{{ fmt(serviceTax) }}</span>
        </div>
      </div>

      <div class="self-stretch h-px border-t border-gray-700" />

      <!-- Total -->
      <div class="flex justify-between items-center">
        <span class="text-gray-300 text-sm font-bold font-['Unbounded'] tracking-tight">РАЗОМ ДО СПЛАТИ</span>
        <span class="text-white text-2xl font-extrabold font-['Unbounded']">{{ fmt(finalTotalAmount) }}</span>
      </div>

      <!-- Submit button -->
      <button
        class="self-stretch px-6 py-4 bg-gradient-to-b from-orange-500 to-[#ea6c0a] rounded-xl shadow-[0px_4px_20px_0px_rgba(249,115,22,0.35)] inline-flex justify-center items-center transition-all duration-150 hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_6px_28px_0px_rgba(249,115,22,0.55)] active:scale-[0.98] active:shadow-none focus:outline-none focus:ring-2 focus:ring-orange-500/50 focus:ring-offset-2 focus:ring-offset-gray-800 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:from-orange-500 disabled:hover:to-[#ea6c0a] disabled:active:scale-100"
        :disabled="isSubmitting || cartStore.selectedProductIds.length === 0"
        @click="handleSubmit"
      >
        <span class="text-white text-base font-bold font-['Unbounded'] leading-6 tracking-wide">
          {{ isSubmitting ? 'Обробка...' : 'Оформити замовлення ➔' }}
        </span>
      </button>

      <!-- Selected items info -->
      <p v-if="cartStore.selectedProductIds.length === 0" class="text-center text-gray-500 text-xs">
        Поверніться до кошика та оберіть товари для замовлення
      </p>
    </section>

  </div>
</template>

<style scoped>
/* Scrollbar styling for textarea and autocomplete dropdowns */
textarea::-webkit-scrollbar,
ul::-webkit-scrollbar { width: 4px; }
textarea::-webkit-scrollbar-track,
ul::-webkit-scrollbar-track { background: transparent; }
textarea::-webkit-scrollbar-thumb,
ul::-webkit-scrollbar-thumb { background: #3d4158; border-radius: 2px; }
/* Shake animation for promo error */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%       { transform: translateX(-4px); }
  40%       { transform: translateX(4px); }
  60%       { transform: translateX(-3px); }
  80%       { transform: translateX(3px); }
}
.animate-shake { animation: shake 0.35s ease; }
</style>
