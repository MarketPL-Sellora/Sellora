<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { apiClient } from '../../api/axios'

// ─── Типи ─────────────────────────────────────────────────────────────────────
interface PromoCode {
  id: number
  code: string
  discount_type: 'PERCENTAGE' | 'FIXED'
  value: number
  start_date: string
  end_date: string
  usage_limit: number
  usage_count: number
  is_active: boolean
}

// ─── Стан ─────────────────────────────────────────────────────────────────────
const promoCodes = ref<PromoCode[]>([])
const isLoading = ref(false)
const isSaving = ref(false)
const isModalOpen = ref(false)
const editingId = ref<number | null>(null)
const errorMessage = ref('')

const form = reactive({
  code: '',
  discount_type: 'PERCENTAGE' as 'PERCENTAGE' | 'FIXED',
  value: 0,
  start_date: '',
  end_date: '',
  usage_limit: 0,
  is_active: true,
})

// ─── Утиліти ──────────────────────────────────────────────────────────────────

/** Конвертує ISO рядок у формат datetime-local для input */
function toLocalDatetime(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** Форматує ISO дату для відображення у таблиці */
function formatDate(iso: string): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return '—'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ─── Завантаження списку ──────────────────────────────────────────────────────
async function fetchPromoCodes() {
  isLoading.value = true
  try {
    const response = await apiClient.get('/promo_codes')
    promoCodes.value = Array.isArray(response.data) ? response.data : response.data.content || []
  } catch (error) {
    console.error('Failed to fetch promo codes', error)
  } finally {
    isLoading.value = false
  }
}

// ─── Відкрити модалку для створення ───────────────────────────────────────────
function openCreateModal() {
  editingId.value = null
  form.code = ''
  form.discount_type = 'PERCENTAGE'
  form.value = 0
  form.start_date = ''
  form.end_date = ''
  form.usage_limit = 0
  form.is_active = true
  errorMessage.value = ''
  isModalOpen.value = true
}

// ─── Відкрити модалку для редагування ─────────────────────────────────────────
function openEditModal(promo: PromoCode) {
  editingId.value = promo.id
  form.code = promo.code
  form.discount_type = promo.discount_type
  form.value = promo.value
  form.start_date = toLocalDatetime(promo.start_date)
  form.end_date = toLocalDatetime(promo.end_date)
  form.usage_limit = promo.usage_limit
  form.is_active = promo.is_active
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
      code: form.code,
      discount_type: form.discount_type,
      value: Number(form.value),
      start_date: form.start_date ? new Date(form.start_date).toISOString() : null,
      end_date: form.end_date ? new Date(form.end_date).toISOString() : null,
      usage_limit: Number(form.usage_limit),
      is_active: form.is_active,
    }

    if (editingId.value !== null) {
      await apiClient.put(`/promo_codes/${editingId.value}`, payload)
    } else {
      await apiClient.post('/promo_codes', payload)
    }

    closeModal()
    await fetchPromoCodes()
  } catch (error: any) {
    errorMessage.value =
      error.response?.data?.message || 'Помилка при збереженні промокоду. Спробуйте ще раз.'
  } finally {
    isSaving.value = false
  }
}

// ─── Ініціалізація ────────────────────────────────────────────────────────────
onMounted(fetchPromoCodes)
</script>

<template>
  <div class="flex flex-col gap-6">
    <!-- ── Заголовок + кнопки ─────────────────────────────────────────── -->
    <div class="flex items-center justify-between mb-4">
      <span class="text-gray-100 text-xl font-black font-['Unbounded']">
        Промокоди
      </span>
      <div class="flex items-center gap-2">
        <button
          @click="fetchPromoCodes"
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
          Додати промокод
        </button>
      </div>
    </div>

    <!-- ── Loader ───────────────────────────────────────────────────────── -->
    <div v-if="isLoading" class="flex justify-center py-20 animate-pulse text-slate-500">
      Завантажуємо промокоди...
    </div>

    <!-- ── Таблиця промокодів ────────────────────────────────────────────── -->
    <div v-else class="overflow-x-auto rounded-2xl outline outline-1 outline-[#1e2d3d]">
      <table class="w-full text-sm text-left">
        <thead class="bg-[#131720] text-gray-400 uppercase text-xs tracking-wider">
          <tr>
            <th class="px-4 py-3.5">ID</th>
            <th class="px-4 py-3.5">Код</th>
            <th class="px-4 py-3.5">Тип</th>
            <th class="px-4 py-3.5">Значення</th>
            <th class="px-4 py-3.5">Термін дії</th>
            <th class="px-4 py-3.5">Ліміт</th>
            <th class="px-4 py-3.5">Статус</th>
            <th class="px-4 py-3.5 text-right">Дії</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[#1e2d3d]">
          <tr
            v-for="promo in promoCodes"
            :key="promo.id"
            class="bg-gray-900 hover:bg-[#151c28] transition-colors"
          >
            <td class="px-4 py-3 text-gray-300 font-mono">{{ promo.id }}</td>
            <td class="px-4 py-3 text-gray-100 font-semibold font-mono tracking-wide">{{ promo.code }}</td>
            <td class="px-4 py-3">
              <span
                v-if="promo.discount_type === 'PERCENTAGE'"
                class="px-2 py-0.5 bg-violet-500/15 text-violet-400 text-[10px] font-bold rounded-md uppercase"
              >
                Відсоток
              </span>
              <span
                v-else
                class="px-2 py-0.5 bg-sky-500/15 text-sky-400 text-[10px] font-bold rounded-md uppercase"
              >
                Фіксована
              </span>
            </td>
            <td class="px-4 py-3 text-gray-100 font-medium">
              {{ promo.discount_type === 'PERCENTAGE' ? `${promo.value}%` : `${promo.value} ₴` }}
            </td>
            <td class="px-4 py-3 text-gray-400 text-xs whitespace-nowrap">
              <div>{{ formatDate(promo.start_date) }}</div>
              <div class="text-gray-600">{{ formatDate(promo.end_date) }}</div>
            </td>
            <td class="px-4 py-3 text-gray-300">
              <span class="text-orange-400 font-semibold">{{ promo.usage_count ?? 0 }}</span>
              <span class="text-gray-600"> / {{ promo.usage_limit || '∞' }}</span>
            </td>
            <td class="px-4 py-3">
              <span
                v-if="promo.is_active"
                class="px-2.5 py-1 bg-emerald-500/15 text-emerald-400 text-[10px] font-bold rounded-md uppercase tracking-wide"
              >
                Активний
              </span>
              <span
                v-else
                class="px-2.5 py-1 bg-gray-500/15 text-gray-400 text-[10px] font-bold rounded-md uppercase tracking-wide"
              >
                Неактивний
              </span>
            </td>
            <td class="px-4 py-3 text-right">
              <button
                class="px-3 py-1.5 rounded-lg text-xs font-medium text-orange-400 bg-orange-500/10 hover:bg-orange-500/20 border border-orange-500/20 transition-colors"
                @click="openEditModal(promo)"
              >
                Редагувати
              </button>
            </td>
          </tr>
          <tr v-if="promoCodes.length === 0">
            <td colspan="8" class="px-5 py-12 text-center text-gray-500">
              Промокодів ще немає.
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
        <div class="w-full max-w-lg bg-[#131720] rounded-2xl p-6 outline outline-1 outline-[#1e2d3d] shadow-2xl max-h-[90vh] overflow-y-auto">
          <h3 class="text-gray-100 text-lg font-bold font-['Unbounded'] mb-5">
            {{ editingId !== null ? 'Редагувати промокод' : 'Новий промокод' }}
          </h3>

          <!-- Повідомлення про помилку -->
          <div
            v-if="errorMessage"
            class="mb-4 px-4 py-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm"
          >
            {{ errorMessage }}
          </div>

          <!-- Поле: Код -->
          <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">Код</label>
          <input
            v-model="form.code"
            type="text"
            placeholder="Наприклад: SUMMER2026"
            class="w-full px-4 py-2.5 mb-4 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors placeholder:text-gray-600 font-mono uppercase"
          />

          <!-- Рядок: Тип знижки + Значення -->
          <div class="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">Тип знижки</label>
              <select
                v-model="form.discount_type"
                class="w-full px-4 py-2.5 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors"
              >
                <option value="PERCENTAGE">Відсоток (%)</option>
                <option value="FIXED">Фіксована (₴)</option>
              </select>
            </div>
            <div>
              <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">Значення</label>
              <input
                v-model.number="form.value"
                type="number"
                step="0.01"
                min="0"
                placeholder="10"
                class="w-full px-4 py-2.5 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors placeholder:text-gray-600"
              />
            </div>
          </div>

          <!-- Рядок: Дати -->
          <div class="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">Початок</label>
              <input
                v-model="form.start_date"
                type="datetime-local"
                class="w-full px-4 py-2.5 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors [color-scheme:dark]"
              />
            </div>
            <div>
              <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">Кінець</label>
              <input
                v-model="form.end_date"
                type="datetime-local"
                class="w-full px-4 py-2.5 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors [color-scheme:dark]"
              />
            </div>
          </div>

          <!-- Поле: Ліміт використань -->
          <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">Ліміт використань</label>
          <input
            v-model.number="form.usage_limit"
            type="number"
            min="0"
            placeholder="0 = без обмежень"
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
              Активний
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
              :disabled="!form.code.trim() || isSaving"
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
