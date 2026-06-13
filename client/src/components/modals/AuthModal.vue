<script setup lang="ts">
import { ref, reactive } from 'vue'
import { apiClient } from '../../api/axios'
import { useUserStore } from '../../state/userStore'

const isLogin = ref(true)

const loginForm = reactive({ email: '', password: '' })
const registerForm = reactive({ email: '', password: '', passwordConfirm: '' })

const isLoading = ref(false)
const errorMessage = ref('')

const showLoginPassword = ref(false)
const showRegPassword = ref(false)
const showRegPasswordConf = ref(false)

const userStore = useUserStore()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'login', payload: typeof loginForm): void
  (e: 'register', payload: typeof registerForm): void
  (e: 'login-success'): void
}>()

async function handleAuth() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    if (isLogin.value) {
      await apiClient.post('/auth/login', {
        email: loginForm.email,
        password: loginForm.password,
      })
      emit('login', { ...loginForm })
    } else {
      if (registerForm.password !== registerForm.passwordConfirm) {
        errorMessage.value = 'Паролі не збігаються'
        isLoading.value = false
        return
      }
      await apiClient.post('/auth/register', {
        email: registerForm.email,
        password: registerForm.password,
      })
      emit('register', { ...registerForm })
    }

    await userStore.fetchMe()

    emit('login-success')
    emit('close')

  } catch (error: any) {
    console.error(error)
    errorMessage.value = error.response?.data?.message || (isLogin.value ? 'Невірний email або пароль.' : 'Помилка реєстрації.')
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div
    class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4"
    @click.self="emit('close')"
  >
    <div
      class="w-96 p-8 relative bg-zinc-900 rounded-2xl flex flex-col gap-8 shadow-[0px_25px_50px_-12px_rgba(0,0,0,0.60)]"
      @click.stop
    >
      <button
        aria-label="Закрити"
        class="absolute right-5 top-5 text-gray-500 hover:text-gray-300 transition-colors duration-150 active:scale-90"
        @click="emit('close')"
      >
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M1 1L17 17M17 1L1 17" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
        </svg>
      </button>

      <div class="self-stretch border-b border-gray-700/60 inline-flex justify-start items-start gap-6">
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

      <div
        v-if="errorMessage"
        class="p-3 bg-red-500/10 border border-red-500/50 rounded-xl flex items-start gap-3 animate-pulse"
      >
        <svg class="w-5 h-5 text-red-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span class="text-red-400 text-sm font-['Onest']">{{ errorMessage }}</span>
      </div>

      <div v-if="isLogin" class="self-stretch flex flex-col gap-4">
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">Email</label>
          <input v-model="loginForm.email" type="email" placeholder="you@example.com" class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500" />
        </div>
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">Пароль</label>
          <div class="relative">
            <input v-model="loginForm.password" :type="showLoginPassword ? 'text' : 'password'" placeholder="••••••••" class="w-full px-4 py-3 pr-11 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500" />
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-gray-400 transition-colors duration-150" @click="showLoginPassword = !showLoginPassword">
              <svg v-if="!showLoginPassword" class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M1 8s2.5-5 7-5 7 5 7 5-2.5 5-7 5-7-5-7-5z" stroke-linecap="round"/><circle cx="8" cy="8" r="2"/></svg>
              <svg v-else class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M2 2l12 12M6.5 6.7A2 2 0 0010 9.5M4.2 4.5C2.6 5.6 1 8 1 8s2.5 5 7 5c1.4 0 2.6-.4 3.6-1M7 3.1C7.3 3 7.7 3 8 3c4.5 0 7 5 7 5s-.6 1.2-1.7 2.3" stroke-linecap="round"/></svg>
            </button>
          </div>
        </div>
        <div class="flex justify-end -mt-1">
          <button class="text-gray-500 text-xs font-normal font-['Onest'] leading-4 hover:text-orange-500 transition-colors duration-150">Забули пароль?</button>
        </div>
        <div class="pt-1">
          <button class="w-full py-3.5 relative bg-orange-500 rounded-xl flex justify-center items-center shadow-[0px_4px_6px_-4px_rgba(249,115,22,0.20),0px_10px_15px_-3px_rgba(249,115,22,0.20)] transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] active:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-orange-500/50 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:bg-orange-500 disabled:active:scale-100" :disabled="isLoading" @click="handleAuth">
            <span class="text-white text-sm font-semibold font-['Onest'] leading-5 tracking-tight">{{ isLoading ? 'Завантаження...' : 'Увійти' }}</span>
          </button>
        </div>
      </div>

      <form v-else class="self-stretch flex flex-col gap-4" @submit.prevent="handleAuth">
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">Email</label>
          <input v-model="registerForm.email" type="email" placeholder="you@example.com" :disabled="isLoading" class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed" />
        </div>
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">Пароль</label>
          <div class="relative">
            <input v-model="registerForm.password" :type="showRegPassword ? 'text' : 'password'" placeholder="••••••••" :disabled="isLoading" class="w-full px-4 py-3 pr-11 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed" />
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-gray-400 transition-colors duration-150" @click="showRegPassword = !showRegPassword">
              <svg v-if="!showRegPassword" class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M1 8s2.5-5 7-5 7 5 7 5-2.5 5-7 5-7-5-7-5z" stroke-linecap="round"/><circle cx="8" cy="8" r="2"/></svg>
              <svg v-else class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M2 2l12 12M6.5 6.7A2 2 0 0010 9.5M4.2 4.5C2.6 5.6 1 8 1 8s2.5 5 7 5c1.4 0 2.6-.4 3.6-1M7 3.1C7.3 3 7.7 3 8 3c4.5 0 7 5 7 5s-.6 1.2-1.7 2.3" stroke-linecap="round"/></svg>
            </button>
          </div>
        </div>
        <div class="flex flex-col gap-1.5">
          <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">Повторіть пароль</label>
          <div class="relative">
            <input v-model="registerForm.passwordConfirm" :type="showRegPasswordConf ? 'text' : 'password'" placeholder="••••••••" :disabled="isLoading" class="w-full px-4 py-3 pr-11 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all duration-150 focus:outline-none focus:ring-1 focus:border-orange-500 disabled:opacity-50 disabled:cursor-not-allowed" :class="registerForm.passwordConfirm && registerForm.passwordConfirm !== registerForm.password ? 'text-red-400 focus:ring-red-500' : 'text-gray-300 focus:ring-orange-500'" />
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-gray-400 transition-colors duration-150" @click="showRegPasswordConf = !showRegPasswordConf">
              <svg v-if="!showRegPasswordConf" class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M1 8s2.5-5 7-5 7 5 7 5-2.5 5-7 5-7-5-7-5z" stroke-linecap="round"/><circle cx="8" cy="8" r="2"/></svg>
              <svg v-else class="w-4 h-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M2 2l12 12M6.5 6.7A2 2 0 0010 9.5M4.2 4.5C2.6 5.6 1 8 1 8s2.5 5 7 5c1.4 0 2.6-.4 3.6-1M7 3.1C7.3 3 7.7 3 8 3c4.5 0 7 5 7 5s-.6 1.2-1.7 2.3" stroke-linecap="round"/></svg>
            </button>
          </div>
          <p v-if="registerForm.passwordConfirm && registerForm.passwordConfirm !== registerForm.password" class="text-red-400 text-xs font-normal font-['Onest'] leading-4 mt-0.5">Паролі не збігаються</p>
        </div>
        <div class="pt-1">
          <button type="submit" :disabled="isLoading" class="w-full py-3.5 relative bg-orange-500 rounded-xl flex justify-center items-center gap-2 shadow-[0px_4px_6px_-4px_rgba(249,115,22,0.20),0px_10px_15px_-3px_rgba(249,115,22,0.20)] transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] active:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-orange-500/50 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:bg-orange-500 disabled:active:scale-100">
            <span class="text-white text-sm font-semibold font-['Onest'] leading-5 tracking-tight">{{ isLoading ? 'Завантаження...' : 'Зареєструватися' }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>