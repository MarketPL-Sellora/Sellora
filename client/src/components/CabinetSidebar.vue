<script setup lang="ts">
import { ref } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface MenuItem {
  id: string
  label: string
  count?: number
  icon: 'orders' | 'groups' | 'wishlist' | 'settings'
}

// ─── User (replace with props / store later) ──────────────────────────────────

const user = ref({
  initials: 'МК',
  name:     'Максим Коваленко',
  email:    'm.kovalenko@gmail.com',
  badge:    'Pro учасник',
})

// ─── Menu items ───────────────────────────────────────────────────────────────

const menuItems: MenuItem[] = [
  { id: 'orders',  label: 'Мої замовлення',      count: 4,  icon: 'orders'   },
  { id: 'groups',  label: 'Мої групові покупки',  count: 2,  icon: 'groups'   },
  { id: 'wishlist',label: 'Обране',               count: 12, icon: 'wishlist' },
  { id: 'settings',label: 'Налаштування',                    icon: 'settings' },
]

const activeTab = ref('groups')

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'navigate', id: string): void
  (e: 'logout'): void
}>()

function navigate(id: string) {
  activeTab.value = id
  emit('navigate', id)
}
</script>

<template>
  <div class="w-full inline-flex flex-col justify-start items-start">
    <div class="self-stretch p-4 bg-gray-900 rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] flex flex-col gap-3">

      <!-- ── Avatar + user info ────────────────────────────────────────── -->
      <div class="self-stretch flex flex-col items-center gap-2 pt-4 pb-2">
        <!-- Gradient avatar ring -->
        <div class="w-14 h-14 p-0.5 bg-gradient-to-br from-orange-500 to-violet-600 rounded-[30px] flex justify-center items-center shrink-0">
          <div class="w-full h-full bg-[#1a2235] rounded-3xl flex justify-center items-center">
            <span class="text-orange-500 text-xl font-bold font-['Unbounded'] leading-7">
              {{ user.initials }}
            </span>
          </div>
        </div>

        <!-- Name -->
        <span class="text-gray-100 text-base font-bold font-['Onest'] leading-5 text-center">
          {{ user.name }}
        </span>

        <!-- Email -->
        <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4 text-center">
          {{ user.email }}
        </span>

        <!-- Badge -->
        <div class="px-2.5 py-[3px] bg-orange-500/10 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/20">
          <span class="text-orange-500 text-xs font-normal font-['Onest'] leading-4">
            {{ user.badge }}
          </span>
        </div>
      </div>

      <div class="self-stretch h-px bg-[#1e2d3d]" />

      <!-- ── Navigation items ──────────────────────────────────────────── -->
      <nav class="self-stretch flex flex-col gap-0.5">
        <button
          v-for="item in menuItems"
          :key="item.id"
          class="self-stretch px-4 py-2.5 rounded-[9.6px] inline-flex justify-start items-center gap-3 transition-all duration-150 focus:outline-none"
          :class="
            activeTab === item.id
              ? 'bg-orange-500/10 text-orange-500'
              : 'text-gray-400 hover:bg-white/5 hover:text-white'
          "
          @click="navigate(item.id)"
        >
          <!-- Icon -->
          <span class="w-3.5 h-3.5 shrink-0 flex items-center justify-center">
            <!-- Orders: clipboard list -->
            <svg v-if="item.icon === 'orders'" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1" class="w-3.5 h-3.5">
              <rect x="2" y="1.5" width="10" height="11" rx="1.5"/>
              <path d="M4.5 5h5M4.5 7.5h5M4.5 10h3" stroke-linecap="round"/>
              <path d="M5 1.5V3a1 1 0 002 0V1.5" stroke-linecap="round"/>
            </svg>

            <!-- Groups: user-group -->
            <svg v-else-if="item.icon === 'groups'" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1" class="w-3.5 h-3.5">
              <circle cx="5.5" cy="4.5" r="2"/>
              <path d="M1 12a4.5 4.5 0 019 0" stroke-linecap="round"/>
              <circle cx="10.5" cy="4.5" r="1.5"/>
              <path d="M13 12c0-2-1-3.5-2.5-4" stroke-linecap="round"/>
            </svg>

            <!-- Wishlist: heart -->
            <svg v-else-if="item.icon === 'wishlist'" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1" class="w-3.5 h-3.5">
              <path d="M7 11.5S1.5 8 1.5 4.5A2.5 2.5 0 017 3.2a2.5 2.5 0 015.5 1.3C12.5 8 7 11.5 7 11.5z" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>

            <!-- Settings: cog -->
            <svg v-else-if="item.icon === 'settings'" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1" class="w-3.5 h-3.5">
              <circle cx="7" cy="7" r="2"/>
              <path d="M7 1v1.5M7 11.5V13M1 7h1.5M11.5 7H13M2.93 2.93l1.06 1.06M9.9 9.9l1.1 1.1M2.93 11.07l1.06-1.06M9.9 4.1l1.1-1.1" stroke-linecap="round"/>
            </svg>
          </span>

          <!-- Label -->
          <span class="text-sm font-normal font-['Onest'] leading-5">{{ item.label }}</span>

          <!-- Count badge -->
          <div v-if="item.count != null" class="flex-1 inline-flex justify-end">
            <div
              class="px-2 py-[1.6px] rounded-full"
              :class="
                activeTab === item.id
                  ? 'bg-orange-500/20'
                  : 'bg-gray-800'
              "
            >
              <span
                class="text-xs font-medium font-['Onest'] leading-4"
                :class="activeTab === item.id ? 'text-orange-500' : 'text-gray-500'"
              >
                {{ item.count }}
              </span>
            </div>
          </div>
        </button>

        <!-- Divider -->
        <div class="self-stretch h-4 flex items-center">
          <div class="self-stretch w-full h-px bg-[#1e2d3d]" />
        </div>

        <!-- Logout -->
        <button
          class="self-stretch px-4 py-2.5 rounded-[9.6px] inline-flex justify-start items-center gap-3 text-red-400/70 transition-all duration-150 hover:bg-red-500/5 hover:text-red-400 active:scale-[0.98] focus:outline-none"
          @click="emit('logout')"
        >
          <svg class="w-3.5 h-3.5 shrink-0" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
            <path d="M5 12H3a1 1 0 01-1-1V3a1 1 0 011-1h2" stroke-linecap="round"/>
            <path d="M9.5 9.5L13 7l-3.5-2.5M13 7H5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-sm font-normal font-['Onest'] leading-5">Вихід</span>
        </button>
      </nav>

    </div>
  </div>
</template>
