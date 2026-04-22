<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

// Імпортуємо налаштований екземпляр Axios з централізованого модуля.
// Він вже містить baseURL, withCredentials: true та інші налаштування.
import { apiClient } from '../api/axios'

// ─── Tabs ─────────────────────────────────────────────────────────────────────

const isLogin = ref(true)

// ─── Forms ────────────────────────────────────────────────────────────────────

const loginForm = reactive({ email: '', password: '' })

const registerForm = reactive({ name: '', email: '', password: '', passwordConfirm: '' })

// ─── US 4.2: Вибір ролі ───────────────────────────────────────────────────────
// Зберігає вибрану роль користувача: 'buyer' (покупець) або 'seller' (продавець).
// За замовчуванням встановлено 'buyer'.
const role = ref<'buyer' | 'seller'>('buyer')

// Додаткові поля форми реєстрації для ролі «Продавець» (US 4.2)
const sellerForm = reactive({
  shopName:    '', // Назва магазину
  taxId:       '', // ЄДРПОУ або ІПН
  address:     '', // Контактна адреса
})

// Обчислюване значення: чи є поточна роль «Продавець»
// Використовується у шаблоні для умовного відображення додаткових полів
const isSeller = computed(() => role.value === 'seller')

// ─── US 4.1: Стан завантаження ────────────────────────────────────────────────
// Прапорець, що показує, чи триває процес авторизації/реєстрації.
// Коли true — кнопка показує «Завантаження...», а всі інпути стають disabled.
const isLoading = ref(false)

// ─── Password visibility ──────────────────────────────────────────────────────

const showLoginPassword    = ref(false)
const showRegPassword      = ref(false)
const showRegPasswordConf  = ref(false)

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'login',         payload: typeof loginForm):    void
  (e: 'register',      payload: typeof registerForm): void
  (e: 'google'): void
  // Емітується після успішної авторизації на бекенді
  (e: 'login-success'): void
}>()

// ─── Головний обробник форми (handleAuth) ─────────────────────────────────────
// async — функція асинхронна, бо виконує реальний HTTP-запит до бекенду.
// Викликається при submit форми (вкладка «Вхід» або «Реєстрація»).
// Бекенд при успіху встановлює HttpOnly cookie — браузер зберігає його
// автоматично (завдяки withCredentials: true в axios.ts).
async function handleAuth() {
  // Вмикаємо стан завантаження — блокує інпути і змінює текст кнопки
  isLoading.value = true

  try {
    if (isLogin.value) {
      // ── Вкладка «Вхід» ──────────────────────────────────────────────────────
      // Надсилаємо POST-запит на ендпоінт /auth/login з email та паролем.
      // У разі успіху бекенд поверне 2xx і встановить HttpOnly cookie з сесією.
      await apiClient.post('/auth/login', {
        email:    loginForm.email,
        password: loginForm.password,
      })
    } else {
      // ── Вкладка «Реєстрація» ────────────────────────────────────────────────
      // Надсилаємо POST-запит на ендпоінт /auth/register з даними нового юзера.
      // Передаємо роль та, якщо обрано 'seller', додаткові поля продавця.
      await apiClient.post('/auth/register', {
        name:     registerForm.name,
        email:    registerForm.email,
        password: registerForm.password,
        role:     role.value,
        // Поширюємо поля продавця лише якщо роль — 'seller', інакше — порожній об'єкт
        ...(isSeller.value ? sellerForm : {}),
      })
    }

    // Запит завершився успішно (статус 2xx):
    // Повідомляємо батьківський компонент (Header) про вдалу авторизацію
    emit('login-success')

    // Закриваємо модальне вікно
    emit('close')

  } catch (error) {
    // Запит завершився з помилкою (мережева помилка або статус 4xx/5xx):
    // Виводимо деталі в консоль для налагодження.
    // TODO: замінити на user-friendlyповідомлення (наприклад, toast-сповіщення)
    console.error(error)

  } finally {
    // Блок finally виконується завжди — і після успіху, і після помилки.
    // Вимикаємо стан завантаження, щоб розблокувати форму.
    isLoading.value = false
  }
}

// Залишаємо функцію входу для сумісності (обробляє вкладку «Вхід»)
function handleLogin() {
  emit('login', { ...loginForm })
}
// Примітка: handleRegister видалена — її роль тепер виконує handleAuth
</script>

<template>
  <!-- ── Overlay ──────────────────────────────────────────────────────────── -->
  <div
    class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4"
    @click.self="emit('close')"
  >
    <!-- ── Modal card ────────────────────────────────────────────────────── -->
    <div
      class="w-96 p-8 relative bg-zinc-900 rounded-2xl flex flex-col gap-8 shadow-[0px_25px_50px_-12px_rgba(0,0,0,0.60)]"
      @click.stop
    >

      <!-- Close button -->
      <button
        aria-label="Закрити"
        class="absolute right-5 top-5 text-gray-500 hover:text-gray-300 transition-colors duration-150 active:scale-90"
        @click="emit('close')"
      >
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M1 1L17 17M17 1L1 17" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
        </svg>
      </button>

      <!-- ── Tabs ──────────────────────────────────────────────────────── -->
      <div class="self-stretch border-b border-gray-700/60 inline-flex justify-start items-start gap-6">

        <!-- Вхід tab -->
        <button
          class="pb-3 border-b-2 inline-flex flex-col justify-center items-center transition-colors duration-150"
          :class="isLogin
            ? 'border-orange-500'
            : 'border-transparent hover:border-gray-600'"
          @click="isLogin = true"
        >
          <span
            class="text-sm font-['Onest'] leading-5 tracking-tight transition-colors duration-150"
            :class="isLogin ? 'text-orange-500 font-semibold' : 'text-gray-500 font-medium hover:text-gray-300'"
          >
            Вхід
          </span>
        </button>

        <!-- Реєстрація tab -->
        <button
          class="pb-3 border-b-2 inline-flex flex-col justify-center items-center transition-colors duration-150"
          :class="!isLogin
            ? 'border-orange-500'
            : 'border-transparent hover:border-gray-600'"
          @click="isLogin = false"
        >
          <span
            class="text-sm font-['Onest'] leading-5 tracking-tight transition-colors duration-150"
            :class="!isLogin ? 'text-orange-500 font-semibold' : 'text-gray-500 font-medium hover:text-gray-300'"
          >
            Реєстрація
          </span>
        </button>

      </div>

      <!-- ── LOGIN form ────────────────────────────────────────────────── -->
      <div v-if="isLogin" class="self-stretch flex flex-col gap-4">

        <!-- Email -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
            Email
          </label>
          <input
            v-model="loginForm.email"
            type="email"
            placeholder="you@example.com"
            class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500"
          />
        </div>

        <!-- Password -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
            Пароль
          </label>
          <div class="relative">
            <input
              v-model="loginForm.password"
              :type="showLoginPassword ? 'text' : 'password'"
              placeholder="••••••••"
              class="w-full px-4 py-3 pr-11 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500"
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-gray-400 transition-colors duration-150"
              @click="showLoginPassword = !showLoginPassword"
            >
              <!-- Eye / Eye-off -->
              <svg v-if="!showLoginPassword" class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
                <path d="M1 8s2.5-5 7-5 7 5 7 5-2.5 5-7 5-7-5-7-5z" stroke-linecap="round"/>
                <circle cx="8" cy="8" r="2"/>
              </svg>
              <svg v-else class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
                <path d="M2 2l12 12M6.5 6.7A2 2 0 0010 9.5M4.2 4.5C2.6 5.6 1 8 1 8s2.5 5 7 5c1.4 0 2.6-.4 3.6-1M7 3.1C7.3 3 7.7 3 8 3c4.5 0 7 5 7 5s-.6 1.2-1.7 2.3" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Forgot password -->
        <div class="flex justify-end -mt-1">
          <button class="text-gray-500 text-xs font-normal font-['Onest'] leading-4 hover:text-orange-500 transition-colors duration-150">
            Забули пароль?
          </button>
        </div>

        <!-- Submit -->
        <div class="pt-1">
          <button
            class="w-full py-3.5 relative bg-orange-500 rounded-xl flex justify-center items-center shadow-[0px_4px_6px_-4px_rgba(249,115,22,0.20),0px_10px_15px_-3px_rgba(249,115,22,0.20)] transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] active:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-orange-500/50"
            @click="handleLogin"
          >
            <span class="text-white text-sm font-semibold font-['Onest'] leading-5 tracking-tight">Увійти</span>
          </button>
        </div>

        <!-- Divider -->
        <div class="py-1 inline-flex justify-start items-center gap-3">
          <div class="flex-1 h-px border-t border-gray-700" />
          <span class="text-gray-500 text-xs font-medium font-['Onest'] leading-4">або</span>
          <div class="flex-1 h-px border-t border-gray-700" />
        </div>

        <!-- Google -->
        <button
          class="w-full py-3.5 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 flex justify-center items-center gap-3 transition-all duration-150 hover:bg-white/5 hover:outline-gray-500 active:scale-[0.98] focus:outline-none focus:ring-2 focus:ring-white/10"
          @click="emit('google')"
        >
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M17.64 9.20418C17.64 8.56598 17.5827 7.95238 17.4764 7.36328H9V10.8447H13.8436C13.635 11.9697 13.0009 12.9228 12.0477 13.561V15.8192H14.9564C16.6582 14.2524 17.64 11.9451 17.64 9.20418Z" fill="#4285F4"/>
            <path d="M8.99976 18.0009C11.4298 18.0009 13.4671 17.195 14.9562 15.8204L12.0475 13.5622C11.2416 14.1022 10.2107 14.4213 8.99976 14.4213C6.65567 14.4213 4.67158 12.8381 3.96385 10.7109H0.957031V13.0427C2.43794 15.984 5.48158 18.0009 8.99976 18.0009Z" fill="#34A853"/>
            <path d="M3.96409 10.7108C3.78409 10.1708 3.68182 9.59388 3.68182 9.00078C3.68182 8.40768 3.78409 7.83078 3.96409 7.29078V4.95898H0.957275C0.347727 6.17388 0 7.54848 0 9.00078C0 10.4531 0.347727 11.8277 0.957275 13.0426L3.96409 10.7108Z" fill="#FBBC05"/>
            <path d="M8.99976 3.5795C10.3212 3.5795 11.5075 4.0336 12.4403 4.9254L15.0216 2.344C13.463 0.8918 11.4257 0 8.99976 0C5.48158 0 2.43794 2.0168 0.957031 4.9582L3.96385 7.29C4.67158 5.1627 6.65567 3.5795 8.99976 3.5795Z" fill="#EA4335"/>
          </svg>
          <span class="text-gray-300 text-sm font-medium font-['Onest'] leading-5">Увійти через Google</span>
        </button>

      </div>

      <!-- ── REGISTER form ──────────────────────────────────────────────── -->
      <!-- US 4.1: Форма обгорнута у <form> з @submit.prevent="handleAuth" -->
      <!-- Це дозволяє перехопити натискання Enter та сабміт кнопки типу submit -->
      <form v-else class="self-stretch flex flex-col gap-4" @submit.prevent="handleAuth">

        <!-- US 4.2: Перемикач ролі (Покупець / Продавець) ──────────────────── -->
        <!-- При зміні ролі на 'seller' динамічно з'являються 3 додаткові поля -->
        <div class="flex gap-2 p-1 bg-neutral-800 rounded-xl">

          <!-- Кнопка «Я Покупець» — встановлює role = 'buyer' -->
          <button
            type="button"
            class="flex-1 py-2 rounded-lg text-sm font-medium font-['Onest'] leading-5 transition-all duration-150"
            :class="role === 'buyer'
              ? 'bg-orange-500 text-white shadow-sm'
              : 'text-gray-400 hover:text-gray-300'"
            @click="role = 'buyer'"
          >
            🛒 Я Покупець
          </button>

          <!-- Кнопка «Я Продавець» — встановлює role = 'seller' -->
          <button
            type="button"
            class="flex-1 py-2 rounded-lg text-sm font-medium font-['Onest'] leading-5 transition-all duration-150"
            :class="role === 'seller'
              ? 'bg-orange-500 text-white shadow-sm'
              : 'text-gray-400 hover:text-gray-300'"
            @click="role = 'seller'"
          >
            🏪 Я Продавець
          </button>

        </div>
        <!-- / US 4.2: Кінець перемикача ролей -->

        <!-- Name -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
            Ім'я
          </label>
          <!-- US 4.1: :disabled="isLoading" блокує поле під час завантаження -->
          <input
            v-model="registerForm.name"
            type="text"
            placeholder="Ваше ім'я"
            :disabled="isLoading"
            class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed"
          />
        </div>

        <!-- Email -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
            Email
          </label>
          <!-- US 4.1: :disabled="isLoading" блокує поле під час завантаження -->
          <input
            v-model="registerForm.email"
            type="email"
            placeholder="you@example.com"
            :disabled="isLoading"
            class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed"
          />
        </div>

        <!-- Password -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
            Пароль
          </label>
          <div class="relative">
            <!-- US 4.1: :disabled="isLoading" блокує поле під час завантаження -->
            <input
              v-model="registerForm.password"
              :type="showRegPassword ? 'text' : 'password'"
              placeholder="••••••••"
              :disabled="isLoading"
              class="w-full px-4 py-3 pr-11 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed"
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-gray-400 transition-colors duration-150"
              @click="showRegPassword = !showRegPassword"
            >
              <svg v-if="!showRegPassword" class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
                <path d="M1 8s2.5-5 7-5 7 5 7 5-2.5 5-7 5-7-5-7-5z" stroke-linecap="round"/>
                <circle cx="8" cy="8" r="2"/>
              </svg>
              <svg v-else class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
                <path d="M2 2l12 12M6.5 6.7A2 2 0 0010 9.5M4.2 4.5C2.6 5.6 1 8 1 8s2.5 5 7 5c1.4 0 2.6-.4 3.6-1M7 3.1C7.3 3 7.7 3 8 3c4.5 0 7 5 7 5s-.6 1.2-1.7 2.3" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Confirm password -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
            Повторіть пароль
          </label>
          <div class="relative">
            <!-- US 4.1: :disabled="isLoading" блокує поле під час завантаження -->
            <input
              v-model="registerForm.passwordConfirm"
              :type="showRegPasswordConf ? 'text' : 'password'"
              placeholder="••••••••"
              :disabled="isLoading"
              class="w-full px-4 py-3 pr-11 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="
                registerForm.passwordConfirm && registerForm.passwordConfirm !== registerForm.password
                  ? 'text-red-400 focus:ring-red-500'
                  : 'text-gray-300 focus:ring-orange-500'
              "
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-gray-400 transition-colors duration-150"
              @click="showRegPasswordConf = !showRegPasswordConf"
            >
              <svg v-if="!showRegPasswordConf" class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
                <path d="M1 8s2.5-5 7-5 7 5 7 5-2.5 5-7 5-7-5-7-5z" stroke-linecap="round"/>
                <circle cx="8" cy="8" r="2"/>
              </svg>
              <svg v-else class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
                <path d="M2 2l12 12M6.5 6.7A2 2 0 0010 9.5M4.2 4.5C2.6 5.6 1 8 1 8s2.5 5 7 5c1.4 0 2.6-.4 3.6-1M7 3.1C7.3 3 7.7 3 8 3c4.5 0 7 5 7 5s-.6 1.2-1.7 2.3" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <!-- Mismatch hint -->
          <p
            v-if="registerForm.passwordConfirm && registerForm.passwordConfirm !== registerForm.password"
            class="text-red-400 text-xs font-normal font-['Onest'] leading-4 mt-0.5"
          >
            Паролі не збігаються
          </p>
        </div>

        <!-- US 4.2: Додаткові поля для ролі «Продавець» ────────────────────── -->
        <!-- Блок з'являється тільки якщо isSeller === true (role === 'seller') -->
        <!-- Використовує v-if для умовного рендерингу -->
        <template v-if="isSeller">

          <!-- Роздільник з підписом -->
          <div class="flex items-center gap-2">
            <div class="flex-1 h-px border-t border-orange-500/30" />
            <span class="text-orange-400/70 text-xs font-medium font-['Onest']">Дані продавця</span>
            <div class="flex-1 h-px border-t border-orange-500/30" />
          </div>

          <!-- Поле «Назва магазину» (US 4.2) -->
          <div class="flex flex-col gap-1.5">
            <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
              Назва магазину
            </label>
            <!-- US 4.1: :disabled="isLoading" блокує поле під час завантаження -->
            <input
              v-model="sellerForm.shopName"
              type="text"
              placeholder="Наприклад: TechShop UA"
              :disabled="isLoading"
              class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-orange-500/30 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed"
            />
          </div>

          <!-- Поле «ЄДРПОУ/ІПН» (US 4.2) -->
          <div class="flex flex-col gap-1.5">
            <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
              ЄДРПОУ / ІПН
            </label>
            <!-- US 4.1: :disabled="isLoading" блокує поле під час завантаження -->
            <input
              v-model="sellerForm.taxId"
              type="text"
              placeholder="12345678"
              :disabled="isLoading"
              class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-orange-500/30 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed"
            />
          </div>

          <!-- Поле «Контактна адреса» (US 4.2) -->
          <div class="flex flex-col gap-1.5">
            <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
              Контактна адреса
            </label>
            <!-- US 4.1: :disabled="isLoading" блокує поле під час завантаження -->
            <input
              v-model="sellerForm.address"
              type="text"
              placeholder="м. Київ, вул. Хрещатик, 1"
              :disabled="isLoading"
              class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-orange-500/30 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed"
            />
          </div>

        </template>
        <!-- / US 4.2: Кінець додаткових полів продавця -->

        <!-- Submit -->
        <!-- US 4.1: type="submit" — дозволяє формі перехопити натискання через @submit.prevent -->
        <!-- US 4.1: :disabled="isLoading" — блокує повторний сабміт під час завантаження -->
        <!-- US 4.1: Текст кнопки змінюється на «Завантаження...» поки isLoading === true -->
        <div class="pt-1">
          <button
            type="submit"
            :disabled="isLoading"
            class="w-full py-3.5 relative bg-orange-500 rounded-xl flex justify-center items-center gap-2 shadow-[0px_4px_6px_-4px_rgba(249,115,22,0.20),0px_10px_15px_-3px_rgba(249,115,22,0.20)] transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] active:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-orange-500/50 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:bg-orange-500 disabled:active:scale-100"
          >
            <!-- Спінер — відображається тільки під час завантаження (isLoading === true) -->
            <svg
              v-if="isLoading"
              class="w-4 h-4 animate-spin text-white"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            <!-- Текст кнопки: «Завантаження...» або «Зареєструватися» залежно від isLoading -->
            <span class="text-white text-sm font-semibold font-['Onest'] leading-5 tracking-tight">
              {{ isLoading ? 'Завантаження...' : 'Зареєструватися' }}
            </span>
          </button>
        </div>

        <!-- Divider -->
        <div class="py-1 inline-flex justify-start items-center gap-3">
          <div class="flex-1 h-px border-t border-gray-700" />
          <span class="text-gray-500 text-xs font-medium font-['Onest'] leading-4">або</span>
          <div class="flex-1 h-px border-t border-gray-700" />
        </div>

        <!-- Google -->
        <button
          type="button"
          class="w-full py-3.5 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 flex justify-center items-center gap-3 transition-all duration-150 hover:bg-white/5 hover:outline-gray-500 active:scale-[0.98] focus:outline-none focus:ring-2 focus:ring-white/10"
          @click="emit('google')"
        >
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M17.64 9.20418C17.64 8.56598 17.5827 7.95238 17.4764 7.36328H9V10.8447H13.8436C13.635 11.9697 13.0009 12.9228 12.0477 13.561V15.8192H14.9564C16.6582 14.2524 17.64 11.9451 17.64 9.20418Z" fill="#4285F4"/>
            <path d="M8.99976 18.0009C11.4298 18.0009 13.4671 17.195 14.9562 15.8204L12.0475 13.5622C11.2416 14.1022 10.2107 14.4213 8.99976 14.4213C6.65567 14.4213 4.67158 12.8381 3.96385 10.7109H0.957031V13.0427C2.43794 15.984 5.48158 18.0009 8.99976 18.0009Z" fill="#34A853"/>
            <path d="M3.96409 10.7108C3.78409 10.1708 3.68182 9.59388 3.68182 9.00078C3.68182 8.40768 3.78409 7.83078 3.96409 7.29078V4.95898H0.957275C0.347727 6.17388 0 7.54848 0 9.00078C0 10.4531 0.347727 11.8277 0.957275 13.0426L3.96409 10.7108Z" fill="#FBBC05"/>
            <path d="M8.99976 3.5795C10.3212 3.5795 11.5075 4.0336 12.4403 4.9254L15.0216 2.344C13.463 0.8918 11.4257 0 8.99976 0C5.48158 0 2.43794 2.0168 0.957031 4.9582L3.96385 7.29C4.67158 5.1627 6.65567 3.5795 8.99976 3.5795Z" fill="#EA4335"/>
          </svg>
          <span class="text-gray-300 text-sm font-medium font-['Onest'] leading-5">Зареєструватися через Google</span>
        </button>

      </form>

    </div>
  </div>
</template>
