<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useCategoryStore } from '../state/categoryStore'

// ─── Ініціалізація store ──────────────────────────────────────────────────────
const categoryStore = useCategoryStore()

// ─── Стан модалки та форми створення ──────────────────────────────────────────
const isModalOpen = ref(false)
const form = reactive<{ name: string; parentId: number | null }>({
  name: '',
  parentId: null,
})

// ─── Завантаження категорій при монтуванні ────────────────────────────────────
onMounted(() => {
  categoryStore.fetchFlatCategories()
})

// ─── Створення нової категорії ────────────────────────────────────────────────
async function handleCreate() {
  try {
    await categoryStore.createCategory({
      name: form.name,
      parentId: form.parentId,
    })
    isModalOpen.value = false
    form.name = ''
    form.parentId = null
    await categoryStore.fetchFlatCategories()
  } catch (e) {
    console.error('Помилка створення категорії:', e)
  }
}

// ─── Видалення категорії ──────────────────────────────────────────────────────
async function handleDelete(id: number) {
  if (!window.confirm('Ви впевнені, що хочете видалити цю категорію?')) return
  try {
    await categoryStore.deleteCategory(id)
    await categoryStore.fetchFlatCategories()
  } catch (e) {
    alert('Помилка видалення. Можливо, категорія містить товари або підкатегорії.')
  }
}
</script>

<template>
  <div>
    <!-- ── Заголовок + кнопка «Додати» ──────────────────────────────────── -->
    <div class="flex items-center justify-between mb-6">
      <span class="text-gray-100 text-xl font-black font-['Unbounded']">
        Категорії маркетплейсу
      </span>
      <button
        class="px-5 py-2.5 bg-orange-500 rounded-xl text-white text-sm font-semibold hover:bg-orange-400 transition-colors"
        @click="isModalOpen = true"
      >
        Додати категорію
      </button>
    </div>

    <!-- ── Loader ───────────────────────────────────────────────────────── -->
    <div v-if="categoryStore.isLoading" class="flex justify-center py-20 animate-pulse text-slate-500">
      Завантажуємо категорії...
    </div>

    <!-- ── Таблиця категорій ────────────────────────────────────────────── -->
    <div v-else class="overflow-x-auto rounded-2xl outline outline-1 outline-[#1e2d3d]">
      <table class="w-full text-sm text-left">
        <thead class="bg-[#131720] text-gray-400 uppercase text-xs tracking-wider">
          <tr>
            <th class="px-5 py-3.5">ID</th>
            <th class="px-5 py-3.5">Назва</th>
            <th class="px-5 py-3.5">Parent ID</th>
            <th class="px-5 py-3.5 text-right">Дії</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[#1e2d3d]">
          <tr
            v-for="cat in categoryStore.flatCategories"
            :key="cat.id"
            class="bg-gray-900 hover:bg-[#151c28] transition-colors"
          >
            <td class="px-5 py-3 text-gray-300 font-mono">{{ cat.id }}</td>
            <td class="px-5 py-3 text-gray-100 font-medium">{{ cat.name }}</td>
            <td class="px-5 py-3 text-gray-400">{{ cat.parentId ?? '—' }}</td>
            <td class="px-5 py-3 text-right">
              <button
                class="px-3 py-1.5 rounded-lg text-xs font-medium text-red-400 bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 transition-colors"
                @click="handleDelete(cat.id)"
              >
                Видалити
              </button>
            </td>
          </tr>
          <tr v-if="categoryStore.flatCategories.length === 0">
            <td colspan="4" class="px-5 py-12 text-center text-gray-500">
              Категорій ще немає.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ── Модалка створення категорії ──────────────────────────────────── -->
    <Teleport to="body">
      <div
        v-if="isModalOpen"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
        @click.self="isModalOpen = false"
      >
        <div class="w-full max-w-md bg-[#131720] rounded-2xl p-6 outline outline-1 outline-[#1e2d3d] shadow-2xl">
          <h3 class="text-gray-100 text-lg font-bold font-['Unbounded'] mb-5">
            Нова категорія
          </h3>

          <!-- Поле: Назва -->
          <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">Назва</label>
          <input
            v-model="form.name"
            type="text"
            placeholder="Введіть назву категорії"
            class="w-full px-4 py-2.5 mb-4 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors placeholder:text-gray-600"
          />

          <!-- Поле: Батьківська категорія -->
          <label class="block mb-1.5 text-gray-400 text-xs font-semibold uppercase tracking-wider">Батьківська категорія</label>
          <select
            v-model="form.parentId"
            class="w-full px-4 py-2.5 mb-6 bg-[#0f1117] rounded-xl text-gray-100 text-sm outline outline-1 outline-[#1e2d3d] focus:outline-orange-500 transition-colors"
          >
            <option :value="null">Без батьківської (корінь)</option>
            <option v-for="cat in categoryStore.flatCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }} (ID: {{ cat.id }})
            </option>
          </select>

          <!-- Кнопки -->
          <div class="flex justify-end gap-3">
            <button
              class="px-5 py-2.5 rounded-xl text-sm text-gray-400 hover:text-white border border-[#1e2d3d] hover:bg-white/5 transition-colors"
              @click="isModalOpen = false"
            >
              Скасувати
            </button>
            <button
              class="px-5 py-2.5 rounded-xl text-sm text-white font-semibold bg-orange-500 hover:bg-orange-400 disabled:opacity-40 transition-colors"
              :disabled="!form.name.trim()"
              @click="handleCreate"
            >
              Зберегти
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
