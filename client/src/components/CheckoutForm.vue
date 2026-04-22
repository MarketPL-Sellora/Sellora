<script setup lang="ts">
import { ref, reactive } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface DeliveryOption {
  id: string
  label: string
  icon: 'nova-poshta' | 'courier' | 'pickup'
}

interface PaymentOption {
  id: string
  label: string
  subtitle: string
  badges: number
}

// ─── Contact form ─────────────────────────────────────────────────────────────

const form = reactive({
  firstName: '',
  lastName:  '',
  phone:     '',
  email:     '',
  city:      '',
  branch:    '',
  comment:   '',
})

// ─── Delivery ─────────────────────────────────────────────────────────────────

const deliveryOptions: DeliveryOption[] = [
  { id: 'nova-poshta', label: 'Нова Пошта', icon: 'nova-poshta' },
  { id: 'courier',     label: "Кур'єр",     icon: 'courier'     },
  { id: 'pickup',      label: 'Самовивіз',  icon: 'pickup'      },
]

const deliveryMethod = ref('nova-poshta')

// ─── Payment ──────────────────────────────────────────────────────────────────

const paymentOptions: PaymentOption[] = [
  {
    id:       'cod',
    label:    'Оплата під час отримання',
    subtitle: 'Готівкою або карткою на пошті',
    badges:   2,
  },
  {
    id:       'card',
    label:    'Карткою онлайн',
    subtitle: 'Visa, Mastercard, Apple Pay, Google Pay',
    badges:   3,
  },
  {
    id:       'installment',
    label:    'Оплата частинами',
    subtitle: 'Monobank, PrivatBank — без переплат',
    badges:   2,
  },
]

const paymentMethod = ref('cod')

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'submit', payload: typeof form & { delivery: string; payment: string }): void
}>()

function handleSubmit() {
  emit('submit', { ...form, delivery: deliveryMethod.value, payment: paymentMethod.value })
}
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
          КОНТАКТНІ ДАНІ
        </span>
      </div>

      <!-- Fields grid -->
      <div class="self-stretch grid grid-cols-1 sm:grid-cols-2 gap-4">

        <!-- First name -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            Ім'я <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.firstName"
            type="text"
            placeholder="Наприклад: Олексій"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
          />
        </div>

        <!-- Last name -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            Прізвище <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.lastName"
            type="text"
            placeholder="Наприклад: Коваленко"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
          />
        </div>

        <!-- Phone -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            Номер телефону <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.phone"
            type="tel"
            placeholder="+38 (0__) ___ __ __"
            class="w-full px-3.5 py-2.5 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none focus:bg-gray-900/80"
          />
        </div>

        <!-- Email -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            Email <span class="text-orange-500">*</span>
          </label>
          <input
            v-model="form.email"
            type="email"
            placeholder="you@example.com"
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
          ДОСТАВКА
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
          <!-- Nova Poshta icon -->
          <svg v-if="opt.icon === 'nova-poshta'" class="w-5 h-5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" :class="deliveryMethod === opt.id ? 'text-orange-500' : 'text-gray-400'">
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

      <!-- City + Branch selects -->
      <div class="self-stretch pt-1 grid grid-cols-1 sm:grid-cols-2 gap-4">

        <!-- City -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            Місто <span class="text-orange-500">*</span>
          </label>
          <div class="relative">
            <select
              v-model="form.city"
              class="w-full appearance-none h-11 pl-3.5 pr-10 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none cursor-pointer"
            >
              <option value="" disabled>Оберіть місто</option>
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

        <!-- Branch -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
            Відділення / Поштомат <span class="text-orange-500">*</span>
          </label>
          <div class="relative">
            <select
              v-model="form.branch"
              class="w-full appearance-none h-11 pl-3.5 pr-10 bg-gray-900 rounded-lg outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-200 text-sm font-normal font-['Onest'] transition-all duration-150 focus:outline-orange-500 focus:ring-1 focus:ring-orange-500/40 focus:outline-none cursor-pointer"
            >
              <option value="" disabled>Відділення / Поштомат</option>
              <option>Відділення №1</option>
              <option>Відділення №2</option>
              <option>Поштомат №100</option>
            </select>
            <svg class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 6l4 4 4-4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- Comment -->
      <div class="self-stretch flex flex-col gap-1.5">
        <label class="text-gray-400 text-xs font-medium font-['Onest'] leading-4 tracking-tight">
          Коментар до замовлення
        </label>
        <textarea
          v-model="form.comment"
          rows="3"
          placeholder="Додаткові побажання до доставки або замовлення..."
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
          ОПЛАТА
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
</style>
