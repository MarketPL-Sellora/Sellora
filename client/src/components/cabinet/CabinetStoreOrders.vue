<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiClient } from '../../api/axios'
import { useUserStore } from '../../state/userStore'

const userStore = useUserStore()
const orders = ref<any[]>([])
const isLoading = ref(true)
const error = ref('')

// Стан модалки
const isModalOpen = ref(false)
const selectedOrder = ref<any>(null)
const isLoadingDetails = ref(false)
const editForm = ref({
  payment_status: '',
  shipping_status: '',
  tracking_number: ''
})

const isPaymentOpen = ref(false)
const isShippingOpen = ref(false)
const isSaving = ref(false)
const paymentStatuses = ['PENDING', 'PAID', 'CANCELLED']
const shippingStatuses = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED']

async function fetchOrders() {
  isLoading.value = true
  error.value = ''
  try {
    const storeId = userStore.sellerStore?.id
    if (!storeId) {
      error.value = 'Магазин не знайдено'
      return
    }
    const response = await apiClient.get('/stores/' + storeId + '/orders', {
      params: { page: 0, size: 20, sort: 'createdAt,DESC' }
    })
    orders.value = response.data?.content || []
  } catch (e: any) {
    error.value = 'Не вдалося завантажити замовлення магазину'
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchOrders)

const fmtPrice = (p: number) => (p || 0).toLocaleString('uk-UA') + ' ₴'

async function openManageModal(order: any) {
  selectedOrder.value = order
  isModalOpen.value = true
  isLoadingDetails.value = true
  try {
    const res = await apiClient.get(`/orders/${order.id}`)
    selectedOrder.value = res.data
    editForm.value = {
      payment_status: res.data.payment_status || res.data.paymentStatus || '',
      shipping_status: res.data.shipping_status || res.data.shippingStatus || '',
      tracking_number: res.data.tracking_number || res.data.trackingNumber || ''
    }
  } catch (e) {
    console.error('Помилка завантаження деталей замовлення', e)
  } finally {
    isLoadingDetails.value = false
  }
}

function formatAddress(addressObj: any) {
  if (!addressObj) return 'Не вказано'
  try {
    const addr = typeof addressObj === 'string' ? JSON.parse(addressObj) : addressObj
    if (addr.branch) return `${addr.city || ''}, ${addr.branch}`
    if (addr.street) return `${addr.city || ''}, вул. ${addr.street}, буд. ${addr.house || ''}`
    return addr.city || 'Адреса не вказана'
  } catch (e) {
    return 'Помилка форматування адреси'
  }
}

function closeManageModal() {
  isModalOpen.value = false
  selectedOrder.value = null
}

async function saveOrderChanges() {
  if (!selectedOrder.value) return;
  isSaving.value = true;
  try {
    const payload = {
      payment_status: editForm.value.payment_status || null,
      shipping_status: editForm.value.shipping_status || null,
      tracking_number: editForm.value.tracking_number || null
    };

    await apiClient.put(`/orders/${selectedOrder.value.id}`, payload);
    alert('Зміни успішно збережено!');
    closeManageModal();
    fetchOrders(); // Оновлюємо таблицю після успішного збереження
  } catch (e: any) {
    alert(e.response?.data?.message || 'Помилка при збереженні змін');
  } finally {
    isSaving.value = false;
  }
}
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="mb-2">
      <span class="text-gray-100 text-xl font-black font-['Unbounded']">Замовлення магазину</span>
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

    <div v-else class="overflow-x-auto bg-[#131720] rounded-2xl outline outline-1 outline-[#1a2235]">
      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="border-b border-[#1e2d3d] bg-white/5 text-slate-400 text-xs font-medium font-['Onest']">
            <th class="px-4 py-3 font-normal">№</th>
            <th class="px-4 py-3 font-normal">Покупець</th>
            <th class="px-4 py-3 font-normal">Сума</th>
            <th class="px-4 py-3 font-normal">Оплата</th>
            <th class="px-4 py-3 font-normal">Доставка</th>
            <th class="px-4 py-3 font-normal text-right">Дії</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[#1e2d3d]">
          <tr v-for="order in orders" :key="order.id" class="text-sm font-['Onest'] text-gray-200 hover:bg-white/[0.02] transition-colors">
            <td class="px-4 py-3 font-bold">#{{ order.id }}</td>
            <td class="px-4 py-3">
              <div class="flex flex-col">
                <span class="text-white">{{ order.buyer_name }} {{ order.buyer_surname }}</span>
                <span class="text-slate-500 text-xs">{{ order.buyer_phone }}</span>
              </div>
            </td>
            <td class="px-4 py-3 font-bold text-orange-400">{{ fmtPrice(order.total_amount || order.finalPrice) }}</td>
            <td class="px-4 py-3">
              <span class="px-2 py-1 rounded text-[10px] font-bold uppercase tracking-wide bg-gray-800 text-gray-400">
                {{ order.payment_status || order.paymentStatus }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span class="px-2 py-1 rounded text-[10px] font-bold uppercase tracking-wide bg-blue-500/10 text-blue-400">
                {{ order.shipping_status || order.shippingStatus }}
              </span>
            </td>
            <td class="px-4 py-3 text-right">
              <button 
                @click="openManageModal(order)"
                class="px-3 py-1.5 rounded-lg bg-orange-500/10 text-orange-500 text-xs font-bold hover:bg-orange-500/20 transition-colors"
              >
                Керувати
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <!-- Modal -->
  <div v-if="isModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
    <div class="w-full max-w-md bg-[#131720] rounded-2xl outline outline-1 outline-[#1a2235] p-6 flex flex-col gap-6 shadow-2xl">
      <div class="flex justify-between items-center">
        <h3 class="text-white text-lg font-bold font-['Unbounded']">Замовлення #{{ selectedOrder?.id }}</h3>
        <button @click="closeManageModal" class="text-gray-500 hover:text-white transition-colors">
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </button>
      </div>

      <div class="flex flex-col gap-4">
        <!-- Info -->
        <div class="p-3 bg-white/5 rounded-xl border border-white/10 flex flex-col gap-1">
          <span class="text-slate-400 text-xs font-['Onest']">Адреса доставки</span>
          <p class="text-gray-200 text-sm font-['Onest']">
            {{ isLoadingDetails ? 'Завантаження адреси...' : formatAddress(selectedOrder?.delivery_address) }}
          </p>
        </div>

        <!-- Payment Status -->
        <div class="flex flex-col gap-1.5">
          <label class="text-slate-400 text-xs font-['Onest']">Статус оплати</label>
          <div class="relative">
            <div 
              class="w-full bg-white/5 outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] rounded-lg p-2.5 flex justify-between items-center transition-all"
              :class="selectedOrder?.payment_method === 'ONLINE_CARD' ? 'opacity-60 cursor-not-allowed bg-gray-800' : 'cursor-pointer hover:outline-orange-500/50'"
              @click="() => {
                if (selectedOrder?.payment_method === 'ONLINE_CARD') return;
                isPaymentOpen = !isPaymentOpen;
                isShippingOpen = false;
              }"
            >
              <span>{{ editForm.payment_status || 'Оберіть статус' }}</span>
              <svg v-if="selectedOrder?.payment_method !== 'ONLINE_CARD'" class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M6 9l6 6 6-6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </div>
            <p v-if="selectedOrder?.payment_method === 'ONLINE_CARD'" class="text-[10px] text-orange-400 mt-1">
              * Статус змінюється автоматично при оплаті карткою
            </p>
            
            <ul 
              v-if="isPaymentOpen" 
              class="absolute z-50 w-full mt-1 bg-[#131720] border border-gray-700 rounded-lg shadow-lg max-h-48 overflow-y-auto"
            >
              <li 
                v-for="status in paymentStatuses" 
                :key="status"
                class="p-2.5 text-sm text-gray-200 hover:bg-white/10 cursor-pointer font-['Onest']"
                @click="editForm.payment_status = status; isPaymentOpen = false"
              >
                {{ status }}
              </li>
            </ul>
          </div>
        </div>

        <!-- Shipping Status -->
        <div class="flex flex-col gap-1.5">
          <label class="text-slate-400 text-xs font-['Onest']">Статус доставки</label>
          <div class="relative">
            <div 
              class="w-full bg-white/5 outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] rounded-lg p-2.5 cursor-pointer flex justify-between items-center transition-all hover:outline-orange-500/50"
              @click="isShippingOpen = !isShippingOpen; isPaymentOpen = false"
            >
              <span>{{ editForm.shipping_status || 'Оберіть статус' }}</span>
              <svg class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M6 9l6 6 6-6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </div>
            
            <ul 
              v-if="isShippingOpen" 
              class="absolute z-50 w-full mt-1 bg-[#131720] border border-gray-700 rounded-lg shadow-lg max-h-48 overflow-y-auto"
            >
              <li 
                v-for="status in shippingStatuses" 
                :key="status"
                class="p-2.5 text-sm text-gray-200 hover:bg-white/10 cursor-pointer font-['Onest']"
                @click="editForm.shipping_status = status; isShippingOpen = false"
              >
                {{ status }}
              </li>
            </ul>
          </div>
        </div>

        <!-- Tracking Number -->
        <div class="flex flex-col gap-1.5">
          <label class="text-slate-400 text-xs font-['Onest']">Трек-номер</label>
          <input 
            v-model="editForm.tracking_number"
            type="text"
            placeholder="Введіть ТТН"
            class="w-full px-3 py-2.5 bg-white/5 rounded-lg outline outline-1 outline-white/10 text-slate-200 text-sm font-['Onest'] transition-all focus:outline-orange-500 focus:outline-none"
          />
        </div>
      </div>

      <!-- Actions -->
      <div class="flex justify-end gap-3 pt-2">
        <button 
          @click="closeManageModal"
          class="px-4 py-2 rounded-xl text-gray-400 text-sm font-medium hover:text-white transition-colors"
        >
          Скасувати
        </button>
        <button 
          @click="saveOrderChanges"
          :disabled="isSaving"
          class="px-4 py-2 rounded-xl bg-orange-500 text-white text-sm font-bold hover:bg-orange-600 transition-colors shadow-lg shadow-orange-500/20 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ isSaving ? 'Збереження...' : 'Зберегти' }}
        </button>
      </div>
    </div>
  </div>
</template>
