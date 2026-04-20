<script setup lang="ts">
import { ref } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface Tab {
  id: string
  label: string
  count?: number
}

interface Feature {
  icon: 'camera' | 'chip' | 'display' | 'pen' | 'battery' | 'ai'
  title: string
  subtitle: string
}

// ─── Tabs ─────────────────────────────────────────────────────────────────────

const tabs: Tab[] = [
  { id: 'desc',    label: 'ОПИС'              },
  { id: 'specs',   label: 'ХАРАКТЕРИСТИКИ'    },
  { id: 'reviews', label: 'ВІДГУКИ', count: 214 },
]

const activeTab = ref('desc')

// ─── Description paragraphs ───────────────────────────────────────────────────

const descriptionParagraphs: string[] = [
  'Samsung Galaxy S24 Ultra — це флагман нового покоління, що встановлює нові стандарти в мобільній фотографії та штучному інтелекті. Оснащений процесором Snapdragon 8 Gen 3, він забезпечує блискавичну продуктивність у будь-яких умовах.',
  'Революційна камерна система з 200 Мп основним сенсором та підтримкою 100-кратного оптичного зуму відкриває безмежні творчі можливості. Вбудований S Pen з нульовою затримкою дозволяє легко занотовувати ідеї та малювати прямо на екрані.',
  'Galaxy AI — набір інтелектуальних функцій на основі штучного інтелекту, що перетворює спілкування, творчість та продуктивність. Від Circle to Search до Live Translate — телефон розуміє вас краще, ніж будь-коли.',
]

// ─── Features ────────────────────────────────────────────────────────────────

const features: Feature[] = [
  {
    icon: 'camera',
    title: '200 Мп камера',
    subtitle: 'Quad-bayer сенсор, 100x zoom, нічна зйомка',
  },
  {
    icon: 'chip',
    title: 'Snapdragon 8 Gen 3',
    subtitle: '4 нм чіп, 12 ГБ RAM, AnTuTu 2,1 млн',
  },
  {
    icon: 'display',
    title: '6.8" Dynamic AMOLED 2X',
    subtitle: '3088×1440, 120 Гц, 2600 нт пікова яскравість',
  },
  {
    icon: 'pen',
    title: 'S Pen вбудований',
    subtitle: 'Нульова затримка, підтримка AI',
  },
  {
    icon: 'battery',
    title: 'Акумулятор 5000 мАг',
    subtitle: '45 Вт дротова, 15 Вт бездротова зарядка',
  },
  {
    icon: 'ai',
    title: 'Galaxy AI',
    subtitle: 'Circle to Search, Live Translate, Note Assist',
  },
]

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'tab-change', id: string): void
}>()

function setTab(id: string) {
  activeTab.value = id
  emit('tab-change', id)
}
</script>

<template>
  <div class="w-full inline-flex flex-col justify-start items-start gap-8">

    <!-- ── Tab bar ────────────────────────────────────────────────────────── -->
    <div class="self-stretch border-b border-[#1c1f2a] inline-flex justify-start items-start">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="px-6 py-3.5 relative inline-flex justify-center items-center gap-1 text-sm font-normal font-['Onest'] leading-5 transition-colors duration-150 focus:outline-none"
        :class="
          activeTab === tab.id
            ? 'text-white'
            : 'text-[#5a5f7a] hover:text-white'
        "
        @click="setTab(tab.id)"
      >
        {{ tab.label }}
        <span
          v-if="tab.count"
          class="transition-colors duration-150"
          :class="activeTab === tab.id ? 'text-orange-400' : 'text-orange-400/60'"
        >
          ({{ tab.count }})
        </span>

        <!-- Active underline -->
        <span
          v-if="activeTab === tab.id"
          class="absolute bottom-0 left-0 right-0 h-0.5 bg-orange-500 rounded-t"
        />
      </button>
    </div>

    <!-- ── ОПИС tab ───────────────────────────────────────────────────────── -->
    <div
      v-if="activeTab === 'desc'"
      class="self-stretch inline-flex justify-center items-start gap-8 flex-wrap"
    >
      <!-- Left: text + banners -->
      <div class="flex-1 min-w-0 self-stretch inline-flex flex-col justify-start items-start gap-6">

        <!-- Description -->
        <div class="self-stretch flex flex-col gap-3">
          <h2 class="text-white text-xl font-normal font-['Unbounded'] leading-7">
            Про Samsung Galaxy S24 Ultra
          </h2>
          <p
            v-for="(para, i) in descriptionParagraphs"
            :key="i"
            class="text-[#787d99] text-sm font-normal font-['Onest'] leading-6"
            :class="i > 0 ? 'pt-1' : ''"
          >
            {{ para }}
          </p>
        </div>

        <!-- Banner 1 — Camera -->
        <div class="self-stretch py-44 relative bg-[#1c1f2a] rounded-xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] flex justify-center items-center overflow-hidden">
          <div class="absolute inset-0 bg-gradient-to-br from-[#1a1040] to-[#0d1022]" />
          <!-- Replace with: <img src="../assets/banner-camera.jpg" alt="Камерна система 200 Мп" class="absolute inset-0 w-full h-full object-cover" /> -->
          <div class="relative z-10 flex flex-col items-center gap-2">
            <svg class="w-10 h-10 text-[#c8ccdf]" viewBox="0 0 40 40" fill="none" stroke="currentColor" stroke-width="1.5" xmlns="http://www.w3.org/2000/svg">
              <rect x="5" y="11" width="30" height="22" rx="3"/>
              <circle cx="20" cy="22" r="7"/>
              <circle cx="20" cy="22" r="3"/>
              <rect x="14" y="7" width="8" height="4" rx="1"/>
              <circle cx="32" cy="15" r="1.5" fill="currentColor"/>
            </svg>
            <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">
              Банер — камерна система 200 Мп
            </span>
          </div>
        </div>

        <!-- Banner 2 — AI -->
        <div class="self-stretch py-44 relative bg-[#1c1f2a] rounded-xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] flex justify-center items-center overflow-hidden">
          <div class="absolute inset-0 bg-gradient-to-br from-[#0f2027] to-[#203a43]" />
          <!-- Replace with: <img src="../assets/banner-ai.jpg" alt="Galaxy AI функції" class="absolute inset-0 w-full h-full object-cover" /> -->
          <div class="relative z-10 flex flex-col items-center gap-2">
            <svg class="w-10 h-10 text-[#c8ccdf]" viewBox="0 0 40 40" fill="none" stroke="currentColor" stroke-width="1.5" xmlns="http://www.w3.org/2000/svg">
              <circle cx="20" cy="20" r="14"/>
              <path d="M20 10v4M20 26v4M10 20h4M26 20h4"/>
              <circle cx="20" cy="20" r="4"/>
              <path d="M13.4 13.4l2.8 2.8M23.8 23.8l2.8 2.8M26.6 13.4l-2.8 2.8M16.2 23.8l-2.8 2.8"/>
            </svg>
            <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">
              Банер — Galaxy AI функції
            </span>
          </div>
        </div>
      </div>

      <!-- Right: key features -->
      <div class="w-80 shrink-0 self-stretch">
        <div class="self-stretch p-5 bg-[#161820] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] flex flex-col gap-4">
          <h3 class="text-white text-base font-normal font-['Onest'] leading-6">
            Ключові особливості
          </h3>
          <div class="flex flex-col gap-3">
            <div
              v-for="feat in features"
              :key="feat.title"
              class="inline-flex justify-start items-start gap-3"
            >
              <!-- Icon -->
              <div class="w-7 h-7 shrink-0 mt-0.5 bg-orange-500/20 rounded-lg flex justify-center items-center">
                <!-- camera -->
                <svg v-if="feat.icon === 'camera'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <rect x="1" y="4" width="12" height="9" rx="1.5"/>
                  <circle cx="7" cy="8.5" r="2.5"/>
                  <rect x="4.5" y="2" width="3.5" height="2" rx="0.5"/>
                  <circle cx="11.5" cy="5.5" r="0.8" fill="currentColor"/>
                </svg>
                <!-- chip -->
                <svg v-else-if="feat.icon === 'chip'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <rect x="3.5" y="3.5" width="7" height="7" rx="1"/>
                  <path d="M5 1v2.5M7 1v2.5M9 1v2.5M5 10.5V13M7 10.5V13M9 10.5V13M1 5h2.5M1 7h2.5M1 9h2.5M10.5 5H13M10.5 7H13M10.5 9H13"/>
                </svg>
                <!-- display -->
                <svg v-else-if="feat.icon === 'display'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <rect x="1" y="2" width="12" height="8" rx="1"/>
                  <path d="M4.5 12h5M7 10v2"/>
                </svg>
                <!-- pen -->
                <svg v-else-if="feat.icon === 'pen'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <path d="M9.5 1.5l3 3-7 7H2.5v-3l7-7z" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M7.5 3.5l3 3" stroke-linecap="round"/>
                </svg>
                <!-- battery -->
                <svg v-else-if="feat.icon === 'battery'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <rect x="1" y="4" width="10" height="6" rx="1"/>
                  <path d="M11 6v2" stroke-width="1.5" stroke-linecap="round"/>
                  <path d="M13 5.5v3" stroke-linecap="round"/>
                  <path d="M3.5 7h4" stroke-linecap="round"/>
                  <path d="M5.5 5v4" stroke-linecap="round"/>
                </svg>
                <!-- ai -->
                <svg v-else-if="feat.icon === 'ai'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <circle cx="7" cy="7" r="5.5"/>
                  <path d="M7 4v3l2 1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="7" cy="7" r="1.5" fill="currentColor" stroke="none"/>
                </svg>
              </div>

              <!-- Text -->
              <div class="flex flex-col gap-0.5 min-w-0">
                <span class="text-[#c8ccdf] text-sm font-normal font-['Onest'] leading-5">{{ feat.title }}</span>
                <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">{{ feat.subtitle }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── ХАРАКТЕРИСТИКИ tab ─────────────────────────────────────────────── -->
    <div
      v-else-if="activeTab === 'specs'"
      class="self-stretch flex flex-col gap-4"
    >
      <p class="text-[#5a5f7a] text-sm font-normal font-['Onest'] leading-6">
        <!-- Тут буде таблиця характеристик -->
        Технічні характеристики будуть тут.
      </p>
    </div>

    <!-- ── ВІДГУКИ tab ────────────────────────────────────────────────────── -->
    <div
      v-else-if="activeTab === 'reviews'"
      class="self-stretch flex flex-col gap-4"
    >
      <p class="text-[#5a5f7a] text-sm font-normal font-['Onest'] leading-6">
        <!-- Тут будуть відгуки -->
        Відгуки покупців будуть тут.
      </p>
    </div>

  </div>
</template>
