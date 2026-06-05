<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { DICT } from '../../constants/dictionary'
import { useCartStore } from '../../state/cartStore'
import { apiClient } from '../../api/axios'

const router = useRouter()
const cartStore = useCartStore()

onMounted(() => {
  if (!cartStore.cart) {
    cartStore.fetchCart()
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
    promoDiscount.value = {
      code: code,
      discountType: data.discount_type, // 'PERCENTAGE' or 'FIXED'
      discountValue: data.discount_value,
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
  const base = cartStore.selectedTotalAmount

  if (promoDiscount.value.discountType === 'PERCENTAGE') {
    return Math.round(base * promoDiscount.value.discountValue / 100)
  }
  // FIXED
  return Math.min(promoDiscount.value.discountValue, base)
})

const finalTotalAmount = computed<number>(() => {
  return Math.max(0, cartStore.selectedTotalAmount - discountAmount.value)
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

  const payload = {
    store_id: cartStore.selectedMerchantId,
    buyer_name: form.firstName.trim(),
    buyer_surname: form.lastName.trim(),
    buyer_phone: form.phone.trim(),
    buyer_email: form.email.trim(),
    delivery_type: deliveryMethod.value,
    carrier_id: null,
    delivery_address: buildDeliveryAddress(),
    payment_method: paymentMethod.value,
    order_comment: form.comment.trim() || null,
    promo_code: promoDiscount.value?.code || null,
    items: selectedItems,
  }

  try {
    const response = await apiClient.post('/orders/cart/checkout', payload)
    const data = response.data

    // Refresh cart (backend removes ordered items)
    await cartStore.fetchCart()

    // Handle redirect
    if (paymentMethod.value === 'ONLINE_CARD' && data.payment_url) {
      window.location.href = data.payment_url
    } else {
      router.push('/order/' + data.id)
    }
  } catch (error: any) {
    submitError.value = error.response?.data?.message || 'Помилка при оформленні замовлення'
    alert(submitError.value)
  } finally {
    isSubmitting.value = false
  }
}

const fmt = (n: number) => (n || 0).toLocaleString('uk-UA') + ' ₴'
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

      <!-- Dynamic delivery fields -->
      <div v-if="deliveryMethod !== 'PICKUP'" class="self-stretch pt-1 grid grid-cols-1 sm:grid-cols-2 gap-4">

        <!-- City (BRANCH & COURIER) -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            {{ DICT.forms.labels.city }} <span class="text-orange-500">*</span>
          </label>
          <div class="relative">
            <select
              v-model="form.city"
              class="w-full appearance-none h-11 pl-3.5 pr-10 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none cursor-pointer"
            >
              <option value="" disabled>{{ DICT.forms.placeholders.selectCity }}</option>
              <option>Київ</option>
              <option>Харків</option>
              <option>Одеса</option>
              <option>Дніпро</option>
              <option>Львів</option>
            </select>
            <!-- Chevron -->
            <svg class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 6l4 4 4-4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>

        <!-- Branch (BRANCH only) -->
        <div v-if="deliveryMethod === 'BRANCH'" class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            {{ DICT.forms.labels.branch }} <span class="text-orange-500">*</span>
          </label>
          <div class="relative">
            <select
              v-model="form.branch"
              class="w-full appearance-none h-11 pl-3.5 pr-10 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none cursor-pointer"
            >
              <option value="" disabled>{{ DICT.forms.placeholders.selectBranch }}</option>
              <option>Відділення №1</option>
              <option>Відділення №2</option>
              <option>Поштомат №100</option>
            </select>
            <svg class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 6l4 4 4-4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
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

          <!-- Card badges -->
          <div class="flex items-start gap-[4.80px] shrink-0">
            <div
              v-for="i in opt.badges"
              :key="i"
              class="w-8 h-5 bg-gray-700 rounded border border-gray-600"
            />
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
      </div>

      <div class="self-stretch h-px border-t border-gray-700" />

      <!-- Total -->
      <div class="flex justify-between items-center">
        <span class="text-gray-300 text-sm font-bold font-['Unbounded'] tracking-tight">ДО СПЛАТИ</span>
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
/* Remove default select styling on webkit */
select {
  -webkit-appearance: none;
}
/* Scrollbar styling for textarea */
textarea::-webkit-scrollbar { width: 4px; }
textarea::-webkit-scrollbar-track { background: transparent; }
textarea::-webkit-scrollbar-thumb { background: #3d4158; border-radius: 2px; }
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
