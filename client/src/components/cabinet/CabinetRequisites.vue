<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { apiClient } from '../../api/axios'

// ─── Типи ─────────────────────────────────────────────────────────────────────
interface MerchantRequisite {
  id: number
  edrpou: string
  iban: string
  bank_name: string
  is_primary: boolean
}

// ─── Стан ─────────────────────────────────────────────────────────────────────
const requisites = ref<MerchantRequisite[]>([])
const isLoading = ref(false)
const isSaving = ref(false)
const isModalOpen = ref(false)
const editingId = ref<number | null>(null)
const errorMessage = ref('')

const form = reactive({
  edrpou: '',
  iban: '',
  bank_name: '',
  is_primary: false,
})

// ─── Завантаження списку ──────────────────────────────────────────────────────
async function fetchRequisites() {
  isLoading.value = true
  try {
    const response = await apiClient.get('/merchant_requisites')
    requisites.value = Array.isArray(response.data) ? response.data : response.data.content || []
  } catch (error) {
    console.error('Failed to fetch merchant requisites', error)
  } finally {
    isLoading.value = false
  }
}

// ─── Відкрити модалку для створення ───────────────────────────────────────────
function openCreateModal() {
  editingId.value = null
  form.edrpou = ''
  form.iban = ''
  form.bank_name = ''
  form.is_primary = false
  errorMessage.value = ''
  isModalOpen.value = true
}

// ─── Відкрити модалку для редагування ─────────────────────────────────────────
function openEditModal(req: MerchantRequisite) {
  editingId.value = req.id
  form.edrpou = req.edrpou
  form.iban = req.iban
  form.bank_name = req.bank_name
  form.is_primary = req.is_primary
  errorMessage.value = ''
  isModalOpen.value = true
}

// ─── Закрити модалку ──────────────────────────────────────────────────────────
function closeModal() {
  isModalOpen.value = false
  editingId.value = null
  errorMessage.value = ''
}

// ─── Збереження ───────────────────────────────────────────────────────────────
async function handleSave() {
  errorMessage.value = ''
  isSaving.value = true
  try {
    const payload = {
      edrpou: form.edrpou.trim(),
      iban: form.iban.trim(),
      bank_name: form.bank_name.trim(),
      is_primary: form.is_primary,
    }

    if (editingId.value !== null) {
      await apiClient.put(`/merchant_requisites/${editingId.value}`, payload)
    } else {
      await apiClient.post('/merchant_requisites', payload)
    }

    closeModal()
    await fetchRequisites()
  } catch (error: any) {
    errorMessage.value =
      error.response?.data?.message || 'Помилка при збереженні реквізитів. Спробуйте ще раз.'
  } finally {
    isSaving.value = false
  }
}

// ─── Видалення ────────────────────────────────────────────────────────────────
async function handleDelete(id: number) {
  if (!confirm('Ви дійсно хочете видалити цей платіжний реквізит?')) return
  try {
    await apiClient.delete(`/merchant_requisites/${id}`)
    await fetchRequisites()
  } catch (error: any) {
    alert(error.response?.data?.message || 'Помилка при видаленні реквізиту.')
  }
}

// ─── Ініціалізація ────────────────────────────────────────────────────────────
onMounted(fetchRequisites)
</script>

<template>
  <div class="flex flex-col gap-6">
    <!-- ── Заголовок + кнопки ─────────────────────────────────────────── -->
    <div class="flex items-center justify-between mb-4">
      <span class="text-gray-100 text-xl font-black font-['Unbounded']">
        Платіжні реквізити
      </span>
      <div class="flex items-center gap-2">
        <button
          @click="fetchRequisites"
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
          Додати реквізит
        </button>
      </div>
    </div>

    <!-- ── Loader ───────────────────────────────────────────────────────── -->
    <div v-if="isLoading" class="flex justify-center py-20 animate-pulse text-slate-500">
      Завантажуємо реквізити...
    </div>

    <!-- ── Таблиця реквізитів ────────────────────────────────────────────── -->
    <div v-else class="overflow-x-auto rounded-2xl outline outline-1 outline-[#1e2d3d]">
      <table class="w-full text-sm text-left">
        <thead class="bg-[#131720] text-gray-400 uppercase text-xs tracking-wider">
          <tr>
            <th class="px-5 py-3.5">ЄДРПОУ/ІПН</th>
            <th class="px-5 py-3.5">IBAN</th>
            <th class="px-5 py-3.5">Банк</th>
            <th class="px-5 py-3.5">Статус</th>
            <th class="px-5 py-3.5 text-right">Дії</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[#1e2d3d]">
          <tr
            v-for="req in requisites"
            :key="req.id"
            class="bg-gray-900 hover:bg-[#151c28] transition-colors"
          >
            <td class="px-5 py-3 text-gray-100 font-mono font-medium">{{ req.edrpou }}</td>
            <td class="px-5 py-3 text-gray-300 font-mono text-xs">{{ req.iban }}</td>
            <td class="px-5 py-3 text-gray-300">{{ req.bank_name }}</td>
            <td class="px-5 py-3">
              <span
                v-if="req.is_primary"
                class="px-2.5 py-1 bg-orange-500/15 text-orange-400 text-[10px] font-bold rounded-md uppercase tracking-wide"
              >
                Основний
              </span>
              <span
                v-else
                class="px-2.5 py-1 bg-gray-500/15 text-gray-400 text-[10px] font-bold rounded-md uppercase tracking-wide"
              >
                Додатковий
              </span>
            </td>
            <td class="px-5 py-3 text-right">
              <div class="flex justify-end gap-2">
                <button
                  class="px-3 py-1.5 rounded-lg text-xs font-medium text-orange-400 bg-orange-500/10 hover:bg-orange-500/20 border border-orange-500/20 transition-colors"
                  @click="openEditModal(req)"
                >
                  Редагувати
                </button>
                <button
                  class="px-3 py-1.5 rounded-lg text-xs font-medium text-red-400 bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 transition-colors"
                  @click="handleDelete(req.id)"
                >
                  Видалити
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="requisites.length === 0">
            <td colspan="5" class="px-5 py-12 text-center text-gray-500">
              Платіжних реквізитів ще немає.
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
            {{ editingId !== null ? 'Редагувати реквізит' : 'Новий платіжний реквізит' }}
          </h3>

          <!-- Повідомлення про помилку -->
          <div
            v-if="errorMessage"
            class="mb-4 px-4 py-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm"
          >
            {{ errorMessage }}
          </div>

          <!-- Поле: ЄДРПОУ / ІПН -->
          <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">
            ЄДРПОУ / ІПН
          </label>
          <input
            v-model="form.edrpou"
            type="text"
            maxlength="10"
            placeholder="12345678"
            class="w-full px-4 py-2.5 mb-4 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors placeholder:text-gray-600 font-mono"
          />

          <!-- Поле: IBAN -->
          <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">
            IBAN
          </label>
          <input
            v-model="form.iban"
            type="text"
            maxlength="29"
            placeholder="UA213223130000026007233566001"
            class="w-full px-4 py-2.5 mb-4 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors placeholder:text-gray-600 font-mono"
          />

          <!-- Поле: Назва банку -->
          <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">
            Назва банку
          </label>
          <input
            v-model="form.bank_name"
            type="text"
            maxlength="255"
            placeholder="АТ КБ «ПриватБанк»"
            class="w-full px-4 py-2.5 mb-4 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors placeholder:text-gray-600"
          />

          <!-- Поле: Основний -->
          <label class="flex items-center gap-3 mb-6 cursor-pointer select-none group">
            <div class="relative">
              <input
                v-model="form.is_primary"
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
              Основний реквізит
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
              :disabled="!form.edrpou.trim() || !form.iban.trim() || !form.bank_name.trim() || isSaving"
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
