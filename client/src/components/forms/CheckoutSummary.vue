<script setup lang="ts">
import { computed } from 'vue'
import { useCartStore } from '../../state/cartStore'

const cartStore = useCartStore()

// ─── Types ────────────────────────────────────────────────────────────────────

interface TrustBadge {
  icon: 'shield' | 'lock' | 'return'
  label: string
}

// ─── Computed: selected items from cart ────────────────────────────────────────

const selectedItems = computed(() => {
  if (!cartStore.cart?.items?.length) return []
  return cartStore.cart.items.filter(item =>
    cartStore.selectedProductIds.includes(item.productId)
  )
})

// ─── Trust badges ─────────────────────────────────────────────────────────────

const trustBadges: TrustBadge[] = [
  { icon: 'shield', label: 'Захист покупця'    },
  { icon: 'lock',   label: 'SSL-шифрування'    },
  { icon: 'return', label: 'Повернення 30 днів' },
]

const fmt = (n: number) => (n || 0).toLocaleString('uk-UA') + ' ₴'
</script>

<template>
  <div class="w-full inline-flex flex-col justify-start items-start">
    <div class="self-stretch p-6 bg-gray-800 rounded-2xl flex flex-col gap-3">

      <!-- ── Heading ─────────────────────────────────────────────────────── -->
      <h2 class="text-white text-base font-bold font-['Unbounded'] leading-6 tracking-widest">
        ВАШЕ ЗАМОВЛЕННЯ
      </h2>

      <!-- ── Empty state ─────────────────────────────────────────────────── -->
      <div v-if="selectedItems.length === 0" class="py-6 flex flex-col items-center gap-3">
        <div class="w-14 h-14 bg-gray-700/50 rounded-full flex items-center justify-center">
          <svg class="w-7 h-7 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/>
          </svg>
        </div>
        <p class="text-gray-500 text-sm font-['Onest'] text-center">
          Оберіть товари в кошику для оформлення
        </p>
      </div>

      <!-- ── Product rows ────────────────────────────────────────────────── -->
      <template v-if="selectedItems.length > 0">
        <div
          v-for="item in selectedItems"
          :key="item.productId"
          class="self-stretch pt-2 pb-1 inline-flex justify-start items-start gap-3"
        >
          <!-- Thumbnail -->
          <div class="w-16 h-16 shrink-0 bg-gray-700 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 flex justify-center items-center overflow-hidden">
            <img
              :src="item.image || 'https://via.placeholder.com/64'"
              :alt="item.title"
              class="w-full h-full object-contain p-1"
              loading="lazy"
            />
          </div>

          <!-- Info -->
          <div class="flex-1 self-stretch inline-flex flex-col justify-center gap-1">
            <span class="text-gray-200 text-sm font-semibold font-['Onest'] leading-4 line-clamp-2">
              {{ item.title }}
            </span>
            <span v-if="item.merchantName || item.storeName" class="text-gray-500 text-xs font-normal font-['Onest'] leading-4">
              {{ item.storeName || item.merchantName }}
            </span>
            <div class="self-stretch pt-1 flex flex-wrap justify-between items-end gap-2">
              <span class="text-gray-500 text-xs font-normal font-['Onest'] leading-4">
                Кількість: {{ item.quantity }}
              </span>
              <span class="text-white text-sm font-bold font-['Onest'] leading-5 whitespace-nowrap">
                {{ fmt(item.price * item.quantity) }}
              </span>
            </div>
          </div>
        </div>

        <div class="self-stretch h-px border-t border-gray-700" />

        <!-- ── Price breakdown ────────────────────────────────────────────── -->
        <div class="self-stretch pb-1 flex flex-col gap-2">
          <div class="self-stretch inline-flex justify-between items-start">
            <span class="text-gray-400 text-sm font-normal font-['Onest'] leading-5">
              Вартість товарів ({{ selectedItems.length }})
            </span>
            <span class="text-gray-200 text-sm font-normal font-['Onest'] leading-5">
              {{ fmt(cartStore.selectedTotalAmount) }}
            </span>
          </div>
        </div>

        <div class="self-stretch h-px border-t border-gray-700" />

        <!-- ── Total ───────────────────────────────────────────────────────── -->
        <div class="self-stretch flex flex-wrap justify-between items-center gap-2">
          <span class="text-gray-300 text-xs md:text-sm font-bold font-['Unbounded'] leading-5 tracking-tight">
            ДО СПЛАТИ
          </span>
          <span class="text-white text-xl md:text-2xl font-extrabold font-['Unbounded'] leading-8 whitespace-nowrap">
            {{ fmt(cartStore.selectedTotalAmount) }}
          </span>
        </div>
      </template>

      <!-- ── Trust badges ───────────────────────────────────────────────── -->
      <div class="self-stretch pt-1 inline-flex justify-between items-start">
        <div
          v-for="badge in trustBadges"
          :key="badge.label"
          class="flex-1 inline-flex flex-col items-center gap-1"
        >
          <div class="w-8 h-8 bg-orange-500/10 rounded-2xl outline outline-1 outline-offset-[-1px] outline-orange-500/20 flex justify-center items-center">
            <!-- Shield -->
            <svg v-if="badge.icon === 'shield'" class="w-4 h-4 text-gray-200" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M8 1.5l5.5 2v4.7C13.5 11.5 11 14 8 14.5 5 14 2.5 11.5 2.5 8.2V3.5L8 1.5z" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M5.5 8l2 2 3.5-3.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <!-- Lock -->
            <svg v-else-if="badge.icon === 'lock'" class="w-4 h-4 text-gray-200" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
              <rect x="3" y="7" width="10" height="7" rx="1.5"/>
              <path d="M5.5 7V5a2.5 2.5 0 015 0v2" stroke-linecap="round"/>
              <circle cx="8" cy="10.5" r="1" fill="currentColor" stroke="none"/>
            </svg>
            <!-- Return arrow -->
            <svg v-else-if="badge.icon === 'return'" class="w-4 h-4 text-gray-200" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M2.5 8A5.5 5.5 0 108 2.5H5.5" stroke-linecap="round"/>
              <path d="M5.5 2.5L3 5l2.5 2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <span class="text-center text-gray-500 text-xs font-normal font-['Onest'] leading-4">
            {{ badge.label }}
          </span>
        </div>
      </div>

    </div>
  </div>
</template>
