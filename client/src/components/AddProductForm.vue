<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

// —— Оголошення подій форми ——
// 'close' — єдина зовнішня подія: сигналізує батьківському CabinetPage,
// що форму потрібно закрити (незалежно від причини: збереження чи скасування).
// Батько отримує цю подію через @close і повертає isAddingProduct = false.
const emit = defineEmits<{
  (e: 'close'): void
}>()

// ─── Типи ────────────────────────────────────────────────────────────────────

interface Characteristic {
  name: string
  value: string
}

interface PhotoItem {
  id: number
  file: File
  url: string       // blob URL для попереднього перегляду
  isCover: boolean
}

// ─── 1. Реактивний стан форми ─────────────────────────────────────────────────

const productForm = reactive({
  name:        '',  // Назва товару
  category:    '',  // Категорія
  price:       '',  // Вартість
  sku:         '',  // Артикул
  stock:       0,   // Кількість на складі
  description: '',  // Детальний опис
})

// Лічильник символів опису
const descriptionLength = computed(() => productForm.description.length)

// Категорії для <select>
const categoryOptions = [
  'Смартфони', 'Ноутбуки', 'Ігрові консолі',
  'Аудіотехніка', 'Фото та відео', 'Взуття',
  'Одяг', 'Дім і сад', 'Краса та догляд',
  'Інструменти', 'Авто товари', 'Зоотовари', 'Книги',
]

// ─── 2. Логіка фотографій ─────────────────────────────────────────────────────

const photos = ref<PhotoItem[]>([])
let photoIdCounter = 1

// Додавання файлів через <input type="file">
function handleFileInput(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files) return
  addFiles(Array.from(input.files))
  input.value = '' // скидаємо input, щоб можна було завантажити той самий файл знову
}

// Додавання файлів через Drag & Drop
function handleDrop(event: DragEvent) {
  isDragging.value = false
  const files = Array.from(event.dataTransfer?.files ?? [])
  addFiles(files.filter(f => f.type.startsWith('image/')))
}

function addFiles(files: File[]) {
  files.forEach(file => {
    // Перший доданий файл автоматично стає обкладинкою
    photos.value.push({
      id:      photoIdCounter++,
      file,
      url:     URL.createObjectURL(file),
      isCover: photos.value.length === 0,
    })
  })
}

// Видалення фото по індексу; якщо видаляємо обкладинку — перша фото стає нею
function removePhoto(index: number) {
  const wascover = photos.value[index].isCover
  URL.revokeObjectURL(photos.value[index].url) // звільняємо пам'ять
  photos.value.splice(index, 1)
  if (wascover && photos.value.length > 0) {
    photos.value[0].isCover = true
  }
}

// Стан drag-over для виділення зони
const isDragging = ref(false)

// ─── 3. Динамічні характеристики ─────────────────────────────────────────────

const characteristics = ref<Characteristic[]>([
  { name: 'Бренд',   value: 'Apple'          },
  { name: 'Модель',  value: 'iPhone 15 Pro Max' },
  { name: 'Колір',   value: 'Natural Titanium' },
  { name: "Пам'ять", value: '256 ГБ'          },
])

// Додати порожній рядок характеристики
function addCharacteristic() {
  characteristics.value.push({ name: '', value: '' })
}

// Видалити характеристику за індексом
function removeCharacteristic(index: number) {
  characteristics.value.splice(index, 1)
}

// ─── 4. Збереження форми ─────────────────────────────────────────────────────

function handleSave() {
  const data = {
    ...productForm,
    photos:          photos.value.map(p => ({ name: p.file.name, size: p.file.size, isCover: p.isCover })),
    characteristics: characteristics.value.filter(c => c.name.trim() !== ''),
  }
  console.log('Дані на відправку:', data)
  // TODO: тут замінити на реальний API-виклик

  // Повідомляємо батьківський компонент про успішне збереження —
  // CabinetPage закриє форму і поверне список товарів.
  emit('close')
}

// Скасувати — скидаємо форму і повертаємось до списку товарів
function handleCancel() {
  productForm.name        = ''
  productForm.category    = ''
  productForm.price       = ''
  productForm.sku         = ''
  productForm.stock       = 0
  productForm.description = ''
  photos.value.forEach(p => URL.revokeObjectURL(p.url))
  photos.value = []
  characteristics.value = [{ name: '', value: '' }]

  // Повідомляємо батьківський компонент про закриття —
  // CabinetPage поверне isAddingProduct у false.
  emit('close')
}
</script>

<template>

  <!-- ── Хлібні крихти ──────────────────────────────────────────────────────── -->
  <div class="w-full max-w-[896px] inline-flex justify-start items-center gap-2">
    <div class="inline-flex flex-col justify-start items-start">
      <div class="justify-center text-gray-400 text-xs font-normal font-['Onest'] leading-4">Панель продавця</div>
    </div>
    <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
      <path d="M4 2L8 6L4 10" stroke="#6B7280" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
    <div class="inline-flex flex-col justify-start items-start">
      <div class="justify-center text-gray-400 text-xs font-normal font-['Onest'] leading-4">Товари</div>
    </div>
    <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
      <path d="M4 2L8 6L4 10" stroke="#6B7280" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
    <div class="inline-flex flex-col justify-start items-start">
      <div class="justify-center text-orange-400 text-xs font-medium font-['Onest'] leading-4">Новий товар</div>
    </div>
  </div>

  <!-- ── Головна картка форми ───────────────────────────────────────────────── -->
  <div class="w-full max-w-[896px] bg-gray-900 rounded-2xl shadow-[0px_25px_50px_-12px_rgba(0,0,0,0.50)] outline outline-1 outline-offset-[-1px] outline-gray-800 inline-flex flex-col justify-start items-start overflow-hidden">

    <!-- Шапка з кнопками -->
    <div class="self-stretch px-8 py-6 bg-neutral-900 border-b border-gray-800 inline-flex justify-between items-center">
      <div class="inline-flex flex-col justify-start items-start gap-0.5">
        <div class="justify-center text-white text-lg font-bold font-['Onest'] leading-7">Створити новий товар</div>
        <div class="justify-center text-gray-500 text-xs font-normal font-['Onest'] leading-4">Заповніть усі поля та завантажте фотографії</div>
      </div>
      <div class="flex justify-start items-center gap-2.5">
        <!-- Кнопка Скасувати -->
        <button
          class="w-32 h-11 flex items-center justify-center gap-2 bg-gray-800 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 transition-all hover:bg-gray-700 active:scale-95"
          @click="handleCancel"
        >
          <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
            <path d="M1 1L12 12M12 1L1 12" stroke="#9CA3AF" stroke-width="1.6" stroke-linecap="round"/>
          </svg>
          <span class="text-gray-400 text-xs font-medium font-['Onest'] leading-5">Скасувати</span>
        </button>
        <!-- Кнопка Зберегти -->
        <button
          class="px-6 py-2.5 bg-orange-500 rounded-xl shadow-[0px_4px_14px_0px_rgba(249,115,22,0.30)] flex justify-start items-center gap-2 transition-all hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-95"
          @click="handleSave"
        >
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M2 7.5L5.5 11L12 4" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-white text-xs font-semibold font-['Onest'] leading-5">Зберегти товар</span>
        </button>
      </div>
    </div>

    <!-- Тіло форми -->
    <div class="self-stretch p-8 flex flex-col justify-start items-start gap-9">

      <!-- ── Секція 1: Основна інформація ─────────────────────────────────── -->
      <div class="self-stretch flex flex-col justify-start items-start gap-5">
        <!-- Заголовок секції -->
        <div class="self-stretch inline-flex justify-start items-center gap-2.5">
          <div class="w-6 h-6 bg-orange-500/20 rounded-xl flex justify-center items-center">
            <span class="text-orange-500 text-xs font-bold font-['Onest'] leading-4">1</span>
          </div>
          <span class="text-gray-100 text-base font-semibold font-['Onest'] leading-6">Основна інформація</span>
          <div class="flex-1 h-px bg-gray-800"></div>
        </div>

        <div class="self-stretch flex flex-col gap-4">

          <!-- Назва товару -->
          <div class="flex flex-col gap-2">
            <label class="text-gray-500 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">
              Назва товару <span class="text-orange-500">*</span>
            </label>
            <input
              v-model="productForm.name"
              type="text"
              placeholder="Наприклад: iPhone 15 Pro Max 256GB Natural Titanium"
              class="self-stretch px-4 py-3.5 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-800 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500/40 focus:ring-1 focus:ring-orange-500/30 focus:outline-none"
            />
          </div>

          <!-- Категорія -->
          <div class="flex flex-col gap-2">
            <label class="text-gray-500 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">
              Категорія <span class="text-orange-500">*</span>
            </label>
            <div class="self-stretch relative">
              <select
                v-model="productForm.category"
                class="w-full appearance-none pl-4 pr-10 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-800 text-gray-200 text-sm font-normal font-['Onest'] leading-5 transition-all focus:outline-orange-500/40 focus:outline-none cursor-pointer"
              >
                <option value="" disabled>Оберіть категорію…</option>
                <option v-for="cat in categoryOptions" :key="cat" :value="cat">{{ cat }}</option>
              </select>
              <!-- Стрілка вниз -->
              <svg class="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M3 5L7 9L11 5" stroke="#6B7280" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
          </div>

          <!-- Вартість -->
          <div class="flex flex-col gap-2">
            <label class="text-gray-500 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">
              Вартість <span class="text-orange-500">*</span>
            </label>
            <div class="self-stretch relative">
              <input
                v-model="productForm.price"
                type="number"
                min="0"
                placeholder="0.00"
                class="w-full h-12 pl-14 pr-4 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-800 text-gray-200 text-sm font-normal font-['Onest'] transition-all focus:outline-orange-500/40 focus:outline-none placeholder-gray-600"
              />
              <!-- Префікс ₴ -->
              <div class="w-11 h-12 absolute left-0 top-0 bg-gray-800 rounded-tl-xl rounded-bl-xl border-r border-gray-800 flex justify-center items-center pointer-events-none">
                <span class="text-gray-400 text-sm font-semibold font-['Onest'] leading-5">₴</span>
              </div>
            </div>
          </div>

          <!-- Артикул SKU -->
          <div class="flex flex-col gap-2">
            <label class="text-gray-500 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">
              Артикул (SKU)
            </label>
            <input
              v-model="productForm.sku"
              type="text"
              placeholder="Наприклад: APPL-IP15PM-256-NT"
              class="self-stretch px-4 py-3.5 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-800 text-gray-200 text-sm font-normal font-['Onest'] placeholder-gray-600 transition-all focus:outline-orange-500/40 focus:ring-1 focus:ring-orange-500/30 focus:outline-none"
            />
          </div>

          <!-- Кількість на складі -->
          <div class="flex flex-col gap-2">
            <label class="text-gray-500 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">
              Кількість на складі <span class="text-orange-500">*</span>
            </label>
            <input
              v-model.number="productForm.stock"
              type="number"
              min="0"
              placeholder="0"
              class="self-stretch h-12 px-4 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-800 text-gray-200 text-sm font-normal font-['Onest'] transition-all focus:outline-orange-500/40 focus:outline-none"
            />
          </div>
        </div>
      </div>

      <!-- ── Секція 2: Фотографії товару ──────────────────────────────────── -->
      <div class="self-stretch flex flex-col justify-start items-start gap-3">
        <!-- Заголовок секції -->
        <div class="self-stretch inline-flex justify-start items-center gap-2.5">
          <div class="w-6 h-6 bg-orange-500/20 rounded-xl flex justify-center items-center">
            <span class="text-orange-500 text-xs font-bold font-['Onest'] leading-4">2</span>
          </div>
          <span class="text-gray-100 text-base font-semibold font-['Onest'] leading-6">Фотографії товару</span>
          <div class="flex-1 h-px bg-gray-800"></div>
        </div>

        <!-- Зона Drag & Drop -->
        <div
          class="self-stretch px-10 pt-12 pb-10 bg-neutral-900 rounded-xl outline outline-2 outline-offset-[-2px] flex flex-col justify-center items-center gap-3 transition-all duration-150 cursor-pointer"
          :class="isDragging ? 'outline-orange-500/60 bg-orange-500/5' : 'outline-gray-700'"
          @dragover.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @drop.prevent="handleDrop"
          @click="($refs.fileInput as HTMLInputElement).click()"
        >
          <!-- Прихований input для завантаження файлів -->
          <input
            ref="fileInput"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            multiple
            class="hidden"
            @change="handleFileInput"
          />

          <!-- Іконка завантаження -->
          <div class="w-14 h-14 bg-gray-800 rounded-2xl outline outline-1 outline-offset-[-1px] outline-gray-700 flex justify-center items-center">
            <svg width="26" height="26" viewBox="0 0 26 26" fill="none">
              <path d="M13 16V9M13 9L10 12M13 9L16 12" stroke="#F97316" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M22.0004 17.5003C22.0004 16.3068 21.5263 15.1622 20.6824 14.3183C19.8385 13.4744 18.6939 13.0003 17.5004 13.0003H16.0004C16.1576 11.8966 16.0488 10.7712 15.6832 9.71802C15.3176 8.6648 14.7057 7.71415 13.8984 6.94519C13.0912 6.17623 12.1119 5.61121 11.0422 5.29715C9.97247 4.98309 8.8432 4.92909 7.74838 5.13963C6.65357 5.35017 5.62488 5.81917 4.74793 6.50758C3.87099 7.196 3.17116 8.08392 2.7067 9.09744C2.24224 10.111 2.02658 11.2208 2.07767 12.3345C2.12876 13.4482 2.44513 14.5336 3.00043 15.5003" stroke="#6B7280" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M8 20H18" stroke="#6B7280" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>

          <div class="flex flex-col items-start gap-1.5">
            <div class="self-stretch inline-flex justify-center items-center gap-1">
              <span class="text-gray-300 text-sm font-semibold font-['Onest'] leading-5">Перетягніть сюди фото або</span>
              <span class="text-orange-400 text-sm font-semibold font-['Onest'] leading-5">натисніть для завантаження</span>
            </div>
            <div class="self-stretch text-center text-gray-500 text-xs font-normal font-['Onest'] leading-4">
              PNG, JPG, WEBP — до 10 МБ. Рекомендований розмір 800×800 px
            </div>
          </div>

          <!-- Роздільник "або" -->
          <div class="pt-1 flex items-center gap-2">
            <div class="w-12 h-px bg-[#2D3748]"></div>
            <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">або</span>
            <div class="w-12 h-px bg-[#2D3748]"></div>
          </div>

          <button
            class="px-5 py-2.5 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 flex items-center gap-1.5 hover:bg-white/5 transition-all"
            @click.stop="($refs.fileInput as HTMLInputElement).click()"
          >
            <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
              <path d="M6.5 1V12M1 6.5H12" stroke="#9CA3AF" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
            <span class="text-gray-400 text-xs font-medium font-['Onest'] leading-4">Обрати файли</span>
          </button>
        </div>

        <!-- Сітка попереднього перегляду завантажених фото -->
        <div v-if="photos.length > 0" class="self-stretch pt-2 grid grid-cols-4 gap-2">
          <div
            v-for="(photo, index) in photos"
            :key="photo.id"
            class="relative bg-neutral-900 rounded-xl overflow-hidden group/photo aspect-square"
            :class="photo.isCover
              ? 'outline outline-2 outline-offset-[-2px] outline-orange-500/60'
              : 'outline outline-1 outline-offset-[-1px] outline-gray-800'"
          >
            <!-- Прев'ю зображення -->
            <img
              :src="photo.url"
              :alt="`Фото ${index + 1}`"
              class="w-full h-full object-cover"
            />

            <!-- Оверлей при наведенні — кнопка видалення -->
            <div
              class="absolute inset-0 bg-black/50 opacity-0 group-hover/photo:opacity-100 transition-opacity duration-150 flex justify-center items-center"
            >
              <button
                class="w-7 h-7 bg-red-500 rounded-full shadow-lg flex justify-center items-center hover:bg-red-400 active:scale-90 transition-all"
                @click.stop="removePhoto(index)"
              >
                <svg class="w-3 h-3 text-white" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <path d="M1 1L11 11M11 1L1 11"/>
                </svg>
              </button>
            </div>

            <!-- Бейдж обкладинки (тільки для першого фото) -->
            <div
              v-if="photo.isCover"
              class="absolute bottom-0 left-0 right-0 pt-2.5 pb-1.5 bg-orange-500/90 flex justify-center"
            >
              <span class="text-white text-[10px] font-semibold font-['Onest'] leading-4 tracking-tight">Обкладинка</span>
            </div>
          </div>
        </div>

        <!-- Підказка під сіткою фото -->
        <div class="self-stretch">
          <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">
            Перша фотографія — обкладинка товару. Перетягніть, щоб змінити порядок.
          </span>
        </div>
      </div>

      <!-- ── Секція 3: Опис товару ─────────────────────────────────────────── -->
      <div class="self-stretch flex flex-col justify-start items-start gap-5">
        <!-- Заголовок секції -->
        <div class="self-stretch inline-flex justify-start items-center gap-2.5">
          <div class="w-6 h-6 bg-orange-500/20 rounded-xl flex justify-center items-center">
            <span class="text-orange-500 text-xs font-bold font-['Onest'] leading-4">3</span>
          </div>
          <span class="text-gray-100 text-base font-semibold font-['Onest'] leading-6">Опис товару</span>
          <div class="flex-1 h-px bg-gray-800"></div>
        </div>

        <div class="self-stretch flex flex-col gap-1.5">
          <label class="text-gray-500 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">
            Детальний опис
          </label>
          <textarea
            v-model="productForm.description"
            rows="5"
            maxlength="2000"
            placeholder="Детально опишіть товар: матеріал, розміри, особливості використання, комплектацію тощо. Хороший опис підвищує конверсію."
            class="self-stretch px-4 pt-3 pb-6 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-800 text-gray-200 text-sm font-normal font-['Onest'] leading-6 placeholder-gray-600 resize-none transition-all focus:outline-orange-500/40 focus:outline-none"
          />
          <!-- Лічильник символів + підказка -->
          <div class="self-stretch pt-1 inline-flex justify-between items-center">
            <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">
              Мінімум 50 символів. Рекомендовано 150–500.
            </span>
            <span class="text-xs font-['Onest'] leading-4">
              <span :class="descriptionLength < 50 ? 'text-orange-400' : 'text-green-400'" class="font-medium">
                {{ descriptionLength }}
              </span>
              <span class="text-gray-500"> / 2000</span>
            </span>
          </div>
        </div>
      </div>

      <!-- ── Секція 4: Характеристики ─────────────────────────────────────── -->
      <div class="self-stretch flex flex-col justify-start items-start gap-4">
        <!-- Заголовок секції -->
        <div class="self-stretch inline-flex justify-start items-center gap-2.5">
          <div class="w-6 h-6 bg-orange-500/20 rounded-xl flex justify-center items-center">
            <span class="text-orange-500 text-xs font-bold font-['Onest'] leading-4">4</span>
          </div>
          <span class="text-gray-100 text-base font-semibold font-['Onest'] leading-6">Характеристики</span>
          <div class="flex-1 h-px bg-gray-800"></div>
        </div>

        <div class="self-stretch pt-1 flex flex-col gap-3">
          <!-- Заголовки колонок -->
          <div class="self-stretch grid grid-cols-[1fr_1fr_36px] gap-2 px-1">
            <span class="text-gray-500 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">Назва характеристики</span>
            <span class="text-gray-500 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">Значення</span>
            <span></span>
          </div>

          <!-- Рядки характеристик — виводяться через v-for -->
          <div
            v-for="(char, index) in characteristics"
            :key="index"
            class="self-stretch grid grid-cols-[1fr_1fr_36px] gap-2 items-center"
          >
            <!-- Поле "Назва" -->
            <input
              v-model="char.name"
              type="text"
              placeholder="Наприклад: Бренд"
              class="px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-800 text-gray-200 text-sm font-normal font-['Onest'] leading-5 placeholder-gray-600 transition-all focus:outline-orange-500/40 focus:outline-none"
            />
            <!-- Поле "Значення" -->
            <input
              v-model="char.value"
              type="text"
              placeholder="Наприклад: Apple"
              class="px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-800 text-gray-200 text-sm font-normal font-['Onest'] leading-5 placeholder-gray-600 transition-all focus:outline-orange-500/40 focus:outline-none"
            />
            <!-- Кнопка видалення рядка -->
            <button
              class="w-9 h-9 bg-neutral-900 rounded-[10px] outline outline-1 outline-offset-[-1px] outline-gray-800 flex justify-center items-center transition-all hover:bg-red-500/10 hover:outline-red-500/30 active:scale-95 group/del"
              @click="removeCharacteristic(index)"
            >
              <svg class="text-gray-600 group-hover/del:text-red-400 transition-colors" width="13" height="13" viewBox="0 0 13 13" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M2 3.5H11M5 3.5V2.5C5 2.36739 5.05268 2.24021 5.14645 2.14645C5.24021 2.05268 5.36739 2 5.5 2H7.5C7.63261 2 7.75979 2.05268 7.85355 2.14645C7.94732 2.24021 8 2.36739 8 2.5V3.5M5.5 6V9.5M7.5 6V9.5M3 3.5L3.5 10.5C3.5 10.6326 3.55268 10.7598 3.64645 10.8536C3.74021 10.9473 3.86739 11 4 11H9C9.13261 11 9.25979 10.9473 9.35355 10.8536C9.44732 10.7598 9.5 10.6326 9.5 10.5L10 3.5"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Кнопка "+ Додати характеристику" -->
        <div class="self-stretch">
          <button
            class="px-4 py-2.5 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 flex items-center gap-1.5 transition-all hover:bg-white/5 hover:outline-gray-500 active:scale-95"
            @click="addCharacteristic"
          >
            <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
              <path d="M6.5 1V12M1 6.5H12" stroke="#9CA3AF" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
            <span class="text-gray-400 text-xs font-medium font-['Onest'] leading-5">Додати характеристику</span>
          </button>
        </div>
      </div>

    </div>

    <!-- ── Нижня панель з кнопками ────────────────────────────────────────── -->
    <div class="self-stretch px-8 py-5 bg-neutral-900 border-t border-gray-800 inline-flex justify-between items-center">
      <!-- Підказка про обов'язкові поля -->
      <div class="flex justify-start items-center gap-1.5">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <circle cx="7" cy="7" r="5.5" stroke="#4B5563" stroke-width="1.2"/>
          <path d="M7 4.5V8" stroke="#4B5563" stroke-width="1.2" stroke-linecap="round"/>
          <circle cx="7.00039" cy="10.0004" r="0.6" fill="#4B5563"/>
        </svg>
        <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4">
          Усі поля, позначені зірочкою *, є обов'язковими
        </span>
      </div>
      <!-- Дублікат кнопок -->
      <div class="flex items-center gap-2.5">
        <button
          class="w-32 h-11 flex items-center justify-center gap-2 bg-gray-800 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 transition-all hover:bg-gray-700 active:scale-95"
          @click="handleCancel"
        >
          <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
            <path d="M1 1L12 12M12 1L1 12" stroke="#9CA3AF" stroke-width="1.6" stroke-linecap="round"/>
          </svg>
          <span class="text-gray-400 text-xs font-medium font-['Onest'] leading-5">Скасувати</span>
        </button>
        <button
          class="px-6 py-2.5 bg-orange-500 rounded-xl shadow-[0px_4px_14px_0px_rgba(249,115,22,0.30)] flex items-center gap-2 transition-all hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-95"
          @click="handleSave"
        >
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M2 7.5L5.5 11L12 4" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-white text-xs font-semibold font-['Onest'] leading-5">Зберегти товар</span>
        </button>
      </div>
    </div>
  </div>

  <!-- ── Підвал форми ───────────────────────────────────────────────────────── -->
  <div class="w-full max-w-[896px] pt-1 flex flex-col items-center">
    <span class="text-gray-700 text-xs font-normal font-['Onest'] leading-4">
      Seller Dashboard — Панель управління товарами © {{ new Date().getFullYear() }}
    </span>
  </div>

</template>

<style scoped>
/* Прибираємо стрілки у числових інпутів */
input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type='number'] { -moz-appearance: textfield; }

/* Прибираємо дефолтний вигляд select на webkit */
select { -webkit-appearance: none; }
</style>