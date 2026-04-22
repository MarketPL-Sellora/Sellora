<script setup lang="ts">

import { ref, reactive } from 'vue'
import { apiClient } from '../api/axios'

// ─── Tabs ─────────────────────────────────────────────────────────────────────

const isLogin = ref(true)

// ─── Forms ────────────────────────────────────────────────────────────────────

const loginForm = reactive({ email: '', password: '' })

// Видалено поле name: бекенд /auth/register приймає лише email + password (див. Swagger)
const registerForm = reactive({ email: '', password: '', passwordConfirm: '' })

// ─── US 4.1: Стан завантаження ────────────────────────────────────────────────
const isLoading = ref(false)

// ─── Password visibility ──────────────────────────────────────────────────────

const showLoginPassword = ref(false)
const showRegPassword = ref(false)
const showRegPasswordConf = ref(false)

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'login', payload: typeof loginForm): void
  (e: 'register', payload: typeof registerForm): void
  (e: 'google'): void
  (e: 'login-success'): void
}>()

// ─── Головний обробник форми ──────────────────────────────────────────────────

async function handleAuth() {
  isLoading.value = true

  try {
    if (isLogin.value) {
      // TODO: закоментовано — потрібно уточнити ендпоінт логіну у Swagger
      // (поки невідомо чи /auth/login приймає ті самі поля)
      // await apiClient.post('/auth/login', {
      //   email: loginForm.email,
      //   password: loginForm.password,
      // })

      // Тимчасово просто емітуємо подію без реального запиту
      emit('login', { ...loginForm })
    } else {
      // Реєстрація: лише email + password (name видалено — не підтримується бекендом)
      await apiClient.post('/auth/register', {
        email: registerForm.email,
        password: registerForm.password,
      })
    }

    emit('login-success')
    emit('close')

  } catch (error) {
    // TODO: замінити на toast-сповіщення для користувача
    console.error(error)

  } finally {
    isLoading.value = false
  }
}

function handleLogin() {
  emit('login', { ...loginForm })
}

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
          :class="isLogin ? 'border-orange-500' : 'border-transparent hover:border-gray-600'"
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
          :class="!isLogin ? 'border-orange-500' : 'border-transparent hover:border-gray-600'"
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
      <form v-else class="self-stretch flex flex-col gap-4" @submit.prevent="handleAuth">

        <!-- Name — ЗАКОМЕНТОВАНО: бекенд /auth/register не приймає поле name (див. Swagger) -->
        <!--
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
            Ім'я
          </label>
          <input
            v-model="registerForm.name"
            type="text"
            placeholder="Ваше ім'я"
            :disabled="isLoading"
            class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed"
          />
        </div>
        -->

        <!-- Email -->
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">
            Email
          </label>
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

        <!-- Submit -->
        <div class="pt-1">
          <button
            type="submit"
            :disabled="isLoading"
            class="w-full py-3.5 relative bg-orange-500 rounded-xl flex justify-center items-center gap-2 shadow-[0px_4px_6px_-4px_rgba(249,115,22,0.20),0px_10px_15px_-3px_rgba(249,115,22,0.20)] transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] active:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-orange-500/50 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:bg-orange-500 disabled:active:scale-100"
          >
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
