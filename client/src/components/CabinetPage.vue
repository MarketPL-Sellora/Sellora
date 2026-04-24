<script setup lang="ts">
import { ref, reactive } from 'vue'
import Header           from './Header.vue'
import Footer           from './Footer.vue'
import CabinetSidebar   from './CabinetSidebar.vue'
import CabinetGroupBuys from './CabinetGroupBuys.vue'
import ProductCard       from './ProductCard.vue'
// —— Імпорт форми додавання товару ——
// Підключаємо компонент форми, яка відображається замість списку товарів
// коли продавець натискає «Додати товар».
import AddProductForm   from './AddProductForm.vue'
// ─── Імпорт сховища користувача ───────────────────────────────────────────────
import { useUserStore } from '../state/userStore'
// —— Імпорт форми створення магазину ——
// Показується, коли у користувача ще немає магазину і він натискає «Відкрити магазин».
import CreateStoreForm  from './CreateStoreForm.vue'

// —— Активна вкладка ——
// Зберігає ID поточної активної вкладки. За замовчуванням відкриваємо «Замовлення».
// Значення прокидується у CabinetSidebar через prop :activeTab,
// а зміни повертаються через emit @navigate.
const activeTab = ref('orders')

// —— Режим «Додати товар» ——
// Керує видимістю форми AddProductForm у вкладці «Мої товари».
// false → показуємо список існуючих товарів та кнопку «Додати товар».
// true  → приховуємо список і рендеримо форму AddProductForm.
const isAddingProduct = ref(false)

// —— Стан магазину ——
// hasStore — чи є у користувача створений магазин.
// false → показуємо порожній стан із кнопкою «Відкрити магазин».
// true  → показуємо панель «Мій магазин» зі списком товарів.
// TODO: замінити на реальне значення з API після інтеграції бекенду.
const hasStore = ref(false)

// —— Режим створення магазину ——
// Керує видимістю форми CreateStoreForm.
// true → відображаємо форму створення замість порожнього стану.
const isCreatingStore = ref(false)

// ─── Крок 4 (US 4.2): Захист ролі ────────────────────────────────────────────
// Тимчасова змінна для тестування видимості вкладки «Мої товари».
// true  → користувач є продавцем, вкладка «Мої товари» відображається в меню.
// false → користувач — покупець, вкладка прихована.
// TODO: замінити на реальне значення з auth store / API після інтеграції бекенду.
const isUserSeller = ref(true)

// ─── Сховище користувача (email для відображення) ────────────────────────────
const userStore = useUserStore()

// ─── Крок 3: Mock-дані для «Мої замовлення» ──────────────────────────────────
// Масив тестових товарів. Кожен об'єкт відповідає інтерфейсу Product у ProductCard.vue.
// Поля: id, brand, name, image, imageAlt, rating, reviewCount,
//        groupLabel, groupCurrent, groupTotal, price, oldPrice.
const mockOrders = ref([
  {
    id:           101,
    brand:        'Apple',
    name:         'MacBook Air M3 13"',
    image:        'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/mba13-midnight-select-202402?wid=904&hei=840&fmt=jpeg&qlt=90',
    imageAlt:     'MacBook Air M3',
    rating:       5,
    reviewCount:  214,
    groupLabel:   'Учасників',
    groupCurrent: 3,
    groupTotal:   3,
    price:        52999,
    oldPrice:     59999,
  },
  {
    id:           102,
    brand:        'Sony',
    name:         'WH-1000XM5 Headphones',
    image:        'https://www.bhphotovideo.com/images/images2500x2500/sony_wh1000xm5_b_wh_1000xm5_wireless_noise_canceling_over_ear_1668478.jpg',
    imageAlt:     'Sony WH-1000XM5',
    rating:       4,
    reviewCount:  98,
    groupLabel:   'Учасників',
    groupCurrent: 2,
    groupTotal:   4,
    price:        9499,
    oldPrice:     11999,
  },
  {
    id:           103,
    brand:        'Samsung',
    name:         'Galaxy Tab S9 FE 10.9"',
    image:        'https://images.samsung.com/ua/smartphones/galaxy-tab-s9-fe/buy/01_S9FE_kv_D.jpg',
    imageAlt:     'Samsung Galaxy Tab S9 FE',
    rating:       4,
    reviewCount:  57,
    groupLabel:   'Учасників',
    groupCurrent: 1,
    groupTotal:   3,
    price:        14999,
    oldPrice:     17999,
  },
])

// ─── Крок 3: Mock-дані для «Обране» ─────────────────────────────────────────
// Окремий масив для вкладки «Обране» — незалежний від замовлень.
const mockFavorites = ref([
  {
    id:           201,
    brand:        'Logitech',
    name:         'MX Master 3S Wireless Mouse',
    image:        'https://resource.logitech.com/w_386,c_limit,q_auto,f_auto,dpr_1.0/d_transparent.gif/content/dam/logitech/en/products/mice/mx-master-3s/gallery/mx-master-3s-mouse-top-view-graphite.png',
    imageAlt:     'Logitech MX Master 3S',
    rating:       5,
    reviewCount:  341,
    groupLabel:   'Учасників',
    groupCurrent: 2,
    groupTotal:   3,
    price:        3299,
    oldPrice:     4199,
  },
  {
    id:           202,
    brand:        'Keychron',
    name:         'K8 Pro Wireless Keyboard',
    image:        'https://www.keychron.com/cdn/shop/products/Keychron-K8-Pro-QMK-VIA-wireless-mechanical-keyboard-for-Mac-with-hot-swappable-Gateron-G-Pro-switch-carbon-fiber-plate_1200x1200.jpg',
    imageAlt:     'Keychron K8 Pro',
    rating:       5,
    reviewCount:  189,
    groupLabel:   'Учасників',
    groupCurrent: 1,
    groupTotal:   5,
    price:        4799,
    oldPrice:     5999,
  },
  {
    id:           203,
    brand:        'LG',
    name:         'UltraWide 34WN80C-B 34"',
    image:        'https://www.lg.com/ua/images/monitors/md05318374/gallery/1-0.jpg',
    imageAlt:     'LG UltraWide Monitor',
    rating:       4,
    reviewCount:  76,
    groupLabel:   'Учасників',
    groupCurrent: 3,
    groupTotal:   4,
    price:        18499,
    oldPrice:     22999,
  },
])

// ─── Масив товарів магазину продавця (вкладка «Мої товари») ──────────────────
// Починаємо з порожнього масиву. Товари додаються через AddProductForm (unshift).
// TODO: замінити на дані з API після інтеграції бекенду.
const myStoreProducts = ref<Record<string, unknown>[]>([])

// —— Об'єкт з даними магазину ——
// Зберігає інформацію, яку продавець заповнив при створенні магазину.
// Заповнюється після успішного сабміту CreateStoreForm.
const myStore = reactive({
  name: '',
  category: '',
  description: '',
  city: '',
  phone: '',
  website: '',
  instagram: '',
  logo: null as string | null, // blob URL логотипу або null
})

// —— Обробник збереження нового товару ——
// Викликається з AddProductForm через emit('save', product).
// Додає товар на початок масиву (unshift) та закриває форму.
function handleProductSave(product: Record<string, unknown>) {
  myStoreProducts.value.unshift(product)
  isAddingProduct.value = false
}

// —— Обробник успішного створення магазину ——
// formData тепер відповідає backend-схемі:
//   name, address, contactPhone, description, logoUrl — з сервера
//   logoFile — File об'єкт (якщо був завантажений)
function handleStoreCreated(formData: Record<string, unknown>) {
  console.log('Магазин створено:', formData)

  // `address` від бекенду → відображаємо як `city` у профілі
  myStore.name        = (formData.name         as string) || ''
  myStore.category    = ''                                   // not in schema, clear it
  myStore.description = (formData.description  as string) || ''
  myStore.city        = (formData.address       as string) || ''
  myStore.phone       = (formData.contactPhone  as string) || ''
  myStore.website     = (formData.website       as string) || ''
  myStore.instagram   = (formData.instagram     as string) || ''

  // Логотип: якщо сервер повернув URL — використовуємо його;
  // інакше — blob URL із локального файлу (для миттєвого прев'ю)
  if (formData.logoUrl && typeof formData.logoUrl === 'string' && formData.logoUrl !== '') {
    myStore.logo = formData.logoUrl
  } else if (formData.logoFile instanceof File) {
    myStore.logo = URL.createObjectURL(formData.logoFile)
  }

  hasStore.value       = true
  isCreatingStore.value = false
}

// —— Заглушка для кнопки «Редагувати профіль» ——
// TODO: реалізувати модалку або перехід на сторінку редагування магазину.
function editStore() {
  console.log('Редагування магазину (заглушка)')
}
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">

    <Header :showCategories="false" />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-6 py-12 flex gap-8 items-start">

      <!-- ── Ліва колонка: Сайдбар ────────────────────────────────────────────── -->
      <!-- sticky top-6 — сайдбар «прилипає» до верху при скролі правої частини -->
      <div class="w-1/4 sticky top-6">
        <!--
          Передаємо activeTab і isUserSeller у CabinetSidebar через props.
          Слухаємо emit @navigate — отримуємо новий id вкладки і оновлюємо activeTab.
        -->
        <CabinetSidebar
          :activeTab="activeTab"
          :isUserSeller="isUserSeller"
          @navigate="activeTab = $event"
        />
      </div>

      <!-- ── Права колонка: Динамічний контент ────────────────────────────────── -->
      <!-- Крок 2: v-if перемикає контент залежно від значення activeTab -->
      <div class="w-3/4 flex flex-col gap-6">

        <!-- ── Вкладка: Мої замовлення (orders) ─────────────────────────────── -->
        <!-- Крок 3: Виводимо mockOrders через ProductCard у сітці 3 колонки -->
        <div v-if="activeTab === 'orders'">

          <!-- Заголовок вкладки -->
          <div class="mb-6 flex flex-col gap-1">
            <div class="inline-flex gap-2 flex-wrap">
              <span class="text-gray-100 text-2xl font-black font-['Unbounded'] leading-8">Мої замовлення</span>
              <span class="text-gray-600 text-2xl font-normal font-['Unbounded'] leading-8">/ {{ mockOrders.length }} товари</span>
            </div>
            <span class="text-gray-600 text-sm font-normal font-['Onest'] leading-5">
              Ваші активні та завершені замовлення через групові покупки
            </span>
          </div>

          <!-- Крок 3: Сітка карток замовлень -->
          <!-- simple=true — вимикає прогрес-бар та кнопку «Відкрити збір» у ProductCard -->
          <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
            <ProductCard
              v-for="item in mockOrders"
              :key="item.id"
              :product="item"
              :simple="true"
            />
          </div>
        </div>

        <!-- ── Вкладка: Мої групові покупки (groups) ─────────────────────────── -->
        <!-- Використовуємо вже готовий компонент CabinetGroupBuys -->
        <CabinetGroupBuys v-else-if="activeTab === 'groups'" />

        <!-- ── Вкладка: Обране (wishlist) ────────────────────────────────────── -->
        <!-- Крок 3: Виводимо mockFavorites через ProductCard -->
        <div v-else-if="activeTab === 'wishlist'">

          <!-- Заголовок вкладки -->
          <div class="mb-6 flex flex-col gap-1">
            <div class="inline-flex gap-2 flex-wrap">
              <span class="text-gray-100 text-2xl font-black font-['Unbounded'] leading-8">Обране</span>
              <span class="text-gray-600 text-2xl font-normal font-['Unbounded'] leading-8">/ {{ mockFavorites.length }} товари</span>
            </div>
            <span class="text-gray-600 text-sm font-normal font-['Onest'] leading-5">
              Товари, які ви зберегли для майбутньої покупки
            </span>
          </div>

          <!-- Крок 3: Сітка карток обраного -->
          <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
            <ProductCard
              v-for="item in mockFavorites"
              :key="item.id"
              :product="item"
              :simple="true"
            />
          </div>
        </div>

        <!-- ── Вкладка: Мої товари (my-products) ─────────────────────────────── -->
        <!-- US 4.2: рендериться лише для продавця; v-else-if захищає від підробки вкладки -->
        <div v-else-if="activeTab === 'my-products' && isUserSeller">

          <!-- ════════════════════════════════════════════════════════════════ -->
          <!-- Стан 1: Магазину ще немає — порожній стан із пропозицією створити -->
          <!-- ════════════════════════════════════════════════════════════════ -->
          <div v-if="!hasStore && !isCreatingStore" class="flex flex-col items-center justify-center py-20 gap-6">

            <!-- Декоративна іконка магазину -->
            <div class="w-24 h-24 bg-orange-500/10 rounded-3xl outline outline-1 outline-offset-[-1px] outline-orange-500/20 flex justify-center items-center">
              <svg class="w-12 h-12 text-orange-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/>
                <polyline points="9 22 9 12 15 12 15 22"/>
              </svg>
            </div>

            <!-- Текст порожнього стану -->
            <div class="flex flex-col items-center gap-2">
              <span class="text-gray-100 text-xl font-bold font-['Onest'] leading-7">
                У вас ще немає магазину
              </span>
              <span class="text-gray-500 text-sm font-normal font-['Onest'] leading-5 text-center max-w-md">
                Створіть свій магазин на Sellora, щоб почати продавати товари через групові покупки
              </span>
            </div>

            <!-- Кнопка «Відкрити магазин» -->
            <button
              class="px-8 py-3 bg-orange-500 rounded-xl flex items-center gap-2 transition-all duration-200 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] focus:outline-none focus:ring-2 focus:ring-orange-500/50"
              @click="isCreatingStore = true"
            >
              <svg class="w-5 h-5 text-white" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10 4v12M4 10h12"/>
              </svg>
              <span class="text-white text-sm font-semibold font-['Onest'] leading-5">Відкрити магазин</span>
            </button>
          </div>

          <!-- ════════════════════════════════════════════════════════════════ -->
          <!-- Стан 2: Форма створення магазину ─────────────────────────────── -->
          <!-- ════════════════════════════════════════════════════════════════ -->
          <CreateStoreForm
            v-else-if="!hasStore && isCreatingStore"
            @created="handleStoreCreated"
          />

          <!-- ════════════════════════════════════════════════════════════════ -->
          <!-- Стан 3: Магазин створено — дашборд магазину + управління товарами -->
          <!-- ════════════════════════════════════════════════════════════════ -->
          <template v-else>

            <!-- —— Режим перегляду (isAddingProduct === false) —— -->
            <template v-if="!isAddingProduct">

              <!-- ── Профіль магазину (Store Header) ──────────────────────── -->
              <div class="bg-[#131720] rounded-2xl p-6 mb-8 outline outline-1 outline-offset-[-1px] outline-[#1a2235]">
                <div class="flex gap-6">

                  <!-- Логотип магазину (клікабельний) -->
                  <div class="relative shrink-0 group/logo cursor-pointer" title="Оновити фото">
                    <!-- Кругле зображення або SVG-заглушка -->
                    <div class="w-24 h-24 rounded-full overflow-hidden bg-[#0d1117] outline outline-2 outline-offset-[-2px] outline-[#2a3a52] flex justify-center items-center">
                      <img
                        v-if="myStore.logo"
                        :src="myStore.logo"
                        alt="Логотип магазину"
                        class="w-full h-full object-cover"
                      />
                      <!-- SVG-заглушка (іконка магазину) -->
                      <svg v-else class="w-10 h-10 text-[#3d5070]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/>
                        <polyline points="9 22 9 12 15 12 15 22"/>
                      </svg>
                    </div>
                    <!-- Оверлей при наведенні —  «Оновити фото» -->
                    <div class="absolute inset-0 rounded-full bg-black/50 opacity-0 group-hover/logo:opacity-100 transition-opacity duration-200 flex justify-center items-center">
                      <svg class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/>
                        <circle cx="12" cy="13" r="4"/>
                      </svg>
                    </div>
                  </div>

                  <!-- Інформація про магазин -->
                  <div class="flex-1 flex flex-col gap-3 min-w-0">

                    <!-- Назва + бейдж категорії + кнопка редагування -->
                    <div class="flex items-start justify-between gap-4">
                      <div class="flex flex-col gap-2 min-w-0">
                        <h2 class="text-gray-100 text-xl font-bold font-['Onest'] leading-7 truncate">
                          {{ myStore?.name || 'Мій магазин' }}
                        </h2>
                        <!-- Бейдж категорії -->
                        <div v-if="myStore?.category" class="inline-flex self-start">
                          <div class="px-3 py-1 bg-orange-500/10 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/20">
                            <span class="text-orange-400 text-xs font-medium font-['Onest'] leading-4">{{ myStore.category }}</span>
                          </div>
                        </div>
                      </div>
                      <!-- Кнопка «Редагувати профіль» -->
                      <button
                        class="shrink-0 px-4 py-2 rounded-xl outline outline-1 outline-offset-[-1px] outline-[#1e2535] text-gray-400 text-xs font-medium font-['Onest'] leading-5 flex items-center gap-2 transition-all duration-150 hover:bg-white/5 hover:text-gray-200 hover:outline-[#2a3a52]"
                        @click="editStore"
                      >
                        <svg class="w-3.5 h-3.5" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M10 1.5l2.5 2.5L4.5 12H2v-2.5L10 1.5z"/>
                        </svg>
                        Редагувати профіль
                      </button>
                    </div>

                    <!-- Адреса та телефон -->
                    <div class="flex items-center gap-4 flex-wrap">
                      <div v-if="myStore?.city" class="flex items-center gap-1.5">
                        <svg class="w-3.5 h-3.5 text-[#3d5070] shrink-0" viewBox="0 0 14 14" fill="none">
                          <path d="M7 1C4.79 1 3 2.79 3 5C3 8.25 7 13 7 13C7 13 11 8.25 11 5C11 2.79 9.21 1 7 1Z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/>
                          <circle cx="7" cy="5" r="1.4" stroke="currentColor" stroke-width="1.2"/>
                        </svg>
                        <span class="text-gray-400 text-xs font-normal font-['Onest'] leading-4">{{ myStore.city }}</span>
                      </div>
                      <div v-if="myStore?.phone" class="flex items-center gap-1.5">
                        <svg class="w-3.5 h-3.5 text-[#3d5070] shrink-0" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M5.06 5.84a6.46 6.46 0 003.1 3.1l1.03-.69a.5.5 0 01.51-.04l2.28.91a.5.5 0 01.31.46v2.17a.5.5 0 01-.54.5A11.5 11.5 0 012.25 2.79a.5.5 0 01.5-.54H4.9a.5.5 0 01.46.31l.91 2.28a.5.5 0 01-.04.51L5.06 5.84z"/>
                        </svg>
                        <span class="text-gray-400 text-xs font-normal font-['Onest'] leading-4">{{ myStore.phone }}</span>
                      </div>
                    </div>

                    <!-- Опис (обрізаний до 3 рядків) -->
                    <p v-if="myStore?.description" class="text-gray-400 text-sm font-normal font-['Onest'] leading-5 line-clamp-3">
                      {{ myStore.description }}
                    </p>

                    <!-- Посилання (сайт / Instagram) -->
                    <div v-if="myStore?.website || myStore?.instagram" class="flex items-center gap-4 pt-1">
                      <a
                        v-if="myStore.website"
                        :href="myStore.website"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="flex items-center gap-1.5 text-[#3d5070] hover:text-orange-400 transition-colors"
                      >
                        <svg class="w-3.5 h-3.5" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.2">
                          <circle cx="7" cy="7" r="5.5"/>
                          <path d="M1.5 7H12.5M7 1.5C7 1.5 5 4 5 7C5 10 7 12.5 7 12.5C7 12.5 9 10 9 7C9 4 7 1.5 7 1.5Z" stroke-linecap="round"/>
                        </svg>
                        <span class="text-xs font-normal font-['Onest'] leading-4">Сайт</span>
                      </a>
                      <a
                        v-if="myStore.instagram"
                        :href="'https://instagram.com/' + myStore.instagram.replace('@', '')"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="flex items-center gap-1.5 text-[#3d5070] hover:text-orange-400 transition-colors"
                      >
                        <svg class="w-3.5 h-3.5" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.2">
                          <rect x="1.5" y="1.5" width="11" height="11" rx="3"/>
                          <circle cx="7" cy="7" r="2.5"/>
                          <circle cx="10.5" cy="3.5" r="0.6" fill="currentColor" stroke="none"/>
                        </svg>
                        <span class="text-xs font-normal font-['Onest'] leading-4">{{ myStore.instagram }}</span>
                      </a>
                    </div>

                  </div>
                </div>
              </div>
              <!-- / Профіль магазину -->

              <!-- ── Управління товарами ──────────────────────────────────── -->

              <!-- Заголовок секції + кількість товарів -->
              <div class="mb-5 flex items-center justify-between">
                <div class="inline-flex gap-2 items-center">
                  <span class="text-gray-100 text-lg font-bold font-['Onest'] leading-7">Мої товари</span>
                  <span class="text-gray-600 text-sm font-normal font-['Onest'] leading-5">{{ myStoreProducts.length }} активних</span>
                </div>

                <!-- Кнопка «Додати новий товар» -->
                <button
                  class="px-5 py-2.5 bg-orange-500 rounded-xl flex items-center gap-2 transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] focus:outline-none focus:ring-2 focus:ring-orange-500/50"
                  @click="isAddingProduct = true"
                >
                  <svg class="w-4 h-4 text-white" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M8 3v10M3 8h10" stroke-linecap="round"/>
                  </svg>
                  <span class="text-white text-sm font-semibold font-['Onest'] leading-5">Додати новий товар</span>
                </button>
              </div>

              <!-- Сітка карток товарів магазину -->
              <div v-if="myStoreProducts.length > 0" class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
                <ProductCard
                  v-for="product in myStoreProducts"
                  :key="(product as any).id"
                  :product="product"
                  :simple="true"
                />
              </div>

              <!-- Порожній стан — товарів ще немає -->
              <div v-else class="flex flex-col items-center justify-center py-16 gap-4">
                <div class="w-16 h-16 bg-[#131720] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1a2235] flex justify-center items-center">
                  <svg class="w-8 h-8 text-[#2a3a52]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 002 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0022 16z"/>
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                    <line x1="12" y1="22.08" x2="12" y2="12"/>
                  </svg>
                </div>
                <span class="text-gray-500 text-sm font-normal font-['Onest'] leading-5">
                  Ви ще не додали жодного товару
                </span>
                <span class="text-[#2a3a52] text-xs font-normal font-['Onest'] leading-4">
                  Натисніть «Додати новий товар», щоб розпочати
                </span>
              </div>

            </template>

            <!-- —— Режим форми (isAddingProduct === true) ——
                 Повністю замінює дашборд на форму AddProductForm.
                 @save — подія від форми зі збереженим товаром: додаємо у масив.
                 @close — подія від форми (скасування): повертаємо до списку. -->
            <AddProductForm
              v-else
              @save="handleProductSave"
              @close="isAddingProduct = false"
            />

          </template>

        </div>

        <!-- ── Вкладка: Налаштування (settings) ──────────────────────────────── -->
        <div v-else-if="activeTab === 'settings'">

          <!-- Заголовок -->
          <div class="mb-6 flex flex-col gap-1">
            <span class="text-gray-100 text-2xl font-black font-['Unbounded'] leading-8">Налаштування</span>
            <span class="text-gray-600 text-sm font-normal font-['Onest'] leading-5">
              Керуйте своїм профілем та параметрами акаунту
            </span>
          </div>

          <!-- Картка налаштувань профілю -->
          <div class="p-6 bg-gray-900 rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] flex flex-col gap-5">

            <!-- Аватар та ім'я -->
            <div class="flex items-center gap-4">
              <div class="w-16 h-16 p-0.5 bg-gradient-to-br from-orange-500 to-violet-600 rounded-[30px] flex justify-center items-center shrink-0">
                <div class="w-full h-full bg-[#1a2235] rounded-3xl flex justify-center items-center">
                  <span class="text-orange-500 text-xl font-bold font-['Unbounded'] leading-7">МК</span>
                </div>
              </div>
              <div class="flex flex-col gap-0.5">
                <span class="text-gray-100 text-base font-bold font-['Onest']">Максим Коваленко</span>
                <span class="text-gray-600 text-xs font-normal font-['Onest']">{{ userStore.user?.email ?? '' }}</span>
              </div>
              <button class="ml-auto px-4 py-2 rounded-xl outline outline-1 outline-gray-700 text-gray-400 text-sm font-['Onest'] transition-all duration-150 hover:bg-white/5 hover:text-white">
                Змінити фото
              </button>
            </div>

            <div class="h-px bg-[#1e2d3d]" />

            <!-- Поля форми -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">Ім'я</label>
                <input
                  type="text"
                  value="Максим Коваленко"
                  class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500"
                />
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-gray-400 text-xs font-medium font-['Onest'] uppercase leading-4 tracking-tight">Email</label>
                <input
                  type="email"
                  :value="userStore.user?.email ?? ''"
                  class="w-full px-4 py-3 bg-neutral-900 rounded-xl outline outline-1 outline-offset-[-1px] outline-gray-700 text-gray-300 text-sm font-normal font-['Onest'] transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-orange-500"
                />
              </div>
            </div>

            <!-- Кнопка зберегти -->
            <div class="flex justify-end">
              <button class="px-6 py-2.5 bg-orange-500 rounded-xl text-white text-sm font-semibold font-['Onest'] transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98]">
                Зберегти зміни
              </button>
            </div>
          </div>
        </div>

      </div>
      <!-- / Права колонка -->

    </main>

    <Footer />

  </div>
</template>
