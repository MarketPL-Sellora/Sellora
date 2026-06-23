<script setup lang="ts">
import { useConfirmStore } from '../../state/confirmStore'

const confirmStore = useConfirmStore()
</script>

<template>
  <div v-if="confirmStore.isOpen" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-[9999] flex items-center justify-center transition-opacity">
    <div class="bg-stone-900 border border-stone-700 rounded-xl p-6 shadow-2xl w-full max-w-md animate-slide-in">
      <h3 class="text-white text-xl font-bold font-['Unbounded'] mb-2">{{ confirmStore.title }}</h3>
      <p class="text-gray-300 text-sm mb-6">{{ confirmStore.message }}</p>
      
      <div class="flex gap-3 justify-end">
        <button 
          class="px-4 py-2 bg-stone-700 hover:bg-stone-600 text-white rounded-lg transition-colors font-medium text-sm"
          @click="confirmStore.cancel()"
        >
          Скасувати
        </button>
        <button 
          class="px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white rounded-lg transition-colors font-medium text-sm"
          @click="confirmStore.confirm()"
        >
          Підтвердити
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-slide-in { animation: slideIn 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
@keyframes slideIn { 
  from { transform: translateY(20px); opacity: 0; } 
  to { transform: translateY(0); opacity: 1; } 
}
</style>
