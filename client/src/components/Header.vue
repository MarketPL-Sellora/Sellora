<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthModal from './AuthModal.vue'
// ─── Імпорт сховища користувача ───────────────────────────────────────────────
import { useUserStore } from '../state/userStore'

interface Category {
  label: string
  emoji: string
}

const categories: Category[] = [
  { emoji: '📱', label: 'Смартфони' },
  { emoji: '💻', label: 'Ноутбуки' },
  { emoji: '👟', label: 'Взуття' },
  { emoji: '🎮', label: 'Ігри' },
  { emoji: '🏠', label: 'Дім' },
  { emoji: '🧴', label: 'Краса' },
  { emoji: '🔧', label: 'Інструменти' },
  { emoji: '🚗', label: 'Авто' },
  { emoji: '📦', label: 'Групові покупки' },
]

const cartCount      = ref(3)
const searchQuery    = ref('')
const activeCategory = ref<string | null>(null)

// ─── Роутер і поточний маршрут ────────────────────────────────────────────────
const route  = useRoute()
const router = useRouter()

// ─── Сховище користувача (замість локального ref) ─────────────────────────────
const userStore = useUserStore()

// ─── Модальне вікно авторизації ───────────────────────────────────────────────
const isAuthModalOpen = ref(false)

// ─── Чи відображати кнопку «Кабінет» ─────────────────────────────────────────
// Показуємо лише якщо: користувач залогінений І НЕ знаходиться на /cabinet
const showCabinetBtn = computed(
  () => userStore.isAuthenticated && route.path !== '/cabinet',
)

function setActive(label: string) {
  activeCategory.value = activeCategory.value === label ? null : label
}

</script>

<template>
  <header class="w-full bg-[#0f1117]/90 shadow-[0px_0px_40px_0px_rgba(249,115,22,0.04)] shadow-[0px_1px_0px_0px_rgba(249,115,22,0.15)] backdrop-blur-[10px]">

    <div class="w-full max-w-[1536px] mx-auto h-16 px-6 flex justify-between items-center gap-4">

      <!-- ── Logo → / ───────────────────────────────────────────────────── -->
      <router-link
        to="/"
        class="flex items-center gap-2 shrink-0 cursor-pointer transition-transform duration-200 hover:scale-105"
      >
        <img src="../assets/logo.png" alt="Sellora" class="w-8 h-8 rounded-lg object-cover" />
        <span class="text-orange-500 text-lg font-bold font-['Unbounded'] leading-7 select-none">
          Sellora
        </span>
      </router-link>

      <!-- ── Search ──────────────────────────────────────────────────────── -->
      <div class="flex-1 flex justify-center px-4">
        <div class="w-full max-w-[672px] relative flex items-center">
          <div class="w-full pl-10 pr-24 py-2.5 bg-[#1c1f2a] rounded-xl outline outline-1 outline-white/10 flex items-center transition-all duration-200 hover:outline-white/20 focus-within:outline-orange-500/40">
            <svg class="w-4 h-4 absolute left-3 text-[#7c6e7e] pointer-events-none" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.5" />
              <path d="M11 11L14 14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Знайди будь-що... iPhone 15, Xiaomi, Nike Air Max"
              class="flex-1 bg-transparent text-gray-400 text-sm font-normal font-['Onest'] placeholder-gray-500 outline-none"
            />
          </div>
          <button class="px-4 py-1.5 absolute right-1.5 bg-gradient-to-br from-orange-500 to-orange-600 rounded-lg shadow-[0px_4px_16px_0px_rgba(249,115,22,0.35)] transition-all hover:from-orange-400 hover:to-orange-500 active:scale-95">
            <span class="text-white text-xs font-semibold font-['Onest']">Знайти</span>
          </button>
        </div>
      </div>

      <!-- ── Nav buttons ────────────────────────────────────────────────── -->
      <div class="shrink-0 flex items-center gap-2">

        <!-- Кабінет → /cabinet (лише якщо залогінений і НЕ на /cabinet) -->
        <router-link
          v-if="showCabinetBtn"
          to="/cabinet"
          class="px-3 py-2 bg-[#1a1f2e] rounded-xl outline outline-1 outline-white/5 flex items-center gap-2 transition-all hover:bg-[#22273a] hover:outline-white/10 group"
        >
          <svg class="w-4 h-4 text-gray-400 group-hover:text-gray-200 transition-colors" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3.33 14v-1.33A2.67 2.67 0 016 10h4a2.67 2.67 0 012.67 2.67V14" stroke="currentColor" stroke-width="1.33" stroke-linecap="round" />
            <circle cx="8" cy="5.33" r="2.67" stroke="currentColor" stroke-width="1.33" />
          </svg>
          <span class="text-gray-400 group-hover:text-gray-200 text-xs font-normal font-['Onest'] transition-colors">Кабінет</span>
        </router-link>


        <!-- Увійти (неавторизований) → відкриває AuthModal -->
        <button
          v-if="!userStore.isAuthenticated"
          class="px-3 py-2 bg-[#1a1f2e] rounded-xl outline outline-1 outline-white/5 flex items-center gap-2 transition-all hover:bg-[#22273a] hover:outline-white/10 group"
          @click="isAuthModalOpen = true"
        >
          <svg class="w-4 h-4 text-gray-400 group-hover:text-gray-200 transition-colors" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3.33 14v-1.33A2.67 2.67 0 016 10h4a2.67 2.67 0 012.67 2.67V14" stroke="currentColor" stroke-width="1.33" stroke-linecap="round" />
            <circle cx="8" cy="5.33" r="2.67" stroke="currentColor" stroke-width="1.33" />
          </svg>
          <span class="text-gray-400 group-hover:text-gray-200 text-xs font-normal font-['Onest'] transition-colors">Увійти</span>
        </button>

        <!-- Порівняти (без маршруту) -->
        <button class="px-3 py-2 bg-[#1a1f2e] rounded-xl outline outline-1 outline-white/5 flex items-center gap-2 transition-all hover:bg-[#22273a] hover:outline-white/10 group">
          <svg class="w-4 h-4 text-gray-400 group-hover:text-gray-200 transition-colors" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="2" y="2" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.33" />
            <rect x="9" y="2" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.33" />
            <rect x="2" y="9" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.33" />
            <rect x="9" y="9" width="5" height="5" rx="1" stroke="currentColor" stroke-width="1.33" />
          </svg>
          <span class="text-gray-400 group-hover:text-gray-200 text-xs font-normal font-['Onest'] transition-colors">Порівняти</span>
        </button>

        <!-- Кошик → /checkout -->
        <router-link
          to="/checkout"
          class="px-3 py-2 relative bg-[#1a1f2e] rounded-xl outline outline-1 outline-white/5 flex items-center gap-2 transition-all hover:bg-[#22273a] hover:outline-white/10 group"
        >
          <svg class="w-4 h-4 text-gray-400 group-hover:text-gray-200 transition-colors" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M2 2h1.5l1.8 7h7l1.5-5H5" stroke="currentColor" stroke-width="1.33" stroke-linecap="round" stroke-linejoin="round" />
            <circle cx="7" cy="13" r="1" fill="currentColor" />
            <circle cx="11" cy="13" r="1" fill="currentColor" />
          </svg>
          <span class="text-gray-400 group-hover:text-gray-200 text-xs font-normal font-['Onest'] transition-colors">Кошик</span>
          <span
            v-if="cartCount > 0"
            class="absolute -top-1.5 -right-1.5 bg-orange-500 text-white text-[9px] font-bold px-1.5 py-0.5 rounded-full shadow-[0px_2px_8px_0px_rgba(249,115,22,0.60)] transition-transform group-hover:scale-110"
          >
            {{ cartCount }}
          </span>
        </router-link>

      </div>
    </div>

    <!-- ── Category nav ────────────────────────────────────────────────────── -->
    <div class="w-full border-t border-white/5">
      <div class="w-full max-w-[1536px] mx-auto px-6 py-2 flex items-center gap-2 overflow-x-auto scrollbar-none">

        <button
          class="px-3 py-1.5 bg-orange-500/20 rounded-lg outline outline-1 outline-orange-500/30 flex items-center gap-1.5 shrink-0 transition-all hover:bg-orange-500/30 active:scale-95"
          @click="activeCategory = null"
        >
          <svg class="w-3 h-3 text-orange-500" viewBox="0 0 12 12" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
            <rect x="1.8" y="1.8" width="3.6" height="3.6" rx="0.6" />
            <rect x="6.6" y="1.8" width="3.6" height="3.6" rx="0.6" />
            <rect x="1.8" y="6.6" width="3.6" height="3.6" rx="0.6" />
            <rect x="6.6" y="6.6" width="3.6" height="3.6" rx="0.6" />
          </svg>
          <span class="text-orange-500 text-xs font-medium font-['Onest'] whitespace-nowrap">Усі категорії</span>
        </button>

        <span class="text-white/10 text-base mx-1">|</span>

        <button
          v-for="cat in categories"
          :key="cat.label"
          class="px-3 py-1.5 rounded-lg flex items-center shrink-0 transition-all hover:bg-white/5 active:scale-95"
          :class="activeCategory === cat.label ? 'bg-white/10' : ''"
          @click="setActive(cat.label)"
        >
          <span
            class="text-xs font-medium font-['Onest'] whitespace-nowrap transition-colors"
            :class="activeCategory === cat.label ? 'text-white' : 'text-gray-400 hover:text-gray-200'"
          >
            {{ cat.emoji }} {{ cat.label }}
          </span>
        </button>

      </div>
    </div>

  </header>

  <!-- ── Auth modal ────────────────────────────────────────────────────────── -->
  <AuthModal
    v-if="isAuthModalOpen"
    @close="isAuthModalOpen = false"
    @login-success="isAuthModalOpen = false"
  />
</template>
