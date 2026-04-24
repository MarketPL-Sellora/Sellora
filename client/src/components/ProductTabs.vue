<script setup lang="ts">
import { ref, reactive } from 'vue'

// ─── Types ────────────────────────────────────────────────────────────────────

interface Tab {
  id: string
  label: string
  count?: number
}

interface Feature {
  icon: 'camera' | 'chip' | 'display' | 'pen' | 'battery' | 'ai'
  title: string
  subtitle: string
}

// —— Тип одного відгуку ——
interface Review {
  id: number        // Унікальний ідентифікатор відгуку
  author: string    // Ім'я автора
  rating: number    // Оцінка від 1 до 5
  text: string      // Текст відгуку
  date: string      // Дата у форматі YYYY-MM-DD
}

// ─── Tabs ─────────────────────────────────────────────────────────────────────

const tabs: Tab[] = [
  { id: 'desc',    label: 'ОПИС'              },
  { id: 'specs',   label: 'ХАРАКТЕРИСТИКИ'    },
  { id: 'reviews', label: 'ВІДГУКИ', count: 214 },
]

const activeTab = ref('desc')

// ─── Description paragraphs ───────────────────────────────────────────────────

const descriptionParagraphs: string[] = [
  'Samsung Galaxy S24 Ultra — це флагман нового покоління, що встановлює нові стандарти в мобільній фотографії та штучному інтелекті. Оснащений процесором Snapdragon 8 Gen 3, він забезпечує блискавичну продуктивність у будь-яких умовах.',
  'Революційна камерна система з 200 Мп основним сенсором та підтримкою 100-кратного оптичного зуму відкриває безмежні творчі можливості. Вбудований S Pen з нульовою затримкою дозволяє легко занотовувати ідеї та малювати прямо на екрані.',
  'Galaxy AI — набір інтелектуальних функцій на основі штучного інтелекту, що перетворює спілкування, творчість та продуктивність. Від Circle to Search до Live Translate — телефон розуміє вас краще, ніж будь-коли.',
]

// ─── Features ────────────────────────────────────────────────────────────────

const features: Feature[] = [
  {
    icon: 'camera',
    title: '200 Мп камера',
    subtitle: 'Quad-bayer сенсор, 100x zoom, нічна зйомка',
  },
  {
    icon: 'chip',
    title: 'Snapdragon 8 Gen 3',
    subtitle: '4 нм чіп, 12 ГБ RAM, AnTuTu 2,1 млн',
  },
  {
    icon: 'display',
    title: '6.8" Dynamic AMOLED 2X',
    subtitle: '3088×1440, 120 Гц, 2600 нт пікова яскравість',
  },
  {
    icon: 'pen',
    title: 'S Pen вбудований',
    subtitle: 'Нульова затримка, підтримка AI',
  },
  {
    icon: 'battery',
    title: 'Акумулятор 5000 мАг',
    subtitle: '45 Вт дротова, 15 Вт бездротова зарядка',
  },
  {
    icon: 'ai',
    title: 'Galaxy AI',
    subtitle: 'Circle to Search, Live Translate, Note Assist',
  },
]

// —— Масив відгуків ——
// Містить повний список відгуків поточного товару.
// Нові відгуки додаються на початок масиву (додаємо в unshift), щоб вони траплялись найсвіжішими.
const reviews = ref<Review[]>([
  {
    id:     1,                            // Унікальний ID відгуку
    author: 'Олексій Прокопенко', // Ім'я покупця
    rating: 5,                            // Оцінка: 5 з 5
    text:   'Надзвичайний товар! Камера — просто визнання. Фотографую вночі без спалаху — знімки на рівні з дорогими ззеркалками. S Pen дуже зручний для нотаток. Игри літають без проблем.', // Текст відгуку
    date:   '2024-03-15',                 // Дата публікації
  },
  {
    id:     2,                            // Унікальний ID відгуку
    author: 'Марина Савченко',   // Ім'я покупця
    rating: 4,                            // Оцінка: 4 з 5
    text:   'Чудовий телефон, помічний і швидкий. єдине, що не сподобалось — насамперед немає виходу USB-C. Батарея тримає весь день навіть при інтенсивному використанні.', // Текст відгуку
    date:   '2024-04-02',                 // Дата публікації
  },
])

// —— Об'єкт нового відгуку ——
// Зберігає поточний стан форми додавання відгуку.
// reactive() замість ref() — зручніша робота з формою z v-model.
const newReview = reactive({
  author: '', // Ім'я автора — вводиться у текстовий інпут
  rating: 5, // Поточна оцінка — за замовчуванням 5 зірок
  text:   '', // Текст відгуку — сонд textarea
})

// —— Лічильник для генерації id відгуків ——
// На кок нового відгуку збільшується, щоб id завжди були унікальними.
let nextReviewId = reviews.value.length + 1

// —— submitReview ——
// Додає новий відгук на початок масиву reviews (уншифт),
// щоб новий відгук завжди з'являвся першим у списку.
// Після додавання скидає форму, повертаючи поля до значень за замовчуванням.
// Функція ігнорує порожні поля: якщо ім'я або текст порожні — нічого не станеться.
function submitReview() {
  // Перевіряємо обов'язкові поля перед додаванням
  if (!newReview.author.trim() || !newReview.text.trim()) return

  // Додаємо новий відгук на початок списку (найсвіжіший завжди зверху)
  reviews.value.unshift({
    id:     nextReviewId++,                        // Генеруємо унікальний id
    author: newReview.author.trim(),               // Ім'я автора без зайвих символів
    rating: newReview.rating,                      // Оцінка яка є
    text:   newReview.text.trim(),                 // Текст без зайвих символів
    date:   new Date().toISOString().split('T')[0], // Точна дата сьогодні (YYYY-MM-DD)
  })

  // Скидаємо форму після додавання — повертаємо усі поля до значень за замовчуванням
  newReview.author = ''
  newReview.rating = 5
  newReview.text   = ''
}

// —— Допоміжна функція: створення рядка зірок ——
// Повертає масив з n зірок для рендерингу v-for піктограми — активних і порожніх зірок.
function starsArray(rating: number): { filled: boolean }[] {
  return Array.from({ length: 5 }, (_, i) => ({ filled: i < rating }))
}

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'tab-change', id: string): void
}>()

function setTab(id: string) {
  activeTab.value = id
  emit('tab-change', id)
}
</script>

<template>
  <div class="w-full inline-flex flex-col justify-start items-start gap-8">

    <!-- ── Tab bar ────────────────────────────────────────────────────────── -->
    <div class="self-stretch border-b border-[#1c1f2a] flex overflow-x-auto scrollbar-none whitespace-nowrap">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="px-6 py-3.5 relative inline-flex justify-center items-center gap-1 text-sm font-normal font-['Onest'] leading-5 transition-colors duration-150 focus:outline-none"
        :class="
          activeTab === tab.id
            ? 'text-white'
            : 'text-[#5a5f7a] hover:text-white'
        "
        @click="setTab(tab.id)"
      >
        {{ tab.label }}
        <span
          v-if="tab.count"
          class="transition-colors duration-150"
          :class="activeTab === tab.id ? 'text-orange-400' : 'text-orange-400/60'"
        >
          ({{ tab.count }})
        </span>

        <!-- Active underline -->
        <span
          v-if="activeTab === tab.id"
          class="absolute bottom-0 left-0 right-0 h-0.5 bg-orange-500 rounded-t"
        />
      </button>
    </div>

    <!-- ── ОПИС tab ───────────────────────────────────────────────────────── -->
    <div
      v-if="activeTab === 'desc'"
      class="self-stretch inline-flex justify-center items-start gap-8 flex-wrap"
    >
      <!-- Left: text + banners -->
      <div class="flex-1 min-w-0 self-stretch inline-flex flex-col justify-start items-start gap-6">

        <!-- Description -->
        <div class="self-stretch flex flex-col gap-3">
          <h2 class="text-white text-xl font-normal font-['Unbounded'] leading-7">
            Про Samsung Galaxy S24 Ultra
          </h2>
          <p
            v-for="(para, i) in descriptionParagraphs"
            :key="i"
            class="text-[#787d99] text-sm font-normal font-['Onest'] leading-6"
            :class="i > 0 ? 'pt-1' : ''"
          >
            {{ para }}
          </p>
        </div>

        <!-- Banner 1 — Camera -->
        <div class="self-stretch py-20 md:py-44 relative bg-[#1c1f2a] rounded-xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] flex justify-center items-center overflow-hidden">
          <div class="absolute inset-0 bg-gradient-to-br from-[#1a1040] to-[#0d1022]" />
          <!-- Replace with: <img src="../assets/banner-camera.jpg" alt="Камерна система 200 Мп" class="absolute inset-0 w-full h-full object-cover" /> -->
          <div class="relative z-10 flex flex-col items-center gap-2">
            <svg class="w-10 h-10 text-[#c8ccdf]" viewBox="0 0 40 40" fill="none" stroke="currentColor" stroke-width="1.5" xmlns="http://www.w3.org/2000/svg">
              <rect x="5" y="11" width="30" height="22" rx="3"/>
              <circle cx="20" cy="22" r="7"/>
              <circle cx="20" cy="22" r="3"/>
              <rect x="14" y="7" width="8" height="4" rx="1"/>
              <circle cx="32" cy="15" r="1.5" fill="currentColor"/>
            </svg>
            <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">
              Банер — камерна система 200 Мп
            </span>
          </div>
        </div>

        <!-- Banner 2 — AI -->
        <div class="self-stretch py-20 md:py-44 relative bg-[#1c1f2a] rounded-xl outline outline-1 outline-offset-[-1px] outline-[#2a2d3e] flex justify-center items-center overflow-hidden">
          <div class="absolute inset-0 bg-gradient-to-br from-[#0f2027] to-[#203a43]" />
          <!-- Replace with: <img src="../assets/banner-ai.jpg" alt="Galaxy AI функції" class="absolute inset-0 w-full h-full object-cover" /> -->
          <div class="relative z-10 flex flex-col items-center gap-2">
            <svg class="w-10 h-10 text-[#c8ccdf]" viewBox="0 0 40 40" fill="none" stroke="currentColor" stroke-width="1.5" xmlns="http://www.w3.org/2000/svg">
              <circle cx="20" cy="20" r="14"/>
              <path d="M20 10v4M20 26v4M10 20h4M26 20h4"/>
              <circle cx="20" cy="20" r="4"/>
              <path d="M13.4 13.4l2.8 2.8M23.8 23.8l2.8 2.8M26.6 13.4l-2.8 2.8M16.2 23.8l-2.8 2.8"/>
            </svg>
            <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">
              Банер — Galaxy AI функції
            </span>
          </div>
        </div>
      </div>

      <!-- Right: key features -->
      <div class="w-full lg:w-80 shrink-0">
        <div class="self-stretch p-5 bg-[#161820] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] flex flex-col gap-4">
          <h3 class="text-white text-base font-normal font-['Onest'] leading-6">
            Ключові особливості
          </h3>
          <div class="flex flex-col gap-3">
            <div
              v-for="feat in features"
              :key="feat.title"
              class="inline-flex justify-start items-start gap-3"
            >
              <!-- Icon -->
              <div class="w-7 h-7 shrink-0 mt-0.5 bg-orange-500/20 rounded-lg flex justify-center items-center">
                <!-- camera -->
                <svg v-if="feat.icon === 'camera'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <rect x="1" y="4" width="12" height="9" rx="1.5"/>
                  <circle cx="7" cy="8.5" r="2.5"/>
                  <rect x="4.5" y="2" width="3.5" height="2" rx="0.5"/>
                  <circle cx="11.5" cy="5.5" r="0.8" fill="currentColor"/>
                </svg>
                <!-- chip -->
                <svg v-else-if="feat.icon === 'chip'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <rect x="3.5" y="3.5" width="7" height="7" rx="1"/>
                  <path d="M5 1v2.5M7 1v2.5M9 1v2.5M5 10.5V13M7 10.5V13M9 10.5V13M1 5h2.5M1 7h2.5M1 9h2.5M10.5 5H13M10.5 7H13M10.5 9H13"/>
                </svg>
                <!-- display -->
                <svg v-else-if="feat.icon === 'display'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <rect x="1" y="2" width="12" height="8" rx="1"/>
                  <path d="M4.5 12h5M7 10v2"/>
                </svg>
                <!-- pen -->
                <svg v-else-if="feat.icon === 'pen'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <path d="M9.5 1.5l3 3-7 7H2.5v-3l7-7z" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M7.5 3.5l3 3" stroke-linecap="round"/>
                </svg>
                <!-- battery -->
                <svg v-else-if="feat.icon === 'battery'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <rect x="1" y="4" width="10" height="6" rx="1"/>
                  <path d="M11 6v2" stroke-width="1.5" stroke-linecap="round"/>
                  <path d="M13 5.5v3" stroke-linecap="round"/>
                  <path d="M3.5 7h4" stroke-linecap="round"/>
                  <path d="M5.5 5v4" stroke-linecap="round"/>
                </svg>
                <!-- ai -->
                <svg v-else-if="feat.icon === 'ai'" class="w-3.5 h-3.5 text-orange-400" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
                  <circle cx="7" cy="7" r="5.5"/>
                  <path d="M7 4v3l2 1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="7" cy="7" r="1.5" fill="currentColor" stroke="none"/>
                </svg>
              </div>

              <!-- Text -->
              <div class="flex flex-col gap-0.5 min-w-0">
                <span class="text-[#c8ccdf] text-sm font-normal font-['Onest'] leading-5">{{ feat.title }}</span>
                <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">{{ feat.subtitle }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── ХАРАКТЕРИСТИКИ tab ─────────────────────────────────────────────── -->
    <div
      v-else-if="activeTab === 'specs'"
      class="self-stretch flex flex-col gap-4"
    >
      <p class="text-[#5a5f7a] text-sm font-normal font-['Onest'] leading-6">
        <!-- Тут буде таблиця характеристик -->
        Технічні характеристики будуть тут.
      </p>
    </div>

    <!-- ── ВІДГУКИ tab ────────────────────────────────────────────────────── -->
    <div
      v-else-if="activeTab === 'reviews'"
      class="self-stretch flex flex-col gap-8"
    >

      <!-- —— Форма додавання відгуку —— -->
      <!-- submit.prevent — забороняємо перезавантаження сторінки при відправці форми -->
      <form
        class="p-6 bg-[#161820] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] flex flex-col gap-5"
        @submit.prevent="submitReview"
      >
        <!-- Заголовок форми -->
        <div class="flex flex-col gap-0.5">
          <h3 class="text-white text-base font-semibold font-['Onest'] leading-6">Залишити відгук</h3>
          <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">Поділіться своєю думкою з іншими покупцями</span>
        </div>

        <!-- Інпут: Ім'я -->
        <div class="flex flex-col gap-1.5">
          <label class="text-[#5a5f7a] text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">Ваше ім'я</label>
          <input
            v-model="newReview.author"
            type="text"
            placeholder="Наприклад: Олексій П."
            required
            class="px-4 py-3 bg-[#0f1117] rounded-xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] text-gray-200 text-sm font-normal font-['Onest'] placeholder-[#3a3f55] transition-all focus:outline-orange-500/40 focus:ring-1 focus:ring-orange-500/30 focus:outline-none"
          />
        </div>

        <!-- Вибір оцінки: 5 зірок, клік на зірку змінює newReview.rating -->
        <div class="flex flex-col gap-1.5">
          <label class="text-[#5a5f7a] text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">Оцінка</label>
          <div class="flex gap-1">
            <button
              v-for="star in 5"
              :key="star"
              type="button"
              class="text-2xl leading-none transition-transform duration-100 hover:scale-110 active:scale-95 focus:outline-none"
              :class="star <= newReview.rating ? 'text-amber-400' : 'text-[#2a2d3e]'"
              :aria-label="`Оцінка ${star} з 5`"
              @click="newReview.rating = star"
            >
              ★
            </button>
          </div>
        </div>

        <!-- Текстове поле -->
        <div class="flex flex-col gap-1.5">
          <label class="text-[#5a5f7a] text-xs font-medium font-['Onest'] uppercase leading-4 tracking-wide">Текст відгуку</label>
          <textarea
            v-model="newReview.text"
            rows="4"
            placeholder="Що вам сподобалось найбільше? Яка якість товару?"
            required
            class="px-4 py-3 bg-[#0f1117] rounded-xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] text-gray-200 text-sm font-normal font-['Onest'] leading-6 placeholder-[#3a3f55] resize-none transition-all focus:outline-orange-500/40 focus:outline-none"
          />
        </div>

        <!-- Кнопка відправки; :disabled — забороняємо відправку з порожніми полями -->
        <div class="flex justify-end">
          <button
            type="submit"
            :disabled="!newReview.author.trim() || !newReview.text.trim()"
            class="px-6 py-2.5 bg-orange-500 rounded-xl flex items-center gap-2 transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] focus:outline-none focus:ring-2 focus:ring-orange-500/50 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-orange-500 disabled:active:scale-100"
          >
            <svg class="w-3.5 h-3.5 text-white" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M2 7.5L5.5 11L12 4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="text-white text-sm font-semibold font-['Onest'] leading-5">Залишити відгук</span>
          </button>
        </div>
      </form>

      <!-- —— Заголовок списку відгуків —— -->
      <div class="flex items-center gap-3">
        <h3 class="text-white text-base font-semibold font-['Onest'] leading-6">Рецензії покупців</h3>
        <!-- Лічильник відгуків — оновлюється реактивно разом з масивом -->
        <div class="px-2 py-0.5 bg-orange-500/10 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/20">
          <span class="text-orange-400 text-xs font-medium font-['Onest'] leading-4">{{ reviews.length }}</span>
        </div>
      </div>

      <!-- —— Список відгуків ——
           v-for ітерує по масиву reviews; :key — унікальний id кожного відгуку. -->
      <div class="flex flex-col gap-4">
        <div
          v-for="review in reviews"
          :key="review.id"
          class="p-5 bg-[#161820] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] flex flex-col gap-3 transition-all duration-200 hover:outline-[#2a2d3e]"
        >
          <!-- Верхня частина: аватар + ім'я + оцінка + дата -->
          <div class="flex items-start justify-between gap-3 flex-wrap">

            <div class="flex items-center gap-3">
              <!-- Аватар — ініціали імені автора у градієнтному колі -->
              <div class="w-9 h-9 shrink-0 rounded-full bg-gradient-to-br from-orange-500/80 to-violet-600/80 flex items-center justify-center">
                <span class="text-white text-xs font-bold font-['Unbounded'] leading-4">
                  <!-- Беремо першу літеру імені як аватар -->
                  {{ review.author.charAt(0).toUpperCase() }}
                </span>
              </div>

              <div class="flex flex-col gap-0.5">
                <!-- Ім'я автора -->
                <span class="text-[#c8ccdf] text-sm font-semibold font-['Onest'] leading-5">{{ review.author }}</span>
                <!-- Зірки оцінки: заповнені — амброві, порожні — темні -->
                <div class="flex gap-0.5">
                  <span
                    v-for="(star, si) in starsArray(review.rating)"
                    :key="si"
                    class="text-sm leading-none"
                    :class="star.filled ? 'text-amber-400' : 'text-[#2a2d3e]'"
                  >★</span>
                </div>
              </div>
            </div>

            <!-- Дата відгуку -->
            <span class="text-[#3a3f55] text-xs font-normal font-['Onest'] leading-4 shrink-0">{{ review.date }}</span>
          </div>

          <!-- Текст відгуку -->
          <p class="text-[#787d99] text-sm font-normal font-['Onest'] leading-6">
            {{ review.text }}
          </p>
        </div>

        <!-- Порожній стан: немає відгуків — відображаємо повідомлення -->
        <div
          v-if="reviews.length === 0"
          class="py-12 flex flex-col items-center gap-3"
        >
          <svg class="w-10 h-10 text-[#2a2d3e]" viewBox="0 0 40 40" fill="none" stroke="currentColor" stroke-width="1.2">
            <path d="M20 4C11.163 4 4 10.268 4 18c0 4.418 2.239 8.367 5.777 11.09L8 36l7.09-2.363A18.3 18.3 0 0020 34c8.837 0 16-6.268 16-14S28.837 4 20 4z" stroke-linejoin="round"/>
            <path d="M13 18h.01M20 18h.01M27 18h.01" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span class="text-[#3a3f55] text-sm font-normal font-['Onest'] leading-5">Поки відгуків немає. Будьте першим!</span>
        </div>
      </div>

    </div>

  </div>
</template>
