<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiClient } from '../../api/axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const orders = ref<any[]>([])
const isLoading = ref(true)
const error = ref('')

async function fetchOrders() {
  isLoading.value = true
  error.value = ''
  try {
    const response = await apiClient.get('/orders', {
      params: { page: 0, size: 20, sort: 'createdAt,DESC' }
    })
    orders.value = response.data?.content || []
  } catch (e: any) {
    error.value = 'Не вдалося завантажити історію замовлень'
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchOrders)

const fmtDate = (d: string) => {
  if (!d) return ''
  return new Date(d).toLocaleDateString('uk-UA', { day: '2-digit', month: 'long', year: 'numeric' })
}

const fmtPrice = (p: number) => (p || 0).toLocaleString('uk-UA') + ' ₴'

function getPaymentBadge(status: string) {
  switch (status) {
    case 'PAID': return { text: 'Оплачено', class: 'bg-green-500/20 text-green-400' }
    case 'PENDING': return { text: 'В обробці', class: 'bg-yellow-500/20 text-yellow-400' }
    case 'FAILED': return { text: 'Помилка', class: 'bg-red-500/20 text-red-400' }
    case 'REFUNDED': return { text: 'Повернено', class: 'bg-gray-500/20 text-gray-400' }
    default: return { text: status, class: 'bg-gray-500/20 text-gray-400' }
  }
}

function goToOrder(id: number) {
  router.push(`/order/${id}`)
}
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="mb-2">
      <span class="text-gray-100 text-xl font-black font-['Unbounded']">Мої замовлення</span>
    </div>

    <div v-if="isLoading" class="flex justify-center py-20">
      <span class="text-slate-500 animate-pulse font-['Onest']">Завантаження замовлень...</span>
    </div>

    <div v-else-if="error" class="text-center py-20 text-red-400 font-['Onest']">
      {{ error }}
    </div>

    <div v-else-if="orders.length === 0" class="flex flex-col items-center justify-center py-20 gap-4 text-center">
      <div class="w-20 h-20 bg-white/5 rounded-full flex items-center justify-center">
        <svg class="w-10 h-10 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <span class="text-gray-400 text-sm font-['Onest']">У вас ще немає замовлень</span>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div 
        v-for="order in orders" 
        :key="order.id"
        class="bg-[#131720] rounded-2xl p-5 outline outline-1 outline-[#1a2235] flex flex-col gap-4 hover:outline-orange-500/30 transition-all cursor-pointer group"
        @click="goToOrder(order.id)"
      >
        <div class="flex justify-between items-start">
          <div class="flex flex-col gap-1">
            <span class="text-white font-bold font-['Unbounded'] tracking-wide line-clamp-1">
              {{ (order.items_preview || order.items)?.[0]?.title_snapshot || 'Замовлення' }}
              <span v-if="(order.items_preview || order.items)?.length > 1" class="text-slate-400 text-sm font-['Onest'] font-normal">
                та ще {{ (order.items_preview || order.items).length - 1 }}
              </span>
            </span>
            <div class="flex items-center gap-2">
              <span class="text-slate-500 text-xs font-['Onest']">№{{ order.id }}</span>
              <span class="w-1 h-1 rounded-full bg-slate-700"></span>
              <span class="text-slate-500 text-xs font-['Onest']">{{ fmtDate(order.created_at || order.createdAt) }}</span>
            </div>
          </div>
          <span 
            class="px-2.5 py-1 rounded text-[10px] font-bold uppercase tracking-wide"
            :class="getPaymentBadge(order.payment_status || order.paymentStatus).class"
          >
            {{ getPaymentBadge(order.payment_status || order.paymentStatus).text }}
          </span>
        </div>

        <div class="h-px bg-white/5" />

        <div class="flex justify-between items-center">
          <div class="flex -space-x-3">
            <div 
              v-for="(item, idx) in (order.items_preview || order.items || []).filter((i: any) => i.image_snapshot || i.imageSnapshot).slice(0, 3)" 
              :key="idx"
              class="w-12 h-12 rounded-lg bg-[#0d1117] border-2 border-[#131720] overflow-hidden flex items-center justify-center relative z-10"
            >
              <img v-if="item.image_snapshot || item.imageSnapshot" :src="item.image_snapshot || item.imageSnapshot" class="w-full h-full object-cover" />
              <svg v-else class="w-5 h-5 text-slate-600" viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 15l5-5 5 5 8-8"/></svg>
            </div>
            <div v-if="(order.items_preview || order.items || []).length > 3" class="w-12 h-12 rounded-lg bg-gray-800 border-2 border-[#131720] flex items-center justify-center relative z-0">
              <span class="text-xs text-white font-bold">+{{ (order.items_preview || order.items || []).length - 3 }}</span>
            </div>
          </div>
          <div class="flex flex-col items-end">
            <span class="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Сума</span>
            <span class="text-orange-400 font-bold font-['Unbounded']">{{ fmtPrice(order.total_amount || order.finalPrice) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
