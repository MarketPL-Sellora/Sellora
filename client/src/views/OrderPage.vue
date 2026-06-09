<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/layout/Header.vue'
import Footer from '../components/layout/Footer.vue'
import { apiClient } from '../api/axios'

const route = useRoute()
const router = useRouter()

// ─── Types ────────────────────────────────────────────────────────────────────

interface OrderItem {
  id: number
  product_id: number
  title_snapshot: string
  price_snapshot?: number
  unit_price?: number
  unitPrice?: number
  total_price?: number
  totalPrice?: number
  image_snapshot: string | null
  quantity: number
  subtotal: number
}

interface Order {
  id: number
  store_id: number
  store_name: string
  buyer_name: string
  buyer_surname: string
  buyer_phone: string
  buyer_email: string
  delivery_type: string
  delivery_address: Record<string, string> | null
  payment_method: string
  payment_status: string
  payment_url: string | null
  shipping_status: string
  order_comment: string | null
  promo_code: string | null
  subtotal: number
  discount: number
  tax: number
  total_amount: number
  items: OrderItem[]
  created_at: string
  updated_at: string
}

// ─── State ────────────────────────────────────────────────────────────────────

const order = ref<Order | null>(null)
const isLoading = ref(true)
const error = ref<string | null>(null)

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmt = (n: number) => (n || 0).toLocaleString('uk-UA') + ' ₴'

function formatDate(iso: string): string {
  const d = new Date(iso)
  return d.toLocaleDateString('uk-UA', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// ─── Status helpers ───────────────────────────────────────────────────────────

const paymentStatusMap: Record<string, { label: string; color: string; bg: string; border: string }> = {
  PAID:     { label: 'Оплачено',          color: 'text-emerald-400', bg: 'bg-emerald-500/15', border: 'border-emerald-500/25' },
  PENDING:  { label: 'Очікує оплати',     color: 'text-amber-400',   bg: 'bg-amber-500/15',   border: 'border-amber-500/25'   },
  FAILED:   { label: 'Помилка оплати',    color: 'text-red-400',     bg: 'bg-red-500/15',     border: 'border-red-500/25'     },
  REFUNDED: { label: 'Повернення коштів', color: 'text-sky-400',     bg: 'bg-sky-500/15',     border: 'border-sky-500/25'     },
}

const shippingStatusMap: Record<string, { label: string; color: string; bg: string; border: string }> = {
  PENDING:    { label: 'Очікує відправки', color: 'text-amber-400',   bg: 'bg-amber-500/15',   border: 'border-amber-500/25'   },
  SHIPPED:    { label: 'Відправлено',      color: 'text-sky-400',     bg: 'bg-sky-500/15',     border: 'border-sky-500/25'     },
  DELIVERED:  { label: 'Доставлено',       color: 'text-emerald-400', bg: 'bg-emerald-500/15', border: 'border-emerald-500/25' },
  RETURNED:   { label: 'Повернення',       color: 'text-red-400',     bg: 'bg-red-500/15',     border: 'border-red-500/25'     },
  PROCESSING: { label: 'В обробці',        color: 'text-violet-400',  bg: 'bg-violet-500/15',  border: 'border-violet-500/25'  },
}

const deliveryTypeMap: Record<string, string> = {
  BRANCH:  'Відділення',
  COURIER: "Кур'єрська доставка",
  PICKUP:  'Самовивіз',
}

const paymentMethodMap: Record<string, string> = {
  CASH_ON_DELIVERY: 'Оплата при отриманні',
  ONLINE_CARD:      'Карткою онлайн',
}

function getPaymentBadge(status: string) {
  return paymentStatusMap[status] ?? { label: status, color: 'text-gray-400', bg: 'bg-gray-500/15', border: 'border-gray-500/25' }
}
function getShippingBadge(status: string) {
  return shippingStatusMap[status] ?? { label: status, color: 'text-gray-400', bg: 'bg-gray-500/15', border: 'border-gray-500/25' }
}

// ─── Delivery address formatted ───────────────────────────────────────────────

const deliveryAddressFormatted = computed(() => {
  const addr = order.value?.delivery_address
  if (!addr) return null
  const parts: string[] = []
  if (addr.city) parts.push(addr.city)
  if (addr.branch) parts.push(addr.branch)
  if (addr.street) parts.push(addr.street)
  if (addr.house) parts.push(addr.house)
  return parts.join(', ')
})

// ─── Fetch ────────────────────────────────────────────────────────────────────

onMounted(async () => {
  const orderId = route.params.id
  if (!orderId) {
    error.value = 'ID замовлення не вказано'
    isLoading.value = false
    return
  }

  try {
    const response = await apiClient.get<Order>(`/orders/${orderId}`)
    order.value = response.data
  } catch (err: any) {
    if (err.response?.status === 404) {
      error.value = 'Замовлення не знайдено'
    } else if (!err.isHandled) {
      error.value = err.response?.data?.message || 'Помилка завантаження замовлення'
    }
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header :showCategories="false" />

    <main class="flex-1 w-full max-w-[1140px] mx-auto px-4 md:px-6 py-8 md:py-12 flex flex-col gap-6">

      <!-- ── Back button ──────────────────────────────────────────────────── -->
      <button
        class="inline-flex items-center gap-2 text-gray-400 hover:text-orange-400 text-sm font-medium font-['Onest'] transition-colors w-fit"
        @click="router.push('/')"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
        На головну
      </button>

      <!-- ── Loading skeleton ─────────────────────────────────────────────── -->
      <template v-if="isLoading">
        <div class="flex flex-col gap-6 animate-pulse">
          <div class="h-10 w-72 bg-gray-800 rounded-xl" />
          <div class="h-6 w-48 bg-gray-800 rounded-lg" />
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="h-48 bg-gray-800 rounded-2xl" />
            <div class="h-48 bg-gray-800 rounded-2xl" />
          </div>
          <div class="h-64 bg-gray-800 rounded-2xl" />
          <div class="h-40 bg-gray-800 rounded-2xl" />
        </div>
      </template>

      <!-- ── Error state ──────────────────────────────────────────────────── -->
      <template v-else-if="error">
        <div class="flex flex-col items-center justify-center py-20 gap-5">
          <div class="w-20 h-20 bg-red-500/10 rounded-full flex items-center justify-center">
            <svg class="w-10 h-10 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
            </svg>
          </div>
          <h2 class="text-xl font-bold font-['Unbounded'] text-gray-200">{{ error }}</h2>
          <p class="text-gray-500 text-sm">Перевірте посилання або поверніться на головну</p>
          <button
            class="mt-2 px-6 py-3 bg-orange-500 hover:bg-orange-400 rounded-xl text-sm font-bold font-['Unbounded'] transition-colors"
            @click="router.push('/')"
          >
            На головну
          </button>
        </div>
      </template>

      <!-- ── Order content ────────────────────────────────────────────────── -->
      <template v-else-if="order">

        <!-- ── Header ───────────────────────────────────────────────────── -->
        <section class="p-6 md:p-8 bg-[#1c1f2a] rounded-2xl border border-gray-800/50 flex flex-col gap-4">
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div class="flex flex-col gap-1">
              <h1 class="text-2xl md:text-3xl font-bold font-['Unbounded'] tracking-tight">
                Замовлення <span class="text-orange-500">#{{ order.id }}</span>
              </h1>
              <span class="text-gray-500 text-sm font-['Onest']">
                {{ formatDate(order.created_at) }}
              </span>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <!-- Payment badge -->
              <span
                class="px-3 py-1.5 rounded-lg text-xs font-bold uppercase tracking-wider border"
                :class="[getPaymentBadge(order.payment_status).color, getPaymentBadge(order.payment_status).bg, getPaymentBadge(order.payment_status).border]"
              >
                {{ getPaymentBadge(order.payment_status).label }}
              </span>
              <!-- Shipping badge -->
              <span
                class="px-3 py-1.5 rounded-lg text-xs font-bold uppercase tracking-wider border"
                :class="[getShippingBadge(order.shipping_status).color, getShippingBadge(order.shipping_status).bg, getShippingBadge(order.shipping_status).border]"
              >
                {{ getShippingBadge(order.shipping_status).label }}
              </span>
            </div>
          </div>

          <!-- Store name -->
          <div v-if="order.store_name" class="inline-flex items-center gap-2 text-gray-400 text-sm">
            <svg class="w-4 h-4 text-gray-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 21v-7.5a.75.75 0 01.75-.75h3a.75.75 0 01.75.75V21m-4.5 0H2.36m11.14 0H18m0 0h3.64m-1.39 0V9.349m-16.5 11.65V9.35m0 0a3.001 3.001 0 003.75-.615A2.993 2.993 0 009.75 9.75c.896 0 1.7-.393 2.25-1.016a2.993 2.993 0 002.25 1.016c.896 0 1.7-.393 2.25-1.016a3.001 3.001 0 003.75.614m-16.5 0a3.004 3.004 0 01-.621-4.72L4.318 3.44A1.5 1.5 0 015.378 3h13.243a1.5 1.5 0 011.06.44l1.19 1.189a3 3 0 01-.621 4.72m-13.5 8.65h3.75a.75.75 0 00.75-.75V13.5a.75.75 0 00-.75-.75H6.75a.75.75 0 00-.75.75v3.75c0 .415.336.75.75.75z" />
            </svg>
            Магазин: <span class="text-gray-300 font-medium">{{ order.store_name }}</span>
          </div>
        </section>

        <!-- ── Info grid ────────────────────────────────────────────────── -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

          <!-- Buyer info -->
          <section class="p-6 bg-[#1c1f2a] rounded-2xl border border-gray-800/50 flex flex-col gap-4">
            <h3 class="text-sm font-bold font-['Unbounded'] text-gray-300 uppercase tracking-wider flex items-center gap-2">
              <svg class="w-4 h-4 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
              </svg>
              Покупець
            </h3>
            <div class="flex flex-col gap-2.5">
              <div class="flex justify-between items-center">
                <span class="text-gray-500 text-sm">Ім'я</span>
                <span class="text-gray-200 text-sm font-medium">{{ order.buyer_name }} {{ order.buyer_surname }}</span>
              </div>
              <div class="h-px bg-gray-800" />
              <div class="flex justify-between items-center">
                <span class="text-gray-500 text-sm">Телефон</span>
                <span class="text-gray-200 text-sm font-medium">{{ order.buyer_phone }}</span>
              </div>
              <div class="h-px bg-gray-800" />
              <div class="flex justify-between items-center">
                <span class="text-gray-500 text-sm">Email</span>
                <span class="text-gray-200 text-sm font-medium">{{ order.buyer_email }}</span>
              </div>
              <div class="h-px bg-gray-800" />
              <div class="flex justify-between items-center">
                <span class="text-gray-500 text-sm">Оплата</span>
                <span class="text-gray-200 text-sm font-medium">{{ paymentMethodMap[order.payment_method] || order.payment_method }}</span>
              </div>
            </div>
          </section>

          <!-- Delivery info -->
          <section class="p-6 bg-[#1c1f2a] rounded-2xl border border-gray-800/50 flex flex-col gap-4">
            <h3 class="text-sm font-bold font-['Unbounded'] text-gray-300 uppercase tracking-wider flex items-center gap-2">
              <svg class="w-4 h-4 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 18.75a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 01-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 00-3.213-9.193 2.056 2.056 0 00-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 00-10.026 0 1.106 1.106 0 00-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12" />
              </svg>
              Доставка
            </h3>
            <div class="flex flex-col gap-2.5">
              <div class="flex justify-between items-center">
                <span class="text-gray-500 text-sm">Тип</span>
                <span class="text-gray-200 text-sm font-medium">{{ deliveryTypeMap[order.delivery_type] || order.delivery_type }}</span>
              </div>
              <template v-if="deliveryAddressFormatted">
                <div class="h-px bg-gray-800" />
                <div class="flex justify-between items-start gap-4">
                  <span class="text-gray-500 text-sm shrink-0">Адреса</span>
                  <span class="text-gray-200 text-sm font-medium text-right">{{ deliveryAddressFormatted }}</span>
                </div>
              </template>
              <template v-if="order.order_comment">
                <div class="h-px bg-gray-800" />
                <div class="flex flex-col gap-1">
                  <span class="text-gray-500 text-sm">Коментар</span>
                  <span class="text-gray-300 text-sm italic">{{ order.order_comment }}</span>
                </div>
              </template>
            </div>
          </section>
        </div>

        <!-- ── Items ────────────────────────────────────────────────────── -->
        <section class="p-6 md:p-8 bg-[#1c1f2a] rounded-2xl border border-gray-800/50 flex flex-col gap-5">
          <h3 class="text-sm font-bold font-['Unbounded'] text-gray-300 uppercase tracking-wider flex items-center gap-2">
            <svg class="w-4 h-4 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 10-7.5 0v4.5m11.356-1.993l1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 01-1.12-1.243l1.264-12A1.125 1.125 0 015.513 7.5h12.974c.576 0 1.059.435 1.119 1.007zM8.625 10.5a.375.375 0 11-.75 0 .375.375 0 01.75 0zm7.5 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
            </svg>
            Товари ({{ order.items.length }})
          </h3>

          <div class="flex flex-col divide-y divide-gray-800/70">
            <div
              v-for="item in order.items"
              :key="item.id"
              class="py-4 first:pt-0 last:pb-0 flex items-center gap-4 cursor-pointer transition-colors hover:bg-white/5 rounded-xl p-2 -m-2"
              @click="router.push('/product/' + item.product_id)"
            >
              <!-- Image -->
              <div class="w-16 h-16 md:w-20 md:h-20 shrink-0 bg-gray-800 rounded-xl overflow-hidden flex items-center justify-center">
                <img
                  v-if="item.image_snapshot"
                  :src="item.image_snapshot"
                  :alt="item.title_snapshot"
                  class="w-full h-full object-contain p-1.5"
                  loading="lazy"
                />
                <svg v-else class="w-8 h-8 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5a1.5 1.5 0 001.5-1.5V5.25a1.5 1.5 0 00-1.5-1.5H3.75a1.5 1.5 0 00-1.5 1.5v14.25c0 .828.672 1.5 1.5 1.5z" />
                </svg>
              </div>

              <!-- Info -->
              <div class="flex-1 min-w-0 flex flex-col gap-1">
                <span class="text-gray-200 text-sm font-semibold leading-5 line-clamp-2">
                  {{ item.title_snapshot }}
                </span>
                <span class="text-gray-500 text-xs">
                  {{ item.quantity }} × {{ fmt(item.unit_price ?? item.unitPrice ?? 0) }}
                </span>
              </div>

              <!-- Item subtotal -->
              <span class="text-white text-sm md:text-base font-bold font-['Onest'] whitespace-nowrap shrink-0">
                {{ fmt(item.total_price ?? item.totalPrice ?? ((item.unit_price ?? item.unitPrice ?? 0) * item.quantity) ?? item.subtotal ?? 0) }}
              </span>
            </div>
          </div>
        </section>

        <!-- ── Financial summary ─────────────────────────────────────────── -->
        <section class="p-6 md:p-8 bg-[#1c1f2a] rounded-2xl border border-gray-800/50 flex flex-col gap-4">
          <h3 class="text-sm font-bold font-['Unbounded'] text-gray-300 uppercase tracking-wider flex items-center gap-2">
            <svg class="w-4 h-4 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 18.75a60.07 60.07 0 0115.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 013 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 00-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 01-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 003 15h-.75M15 10.5a3 3 0 11-6 0 3 3 0 016 0zm3 0h.008v.008H18V10.5zm-12 0h.008v.008H6V10.5z" />
            </svg>
            Фінансовий підсумок
          </h3>

          <div class="flex flex-col gap-3">
            <!-- Subtotal -->
            <div class="flex justify-between items-center">
              <span class="text-gray-400 text-sm">Вартість товарів</span>
              <span class="text-gray-200 text-sm font-medium">{{ fmt(order.subtotal) }}</span>
            </div>
            <!-- Discount -->
            <div v-if="order.discount > 0" class="flex justify-between items-center">
              <span class="text-gray-400 text-sm">Знижка</span>
              <span class="text-green-400 text-sm font-semibold">− {{ fmt(order.discount) }}</span>
            </div>
            <!-- Promo code -->
            <div v-if="order.promo_code" class="flex justify-between items-center">
              <span class="text-gray-400 text-sm">Промокод</span>
              <span class="text-orange-400 text-sm font-medium">{{ order.promo_code }}</span>
            </div>
            <!-- Tax/commission -->
            <div v-if="order.tax > 0" class="flex justify-between items-center">
              <span class="text-gray-400 text-sm">Комісія сервісу (1%)</span>
              <span class="text-gray-200 text-sm font-medium">{{ fmt(order.tax) }}</span>
            </div>

            <div class="h-px bg-gray-700 my-1" />

            <!-- Total -->
            <div class="flex justify-between items-center">
              <span class="text-gray-300 text-sm font-bold font-['Unbounded'] tracking-tight">РАЗОМ ДО СПЛАТИ</span>
              <span class="text-white text-2xl font-extrabold font-['Unbounded']">{{ fmt(order.total_amount) }}</span>
            </div>
          </div>
        </section>

        <!-- ── Pay button (if pending online payment) ─────────────────────── -->
        <div v-if="order.payment_status === 'PENDING' && order.payment_method === 'ONLINE_CARD' && order.payment_url" class="flex justify-center">
          <a
            :href="order.payment_url"
            class="px-8 py-4 bg-gradient-to-b from-orange-500 to-[#ea6c0a] rounded-xl shadow-[0px_4px_20px_0px_rgba(249,115,22,0.35)] text-white text-base font-bold font-['Unbounded'] leading-6 tracking-wide transition-all duration-150 hover:from-orange-400 hover:to-orange-500 hover:shadow-[0px_6px_28px_0px_rgba(249,115,22,0.55)] active:scale-[0.98]"
          >
            Оплатити замовлення ➔
          </a>
        </div>

      </template>

    </main>

    <Footer />
  </div>
</template>
