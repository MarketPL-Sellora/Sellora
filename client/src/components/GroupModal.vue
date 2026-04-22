<script setup lang="ts">
import { ref } from 'vue'

// ─── Props & Emits ────────────────────────────────────────────────────────────

defineProps<{ isOpen?: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

// ─── TODO: додай сюди реальну логіку групової покупки ─────────────────────────
// Наприклад: кількість учасників, таймер, посилання запрошення тощо
const groupLink = ref('sellora.com/group/12345')
</script>

<template>
  <!-- ── Overlay ──────────────────────────────────────────────────────────── -->
  <Teleport to="body">
    <div
      class="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4"
      @click.self="emit('close')"
    >
      <!-- ── Modal window ────────────────────────────────────────────────── -->
      <div
        class="relative w-full max-w-md bg-[#1a1c23] rounded-2xl p-6 flex flex-col gap-5 shadow-[0px_32px_80px_0px_rgba(0,0,0,0.70)]"
        @click.stop
      >
        <!-- Top accent line -->
        <div class="absolute top-0 left-0 right-0 h-[3px] bg-gradient-to-r from-orange-500 via-amber-400 to-orange-500 rounded-t-2xl" />

        <!-- Close button -->
        <button
          aria-label="Закрити"
          class="absolute right-4 top-4 w-8 h-8 bg-white/5 rounded-xl outline outline-1 outline-white/10 flex items-center justify-center transition-all duration-150 hover:bg-white/10 active:scale-95"
          @click="emit('close')"
        >
          <svg class="w-4 h-4 text-slate-400" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.67" stroke-linecap="round">
            <path d="M4 4l8 8M12 4l-8 8"/>
          </svg>
        </button>

        <!-- ── Header ───────────────────────────────────────────────────── -->
        <div class="flex flex-col items-center gap-3 pt-4">
          <!-- Icon -->
          <div class="w-14 h-14 bg-gradient-to-br from-orange-500 to-orange-600 rounded-full flex items-center justify-center shadow-[0px_8px_32px_0px_rgba(249,115,22,0.45)]">
            <svg class="w-7 h-7 text-white" viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="10" cy="10" r="4"/>
              <path d="M2 24a8 8 0 0116 0"/>
              <circle cx="21" cy="10" r="3"/>
              <path d="M24 24c0-3-1.5-5.5-4-6.5"/>
            </svg>
          </div>

          <!-- Title -->
          <h2 class="text-white text-xl font-black font-['Unbounded'] leading-7 text-center">
            Групова покупка
          </h2>

          <!-- Subtitle -->
          <p class="text-slate-400 text-sm font-normal font-['Onest'] leading-6 text-center max-w-xs">
            Зберіть групу з <span class="text-orange-500 font-semibold">3 учасників</span> протягом
            <span class="text-white font-semibold">24 годин</span> та отримайте знижку
            <span class="text-green-400 font-semibold">до 30%</span>.
          </p>
        </div>

        <div class="h-px bg-white/5" />

        <!-- ── TODO: Учасники групи ──────────────────────────────────────── -->
        <div class="flex flex-col gap-2">
          <span class="text-slate-600 text-[9px] font-semibold font-['Unbounded'] uppercase leading-4 tracking-wide text-center">
            Учасники групи
          </span>
          <!-- Placeholder: замінити на реальний список учасників -->
          <div class="flex justify-center gap-3">
            <div class="w-12 h-12 bg-gradient-to-br from-orange-500 to-red-600 rounded-full flex items-center justify-center shadow-[0_0_0_3px_rgba(249,115,22,0.3)]">
              <span class="text-white text-sm font-bold font-['Unbounded']">МК</span>
            </div>
            <div v-for="i in 2" :key="i" class="w-12 h-12 bg-white/5 rounded-full outline outline-2 outline-white/20 flex items-center justify-center">
              <span class="text-gray-600 text-xl">+</span>
            </div>
          </div>
          <p class="text-center text-xs font-['Onest'] text-slate-600">
            <span class="text-orange-500 font-bold">1</span>
            <span> з </span>
            <span class="text-gray-300 font-semibold">3</span>
            <span> учасників приєдналось</span>
          </p>
        </div>

        <div class="h-px bg-white/5" />

        <!-- ── TODO: Посилання запрошення ───────────────────────────────── -->
        <div class="flex flex-col gap-2">
          <span class="text-slate-600 text-[9px] font-semibold font-['Unbounded'] uppercase leading-4 tracking-wide">
            Посилання для запрошення
          </span>
          <div class="flex items-center gap-2">
            <div class="flex-1 px-3 py-2.5 bg-white/5 rounded-xl outline outline-1 outline-white/10 text-slate-200 text-xs font-['Onest'] truncate">
              {{ groupLink }}
            </div>
            <button class="px-4 py-2.5 bg-gradient-to-b from-orange-500 to-orange-600 rounded-xl text-white text-sm font-semibold font-['Onest'] whitespace-nowrap transition-all hover:from-orange-400 hover:to-orange-500 active:scale-95">
              Копіювати
            </button>
          </div>
        </div>

        <!-- ── CTA ──────────────────────────────────────────────────────── -->
        <button class="w-full py-3.5 bg-orange-500 rounded-xl text-white text-sm font-bold font-['Unbounded'] tracking-wide transition-all hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98]">
          <!-- TODO: замінити на реальну дію -->
          ПРИЄДНАТИСЯ ДО ГРУПИ
        </button>

      </div>
    </div>
  </Teleport>
</template>
