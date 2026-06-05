<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { apiClient } from '../../api/axios'

// ─── Типи ─────────────────────────────────────────────────────────────────────
interface ShippingCarrier {
  id: number
  name: string
  code: string
  is_active: boolean
}

// ─── Стан ─────────────────────────────────────────────────────────────────────
const carriers = ref<ShippingCarrier[]>([])
const isLoading = ref(false)
const isSaving = ref(false)
const isModalOpen = ref(false)
const editingId = ref<number | null>(null)
const errorMessage = ref('')

const form = reactive<{ name: string; code: string; is_active: boolean }>({
  name: '',
  code: '',
  is_active: true,
})

// ─── Завантаження списку ──────────────────────────────────────────────────────
async function fetchCarriers() {
  isLoading.value = true
  try {
    const response = await apiClient.get('/shipping_carriers')
    carriers.value = Array.isArray(response.data) ? response.data : response.data.content || []
  } catch (error) {
    console.error('Failed to fetch shipping carriers', error)
  } finally {
    isLoading.value = false
  }
}

// ─── Відкрити модалку для створення ───────────────────────────────────────────
function openCreateModal() {
  editingId.value = null
  form.name = ''
  form.code = ''
  form.is_active = true
  errorMessage.value = ''
  isModalOpen.value = true
}

// ─── Відкрити модалку для редагування ─────────────────────────────────────────
function openEditModal(carrier: ShippingCarrier) {
  editingId.value = carrier.id
  form.name = carrier.name
  form.code = carrier.code
  form.is_active = carrier.is_active
  errorMessage.value = ''
  isModalOpen.value = true
}

// ─── Закрити модалку ──────────────────────────────────────────────────────────
function closeModal() {
  isModalOpen.value = false
  editingId.value = null
  errorMessage.value = ''
}

// ─── Збереження (створення або оновлення) ─────────────────────────────────────
async function handleSave() {
  errorMessage.value = ''
  isSaving.value = true
  try {
    const payload = {
      name: form.name,
      code: form.code,
      is_active: form.is_active,
    }

    if (editingId.value !== null) {
      await apiClient.put(`/shipping_carriers/${editingId.value}`, payload)
    } else {
      await apiClient.post('/shipping_carriers', payload)
    }

    closeModal()
    await fetchCarriers()
  } catch (error: any) {
    if (error.response?.status === 409) {
      errorMessage.value =
        error.response?.data?.message ||
        'Поштова служба з таким кодом або назвою вже існує.'
    } else {
      errorMessage.value =
        error.response?.data?.message || 'Помилка при збереженні. Спробуйте ще раз.'
    }
  } finally {
    isSaving.value = false
  }
}

// ─── Ініціалізація ────────────────────────────────────────────────────────────
onMounted(fetchCarriers)
</script>

<template>
  <div class="flex flex-col gap-6">
    <!-- ── Заголовок + кнопка «Створити» ──────────────────────────────── -->
    <div class="flex items-center justify-between mb-4">
      <span class="text-gray-100 text-xl font-black font-['Unbounded']">
        Поштові служби
      </span>
      <div class="flex items-center gap-2">
        <button
          @click="fetchCarriers"
          class="p-2.5 bg-gray-800 rounded-xl hover:bg-gray-700 transition-colors"
          title="Оновити"
        >
          <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
        </button>
        <button
          class="px-5 py-2.5 bg-orange-500 rounded-xl text-white text-sm font-semibold hover:bg-orange-400 transition-colors"
          @click="openCreateModal"
        >
          Створити
        </button>
      </div>
    </div>

    <!-- ── Loader ───────────────────────────────────────────────────────── -->
    <div v-if="isLoading" class="flex justify-center py-20 animate-pulse text-slate-500">
      Завантажуємо поштові служби...
    </div>

    <!-- ── Таблиця поштових служб ────────────────────────────────────────── -->
    <div v-else class="overflow-x-auto rounded-2xl outline outline-1 outline-[#1e2d3d]">
      <table class="w-full text-sm text-left">
        <thead class="bg-[#131720] text-gray-400 uppercase text-xs tracking-wider">
          <tr>
            <th class="px-5 py-3.5">ID</th>
            <th class="px-5 py-3.5">Назва</th>
            <th class="px-5 py-3.5">Код</th>
            <th class="px-5 py-3.5">Статус</th>
            <th class="px-5 py-3.5 text-right">Дії</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[#1e2d3d]">
          <tr
            v-for="carrier in carriers"
            :key="carrier.id"
            class="bg-gray-900 hover:bg-[#151c28] transition-colors"
          >
            <td class="px-5 py-3 text-gray-300 font-mono">{{ carrier.id }}</td>
            <td class="px-5 py-3 text-gray-100 font-medium">{{ carrier.name }}</td>
            <td class="px-5 py-3 text-gray-400 font-mono">{{ carrier.code }}</td>
            <td class="px-5 py-3">
              <span
                v-if="carrier.is_active"
                class="px-2.5 py-1 bg-emerald-500/15 text-emerald-400 text-[10px] font-bold rounded-md uppercase tracking-wide"
              >
                Активна
              </span>
              <span
                v-else
                class="px-2.5 py-1 bg-gray-500/15 text-gray-400 text-[10px] font-bold rounded-md uppercase tracking-wide"
              >
                Неактивна
              </span>
            </td>
            <td class="px-5 py-3 text-right">
              <button
                class="px-3 py-1.5 rounded-lg text-xs font-medium text-orange-400 bg-orange-500/10 hover:bg-orange-500/20 border border-orange-500/20 transition-colors"
                @click="openEditModal(carrier)"
              >
                Редагувати
              </button>
            </td>
          </tr>
          <tr v-if="carriers.length === 0">
            <td colspan="5" class="px-5 py-12 text-center text-gray-500">
              Поштових служб ще немає.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ── Модалка створення / редагування ──────────────────────────────── -->
    <Teleport to="body">
      <div
        v-if="isModalOpen"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
        @click.self="closeModal"
      >
        <div class="w-full max-w-md bg-[#131720] rounded-2xl p-6 outline outline-1 outline-[#1e2d3d] shadow-2xl">
          <h3 class="text-gray-100 text-lg font-bold font-['Unbounded'] mb-5">
            {{ editingId !== null ? 'Редагувати поштову службу' : 'Нова поштова служба' }}
          </h3>

          <!-- Повідомлення про помилку -->
          <div
            v-if="errorMessage"
            class="mb-4 px-4 py-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm"
          >
            {{ errorMessage }}
          </div>

          <!-- Поле: Назва -->
          <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">
            Назва
          </label>
          <input
            v-model="form.name"
            type="text"
            placeholder="Наприклад: Нова Пошта"
            class="w-full px-4 py-2.5 mb-4 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors placeholder:text-gray-600"
          />

          <!-- Поле: Код -->
          <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">
            Код
          </label>
          <input
            v-model="form.code"
            type="text"
            placeholder="Наприклад: nova_poshta"
            class="w-full px-4 py-2.5 mb-4 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors placeholder:text-gray-600"
          />

          <!-- Поле: Статус -->
          <label class="flex items-center gap-3 mb-6 cursor-pointer select-none group">
            <div class="relative">
              <input
                v-model="form.is_active"
                type="checkbox"
                class="sr-only peer"
              />
              <div
                class="w-10 h-5 rounded-full transition-colors duration-200 bg-gray-700 peer-checked:bg-orange-500"
              />
              <div
                class="absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white transition-transform duration-200 peer-checked:translate-x-5"
              />
            </div>
            <span class="text-gray-400 text-xs font-semibold uppercase tracking-wider group-hover:text-gray-300 transition-colors">
              Активна
            </span>
          </label>

          <!-- Кнопки -->
          <div class="flex justify-end gap-3">
            <button
              class="px-5 py-2.5 rounded-xl text-sm text-gray-400 hover:text-white border border-[#1e2d3d] hover:bg-white/5 transition-colors"
              @click="closeModal"
              :disabled="isSaving"
            >
              Скасувати
            </button>
            <button
              class="px-5 py-2.5 rounded-xl text-sm text-white font-semibold bg-orange-500 hover:bg-orange-400 disabled:opacity-40 transition-colors"
              :disabled="!form.name.trim() || !form.code.trim() || isSaving"
              @click="handleSave"
            >
              <span v-if="isSaving" class="flex items-center gap-2">
                <svg class="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                Збереження...
              </span>
              <span v-else>Зберегти</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
