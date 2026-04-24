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

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-4 md:px-6 py-6 md:py-12 flex flex-col lg:flex-row gap-6 md:gap-8 items-start">

      <div class="w-full lg:w-1/4 lg:sticky lg:top-6 shrink-0">
        <CabinetSidebar
          :activeTab="activeTab"
          :isUserSeller="isUserSeller"
          @navigate="activeTab = $event"
        />
      </div>

      <div class="w-full lg:w-3/4 flex flex-col gap-6">

        <div v-if="activeTab === 'orders'">

          <div class="mb-6 flex flex-col gap-1">
            <div class="inline-flex gap-2 flex-wrap">
              <span class="text-gray-100 text-xl md:text-2xl font-black font-['Unbounded'] leading-8">Мої замовлення</span>
              <span class="text-gray-600 text-xl md:text-2xl font-normal font-['Unbounded'] leading-8">/ {{ mockOrders.length }} товари</span>
            </div>
            <span class="text-gray-600 text-sm font-normal font-['Onest'] leading-5">
              Ваші активні та завершені замовлення через групові покупки
            </span>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5 justify-items-center">
            <ProductCard
              v-for="item in mockOrders"
              :key="item.id"
              :product="item"
              :simple="true"
            />
          </div>
        </div>

        <CabinetGroupBuys v-else-if="activeTab === 'groups'" />

        <div v-else-if="activeTab === 'wishlist'">

          <div class="mb-6 flex flex-col gap-1">
            <div class="inline-flex gap-2 flex-wrap">
              <span class="text-gray-100 text-xl md:text-2xl font-black font-['Unbounded'] leading-8">Обране</span>
              <span class="text-gray-600 text-xl md:text-2xl font-normal font-['Unbounded'] leading-8">/ {{ mockFavorites.length }} товари</span>
            </div>
            <span class="text-gray-600 text-sm font-normal font-['Onest'] leading-5">
              Товари, які ви зберегли для майбутньої покупки
            </span>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5 justify-items-center">
            <ProductCard
              v-for="item in mockFavorites"
              :key="item.id"
              :product="item"
              :simple="true"
            />
          </div>
        </div>

        <div v-else-if="activeTab === 'my-products' && isUserSeller">

          <div v-if="!hasStore && !isCreatingStore" class="flex flex-col items-center justify-center py-20 gap-6 text-center">
            <div class="w-24 h-24 bg-orange-500/10 rounded-3xl outline outline-1 outline-offset-[-1px] outline-orange-500/20 flex justify-center items-center">
              <svg class="w-12 h-12 text-orange-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/>
                <polyline points="9 22 9 12 15 12 15 22"/>
              </svg>
            </div>
            <div class="flex flex-col items-center gap-2">
              <span class="text-gray-100 text-xl font-bold font-['Onest'] leading-7">У вас ще немає магазину</span>
              <span class="text-gray-500 text-sm font-normal font-['Onest'] leading-5 max-w-md px-4">Створіть свій магазин на Sellora, щоб почати продавати товари через групові покупки</span>
            </div>
            <button class="px-8 py-3 bg-orange-500 rounded-xl flex items-center gap-2 transition-all duration-200 hover:bg-orange-400 active:scale-95" @click="isCreatingStore = true">
              <span class="text-white text-sm font-semibold">Відкрити магазин</span>
            </button>
          </div>

          <CreateStoreForm v-else-if="!hasStore && isCreatingStore" @created="handleStoreCreated" />

          <template v-else>
            <template v-if="!isAddingProduct">

              <div class="bg-[#131720] rounded-2xl p-4 md:p-6 mb-8 outline outline-1 outline-[#1a2235]">
                <div class="flex flex-col sm:flex-row gap-6 items-center sm:items-start text-center sm:text-left">
                  <div class="relative shrink-0 cursor-pointer group/logo">
                    <div class="w-24 h-24 rounded-full overflow-hidden bg-[#0d1117] outline outline-2 outline-offset-[-2px] outline-[#2a3a52] flex justify-center items-center">
                      <img v-if="myStore.logo" :src="myStore.logo" class="w-full h-full object-cover" />
                      <svg v-else class="w-10 h-10 text-[#3d5070]" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/></svg>
                    </div>
                  </div>

                  <div class="flex-1 flex flex-col gap-3 min-w-0 w-full">
                    <div class="flex flex-col md:flex-row md:items-start justify-between gap-4">
                      <div class="flex flex-col gap-2">
                        <h2 class="text-gray-100 text-xl font-bold truncate">{{ myStore?.name || 'Мій магазин' }}</h2>
                        <div v-if="myStore?.category" class="inline-flex self-center sm:self-start">
                          <div class="px-3 py-1 bg-orange-500/10 rounded-full outline outline-1 outline-orange-500/20">
                            <span class="text-orange-400 text-xs font-medium">{{ myStore.category }}</span>
                          </div>
                        </div>
                      </div>
                      <button class="px-4 py-2 rounded-xl outline outline-1 outline-[#1e2535] text-gray-400 text-xs font-medium flex items-center justify-center gap-2 hover:bg-white/5" @click="editStore">
                        Редагувати профіль
                      </button>
                    </div>

                    <div class="flex flex-wrap justify-center sm:justify-start items-center gap-4">
                      <div v-if="myStore?.city" class="flex items-center gap-1.5"><span class="text-gray-400 text-xs">📍 {{ myStore.city }}</span></div>
                      <div v-if="myStore?.phone" class="flex items-center gap-1.5"><span class="text-gray-400 text-xs">📞 {{ myStore.phone }}</span></div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="mb-5 flex flex-col md:flex-row items-center justify-between gap-4">
                <div class="inline-flex gap-2 items-center">
                  <span class="text-gray-100 text-lg font-bold">Мої товари</span>
                  <span class="text-gray-600 text-sm">{{ myStoreProducts.length }} активних</span>
                </div>
                <button class="w-full md:w-auto px-5 py-2.5 bg-orange-500 rounded-xl flex items-center justify-center gap-2 hover:bg-orange-400 active:scale-95" @click="isAddingProduct = true">
                  <span class="text-white text-sm font-semibold">Додати новий товар</span>
                </button>
              </div>

              <div v-if="myStoreProducts.length > 0" class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5 justify-items-center">
                <ProductCard v-for="product in myStoreProducts" :key="(product as any).id" :product="product" :simple="true" />
              </div>

              <div v-else class="flex flex-col items-center justify-center py-16 gap-4 text-center">
                <span class="text-gray-500 text-sm px-6">Ви ще не додали жодного товару. Натисніть «Додати новий товар», щоб розпочати</span>
              </div>

            </template>
            <AddProductForm v-else @save="handleProductSave" @close="isAddingProduct = false" />
          </template>

        </div>

        <div v-else-if="activeTab === 'settings'">

          <div class="mb-6 flex flex-col gap-1">
            <span class="text-gray-100 text-2xl font-black font-['Unbounded']">Налаштування</span>
          </div>

          <div class="p-4 md:p-6 bg-gray-900 rounded-2xl outline outline-1 outline-[#1e2d3d] flex flex-col gap-6">
            <div class="flex flex-col sm:flex-row items-center gap-4 text-center sm:text-left">
              <div class="w-16 h-16 p-0.5 bg-gradient-to-br from-orange-500 to-violet-600 rounded-full flex justify-center items-center shrink-0">
                <div class="w-full h-full bg-[#1a2235] rounded-full flex justify-center items-center">
                  <span class="text-orange-500 text-xl font-bold font-['Unbounded']">{{ (userStore.user?.email?.[0] || 'M').toUpperCase() }}</span>
                </div>
              </div>
              <div class="flex flex-col gap-0.5 flex-1 min-w-0">
                <span class="text-gray-100 text-base font-bold truncate">Максим Коваленко</span>
                <span class="text-gray-600 text-xs truncate">{{ userStore.user?.email ?? '' }}</span>
              </div>
              <button class="w-full sm:w-auto px-4 py-2 rounded-xl outline outline-1 outline-gray-700 text-gray-400 text-sm hover:text-white transition-all">Змінити фото</button>
            </div>

            <div class="h-px bg-[#1e2d3d]" />

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-gray-400 text-xs font-medium uppercase tracking-tight">Ім'я</label>
                <input type="text" value="Максим Коваленко" class="w-full px-4 py-3 bg-[#0d1117] rounded-xl outline outline-1 outline-gray-700 text-gray-300 text-sm focus:outline-orange-500" />
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-gray-400 text-xs font-medium uppercase tracking-tight">Email</label>
                <input type="email" :value="userStore.user?.email ?? ''" class="w-full px-4 py-3 bg-[#0d1117] rounded-xl outline outline-1 outline-gray-700 text-gray-300 text-sm focus:outline-orange-500" />
              </div>
            </div>

            <div class="flex justify-center sm:justify-end">
              <button class="w-full sm:w-auto px-8 py-2.5 bg-orange-500 rounded-xl text-white text-sm font-semibold hover:bg-orange-400 transition-all">Зберегти зміни</button>
            </div>
          </div>
        </div>

      </div>
    </main>

    <Footer />

  </div>
</template>

<style>
/* Твої стилі залишаються без змін */
body {
  margin: 0;
  padding: 0;
  overflow-x: hidden;
}

.scrollbar-none::-webkit-scrollbar {
  display: none;
}
.scrollbar-none {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
