<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiClient } from '../../api/axios'

interface StoreItem {
  id: number
  name: string
  contactPhone: string
  status: string
  createdAt: string
}

const stores = ref<StoreItem[]>([])
const isLoading = ref(false)

async function fetchStores() {
  isLoading.value = true
  try {
    const response = await apiClient.get('/stores', { params: { size: 100 } })
    stores.value = response.data.content || []
  } catch (error) {
    console.error('Failed to fetch stores', error)
  } finally {
    isLoading.value = false
  }
}

async function handleStatusChange(storeId: number, currentStatus: string) {
  const newStatus = (currentStatus === 'ACTIVE' || currentStatus === 'CLOSED') ? 'BLOCKED' : 'ACTIVE'
  const actionText = newStatus === 'BLOCKED' ? 'заблокувати' : 'активувати'
  
  if (!confirm(`Ви дійсно хочете ${actionText} цей магазин?`)) return
  
  try {
    await apiClient.patch(`/stores/${storeId}/status`, { status: newStatus })
    await fetchStores()
  } catch (error: any) {
    alert(error.response?.data?.message || 'Помилка при зміні статусу')
  }
}

async function handleDelete(storeId: number) {
  if (!confirm('Ви дійсно хочете видалити цей магазин?')) return
  
  try {
    await apiClient.delete(`/stores/${storeId}`)
    await fetchStores()
  } catch (error: any) {
    alert(error.response?.data?.message || 'Помилка при видаленні')
  }
}

onMounted(fetchStores)
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex justify-between items-center mb-4">
      <span class="text-gray-100 text-xl font-black font-['Unbounded']">Керування магазинами</span>
      <button @click="fetchStores" class="p-2 bg-gray-800 rounded-xl hover:bg-gray-700 transition-colors">
        <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
      </button>
    </div>

    <div v-if="isLoading" class="text-gray-400 animate-pulse">Завантаження...</div>
    <div v-else-if="stores.length === 0" class="text-gray-500">Магазинів не знайдено.</div>
    
    <div v-else class="flex flex-col gap-4">
      <div v-for="store in stores" :key="store.id" class="p-5 bg-gray-900 rounded-2xl outline outline-1 outline-[#1e2d3d] flex flex-col sm:flex-row justify-between items-center gap-4">
        <div class="flex flex-col gap-1 w-full sm:w-auto">
          <div class="flex items-center gap-3">
            <span class="text-gray-100 font-bold">{{ store.name }}</span>
            <span v-if="store.status === 'ACTIVE'" class="px-2 py-0.5 bg-emerald-500/20 text-emerald-400 text-[10px] font-bold rounded uppercase">Активний</span>
            <span v-else-if="store.status === 'PENDING'" class="px-2 py-0.5 bg-yellow-500/20 text-yellow-400 text-[10px] font-bold rounded uppercase">Очікує</span>
            <span v-else-if="store.status === 'BLOCKED'" class="px-2 py-0.5 bg-red-500/20 text-red-400 text-[10px] font-bold rounded uppercase">Заблоковано</span>
            <span v-else-if="store.status === 'CLOSED'" class="px-2 py-0.5 bg-gray-500/20 text-gray-400 text-[10px] font-bold rounded uppercase">Закрито</span>
          </div>
          <span class="text-gray-500 text-xs">ID: {{ store.id }} • {{ store.contactPhone }}</span>
        </div>
        
        <div class="flex items-center gap-2 w-full sm:w-auto">
          <button 
            v-if="store.status === 'ACTIVE' || store.status === 'CLOSED'"
            class="px-4 py-2 bg-orange-500/15 text-orange-400 text-xs font-semibold rounded-xl hover:bg-orange-500/25 transition-colors"
            @click="handleStatusChange(store.id, store.status)"
          >
            Заблокувати
          </button>
          <button 
            v-else
            class="px-4 py-2 bg-emerald-500/15 text-emerald-400 text-xs font-semibold rounded-xl hover:bg-emerald-500/25 transition-colors"
            @click="handleStatusChange(store.id, store.status)"
          >
            Активувати
          </button>
          <button 
            class="px-4 py-2 bg-red-500/15 text-red-400 text-xs font-semibold rounded-xl hover:bg-red-500/25 transition-colors"
            @click="handleDelete(store.id)"
          >
            Видалити
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
