<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/layout/Header.vue'
import Footer from '../components/layout/Footer.vue'
import { INFO_SECTIONS } from '../constants/infoContent'

const route = useRoute()
const router = useRouter()

const activeSection = computed(() => {
  const section = route.params.section as string
  return INFO_SECTIONS[section as keyof typeof INFO_SECTIONS] ? section : 'how-to-order'
})

const currentContent = computed(() => INFO_SECTIONS[activeSection.value as keyof typeof INFO_SECTIONS])

const menuCategories = [
  {
    title: 'ПОКУПЦЯМ',
    items: [
      { id: 'how-to-order', label: 'Як зробити замовлення' },
      { id: 'payment-delivery', label: 'Оплата та доставка' },
      { id: 'returns', label: 'Повернення товарів' },
      { id: 'warranty', label: 'Гарантія та сервіс' },
      { id: 'group-buys', label: 'Групові покупки' },
      { id: 'loyalty', label: 'Програма лояльності' }
    ]
  },
  {
    title: 'ПРОДАВЦЯМ',
    items: [
      { id: 'become-seller', label: 'Стати продавцем' },
      { id: 'seller-cabinet', label: 'Кабінет продавця' },
      { id: 'tariffs', label: 'Тарифи та комісії' },
      { id: 'seller-group-promos', label: 'Групові акції' },
      { id: 'ads', label: 'Реклама на Sellora' },
      { id: 'support', label: 'Технічна підтримка' }
    ]
  },
  {
    title: 'ЮРИДИЧНА ІНФОРМАЦІЯ',
    items: [
      { id: 'terms', label: 'Умови використання' },
      { id: 'privacy', label: 'Політика конфіденційності' },
      { id: 'cookies', label: 'Cookie' }
    ]
  }
]

function navigateTo(id: string) {
  router.push(`/info/${id}`)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">
    <Header />
    
    <main class="flex-1 w-full max-w-[1140px] mx-auto px-4 md:px-6 py-8 md:py-12 flex flex-col md:flex-row gap-8 md:gap-12">
      <!-- Sidebar menu -->
      <aside class="w-full md:w-64 shrink-0 flex flex-col gap-8">
        <div v-for="category in menuCategories" :key="category.title" class="flex flex-col gap-3">
          <h3 class="text-xs font-bold text-gray-500 uppercase tracking-widest font-['Unbounded']">{{ category.title }}</h3>
          <ul class="flex flex-col gap-1">
            <li v-for="item in category.items" :key="item.id">
              <button 
                @click="navigateTo(item.id)"
                class="w-full text-left px-3 py-2 rounded-lg text-sm transition-all duration-200"
                :class="activeSection === item.id ? 'bg-orange-500/10 text-orange-400 font-bold' : 'text-gray-400 hover:text-white hover:bg-white/5'"
              >
                {{ item.label }}
              </button>
            </li>
          </ul>
        </div>
      </aside>

      <!-- Content area -->
      <section class="flex-1 min-w-0 bg-[#161820] outline outline-1 outline-[#1c1f2a] rounded-2xl p-6 md:p-10 flex flex-col gap-6 self-start">
        <h1 class="text-2xl md:text-3xl font-bold font-['Unbounded'] text-white">
          {{ currentContent.title }}
        </h1>
        <div class="h-px w-16 bg-orange-500" />
        <p class="text-gray-400 text-sm md:text-base leading-relaxed whitespace-pre-line">
          {{ currentContent.content }}
        </p>
      </section>
    </main>

    <Footer />
  </div>
</template>
