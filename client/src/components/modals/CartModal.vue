<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../state/cartStore'
import type { CartItem } from '../../state/cartStore'

const emit = defineEmits<{ (e: 'close'): void }>()
const router = useRouter()
const cartStore = useCartStore()

onMounted(() => {
  cartStore.fetchCart()
})

let debounceTimer: ReturnType<typeof setTimeout>

async function changeQuantity(item: CartItem, delta: number) {
  const newQty = item.quantity + delta
  if (newQty < 1) return

  const oldQty = item.quantity
  item.quantity = newQty // Optimistic UI

  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    const success = await cartStore.updateQuantity(item.productId, newQty)
    if (!success) item.quantity = oldQty // Rollback
    else await cartStore.fetchCart() // Refresh totals
  }, 500)
}

async function handleRemove(productId: number) {
  if (confirm('Видалити товар з кошика?')) {
    await cartStore.removeItem(productId)
  }
}

async function handleClear() {
  if (confirm('Очистити весь кошик?')) {
    await cartStore.clearCart()
  }
}

function goToCheckout() {
  emit('close')
  router.push('/checkout')
}

const fmt = (n: number) => (n || 0).toLocaleString('uk-UA') + ' ₴'
</script>

<template>
  <div class="fixed inset-0 z-[9999] flex justify-end bg-black/60 backdrop-blur-sm transition-opacity" @click.self="emit('close')">
    <div class="w-full max-w-md h-full bg-[#111319] shadow-2xl flex flex-col animate-slide-in">

      <!-- Header -->
      <div class="p-5 border-b border-[#1c1f2a] flex justify-between items-center bg-[#161820]">
        <h2 class="text-white text-xl font-bold font-['Unbounded']">Кошик</h2>
        <button class="text-gray-400 hover:text-white transition-colors" @click="emit('close')">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
        </button>
      </div>

      <!-- Body -->
      <div class="flex-1 overflow-y-auto p-5 scrollbar-thin">
        <div v-if="cartStore.isCartLoading && !cartStore.cart?.items?.length" class="text-center py-10 text-gray-500">Завантаження...</div>
        <div v-else-if="!cartStore.cart?.items?.length" class="text-center py-10 flex flex-col items-center gap-4">
          <div class="w-20 h-20 bg-[#1c1f2a] rounded-full flex items-center justify-center text-gray-500">
            <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/></svg>
          </div>
          <p class="text-gray-400">Ваш кошик порожній</p>
        </div>
        
        <div v-else class="flex flex-col gap-4">
          <div class="flex justify-between items-center mb-2">
            <span class="text-sm text-gray-400">Товарів: {{ cartStore.cart.items.length }}</span>
            <button class="text-xs text-red-400 hover:text-red-300 transition-colors" @click="handleClear">Очистити все</button>
          </div>
          
          <div v-for="item in cartStore.cart.items" :key="item.productId" class="p-3 bg-[#1c1f2a] rounded-xl border border-white/5 flex gap-4 relative group">
            <img :src="item.image || 'https://via.placeholder.com/80'" class="w-20 h-20 object-cover rounded-lg bg-white/5" />
            <div class="flex flex-col flex-1 justify-between">
              <div class="pr-6">
                <h3 class="text-sm font-semibold text-white leading-tight line-clamp-2">{{ item.name }}</h3>
                <p class="text-xs text-gray-500 mt-1">Продавець: <span class="text-orange-400">{{ item.merchantName || 'Sellora' }}</span></p>
              </div>
              <div class="flex justify-between items-end mt-2">
                <div class="flex items-center bg-[#0f1117] rounded-lg border border-white/10">
                  <button class="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-white" @click="changeQuantity(item, -1)">−</button>
                  <span class="w-8 text-center text-sm font-medium text-white">{{ item.quantity }}</span>
                  <button class="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-white" @click="changeQuantity(item, 1)">+</button>
                </div>
                <div class="text-right">
                  <div v-if="item.oldPrice" class="text-xs text-gray-500 line-through">{{ fmt(item.oldPrice * item.quantity) }}</div>
                  <div class="text-orange-500 font-bold font-['Unbounded']">{{ fmt(item.price * item.quantity) }}</div>
                </div>
              </div>
            </div>
            <!-- Delete button -->
            <button class="absolute top-3 right-3 text-gray-500 hover:text-red-400 transition-colors opacity-0 group-hover:opacity-100" @click="handleRemove(item.productId)">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div v-if="cartStore.cart?.items?.length" class="p-5 bg-[#161820] border-t border-[#1c1f2a]">
        <div class="flex justify-between items-center mb-4">
          <span class="text-gray-400">До сплати:</span>
          <span class="text-2xl text-white font-bold font-['Unbounded']">{{ fmt(cartStore.cart.totalAmount) }}</span>
        </div>
        <button class="w-full py-4 bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-400 hover:to-emerald-500 text-white font-bold rounded-xl shadow-lg transition-all active:scale-95 uppercase tracking-wide" @click="goToCheckout">
          Оформити замовлення
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-slide-in { animation: slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
@keyframes slideIn { from { transform: translateX(100%); } to { transform: translateX(0); } }
</style>
