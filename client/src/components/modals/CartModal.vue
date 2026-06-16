<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { DICT } from '../../constants/dictionary'
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

  if (item.stockQuantity !== undefined && newQty > item.stockQuantity) {
    alert(`Максимальна доступна кількість: ${item.stockQuantity} шт.`)
    return
  }

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
  if (confirm(DICT.messages.confirmDeleteCart)) {
    await cartStore.removeItem(productId)
  }
}

async function handleClear() {
  if (confirm(DICT.messages.confirmClearCart)) {
    await cartStore.clearCart()
  }
}

function goToCheckout() {
  emit('close')
  router.push('/checkout')
}

function goToProduct(productId: number) {
  emit('close')
  router.push('/product/' + productId)
}

function goToStore(merchantId: number | undefined) {
  if (!merchantId) return
  emit('close')
  router.push({ path: '/', query: { storeId: merchantId } })
}

function isItemSelected(productId: number): boolean {
  return cartStore.selectedProductIds.includes(productId)
}

function isStoreAllSelected(items: CartItem[]): boolean {
  return items.length > 0 && items.every(i => cartStore.selectedProductIds.includes(i.productId))
}

function isStorePartiallySelected(_merchantId: number, items: CartItem[]): boolean {
  const some = items.some(i => cartStore.selectedProductIds.includes(i.productId))
  const all = items.every(i => cartStore.selectedProductIds.includes(i.productId))
  return some && !all
}

const fmt = (n: number) => (n || 0).toLocaleString('uk-UA') + ' ₴'
</script>

<template>
  <div class="fixed inset-0 z-[9999] flex justify-end bg-black/60 backdrop-blur-sm transition-opacity" @click.self="emit('close')">
    <div class="w-full max-w-md h-full bg-[#111319] shadow-2xl flex flex-col animate-slide-in">

      <!-- Header -->
      <div class="p-5 border-b border-[#1c1f2a] flex justify-between items-center bg-[#161820]">
        <h2 class="text-white text-xl font-bold font-['Unbounded']">{{ DICT.cart.title }}</h2>
        <button class="text-gray-400 hover:text-white transition-colors" @click="emit('close')">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
        </button>
      </div>

      <!-- Body -->
      <div class="flex-1 overflow-y-auto p-5 scrollbar-thin">
        <div v-if="cartStore.isCartLoading && !cartStore.cart?.items?.length" class="text-center py-10 text-gray-500">{{ DICT.cart.loading }}</div>
        <div v-else-if="!cartStore.cart?.items?.length" class="text-center py-10 flex flex-col items-center gap-4">
          <div class="w-20 h-20 bg-[#1c1f2a] rounded-full flex items-center justify-center text-gray-500">
            <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/></svg>
          </div>
          <p class="text-gray-400">{{ DICT.cart.empty }}</p>
        </div>
        
        <div v-else class="flex flex-col gap-4">
          <div class="flex justify-between items-center mb-2">
            <span class="text-sm text-gray-400">{{ DICT.cart.itemsCount }} {{ cartStore.cart.items.length }}</span>
            <button class="text-xs text-red-400 hover:text-red-300 transition-colors" @click="handleClear">{{ DICT.cart.clearAll }}</button>
          </div>

          <!-- Grouped by store -->
          <div v-for="(group, mId) in cartStore.groupedItems" :key="mId" class="mb-3">
            <!-- Store header -->
            <div class="flex items-center gap-3 mb-3 p-3 bg-[#161820] rounded-xl border border-white/5">
              <label class="relative flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  :checked="isStoreAllSelected(group.items)"
                  :indeterminate="isStorePartiallySelected(Number(mId), group.items)"
                  class="peer sr-only"
                  @change="cartStore.toggleStoreSelection(Number(mId), group.items)"
                />
                <div class="w-5 h-5 rounded border-2 border-gray-500 peer-checked:border-orange-500 peer-checked:bg-orange-500 flex items-center justify-center transition-all">
                  <svg v-if="isStoreAllSelected(group.items)" class="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="3"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/></svg>
                  <div v-else-if="isStorePartiallySelected(Number(mId), group.items)" class="w-2.5 h-0.5 bg-orange-500 rounded"></div>
                </div>
              </label>
              <div class="flex items-center gap-2 cursor-pointer" @click="goToStore(group.items[0]?.merchantId)">
                <svg class="w-4 h-4 text-orange-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M13.5 21v-7.5a.75.75 0 01.75-.75h3a.75.75 0 01.75.75V21m-4.5 0H2.36m11.14 0H18m0 0h3.64m-1.39 0V9.349m-16.5 11.65V9.35m0 0a3.001 3.001 0 003.75-.615A2.993 2.993 0 009.75 9.75c.896 0 1.7-.393 2.25-1.016a2.993 2.993 0 002.25 1.016c.896 0 1.7-.393 2.25-1.016A3.001 3.001 0 0021 9.349m-18 0a2.994 2.994 0 00.615-3.75L5.25 3h13.5l1.635 2.599A2.994 2.994 0 0021 9.349"/></svg>
                <span class="text-white font-semibold text-sm hover:text-orange-400 transition-colors">{{ group.storeName }}</span>
                <span class="text-xs text-gray-500">({{ group.items.length }})</span>
              </div>
            </div>

            <!-- Items in store group -->
            <div class="flex flex-col gap-3 pl-2">
              <div v-for="item in group.items" :key="item.productId" class="p-3 bg-[#1c1f2a] rounded-xl border border-white/5 flex gap-3 relative group">
                <!-- Item checkbox -->
                <label class="relative flex items-start pt-1 cursor-pointer shrink-0">
                  <input
                    type="checkbox"
                    :checked="isItemSelected(item.productId)"
                    class="peer sr-only"
                    @change="cartStore.toggleItemSelection(item)"
                  />
                  <div class="w-4 h-4 rounded border-2 border-gray-500 peer-checked:border-orange-500 peer-checked:bg-orange-500 flex items-center justify-center transition-all">
                    <svg v-if="isItemSelected(item.productId)" class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="3"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/></svg>
                  </div>
                </label>

                <img :src="item.image || 'https://via.placeholder.com/80'" class="w-20 h-20 object-cover rounded-lg bg-white/5 cursor-pointer hover:opacity-80 transition-opacity shrink-0" @click="goToProduct(item.productId)" />
                <div class="flex flex-col flex-1 min-w-0 justify-between">
                  <div class="pr-6 min-w-0">
                    <h3 class="text-sm font-semibold text-white leading-tight line-clamp-2 cursor-pointer hover:text-orange-400 transition-colors" @click="goToProduct(item.productId)">{{ item.title }}</h3>
                    <p class="text-sm text-gray-400 mt-1 truncate">{{ item.description || 'Опис відсутній' }}</p>
                  </div>
                  <div class="flex justify-between items-end mt-2">
                    <div class="flex items-center bg-[#0f1117] rounded-lg border border-white/10">
                      <button class="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-white" @click="changeQuantity(item, -1)">−</button>
                      <span class="w-8 text-center text-sm font-medium text-white">{{ item.quantity }}</span>
                      <button 
                        class="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-white transition-colors disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:text-gray-400" 
                        :disabled="item.stockQuantity !== undefined && item.quantity >= item.stockQuantity"
                        @click="changeQuantity(item, 1)"
                      >+</button>
                    </div>
                    <div class="text-right shrink-0 whitespace-nowrap">
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
        </div>
      </div>

      <!-- Footer -->
      <div v-if="cartStore.cart?.items?.length" class="p-5 bg-[#161820] border-t border-[#1c1f2a]">
        <div class="flex justify-between items-center mb-1">
          <span class="text-gray-500 text-xs">{{ DICT.cart.toPay }}</span>
          <span v-if="cartStore.selectedProductIds.length === 0" class="text-sm text-gray-500">Виберіть товари</span>
        </div>
        <div class="flex justify-between items-center mb-4">
          <span class="text-gray-400 text-sm">Обрано: {{ cartStore.selectedProductIds.length }}</span>
          <span class="text-2xl text-white font-bold font-['Unbounded']">{{ fmt(cartStore.selectedTotalAmount) }}</span>
        </div>
        <button 
          class="w-full py-4 bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-400 hover:to-emerald-500 text-white font-bold rounded-xl shadow-lg transition-all active:scale-95 uppercase tracking-wide disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:from-emerald-500 disabled:hover:to-emerald-600 disabled:active:scale-100"
          :disabled="cartStore.selectedProductIds.length === 0"
          @click="goToCheckout"
        >
          {{ DICT.cart.checkoutBtn }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-slide-in { animation: slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
@keyframes slideIn { from { transform: translateX(100%); } to { transform: translateX(0); } }
</style>
