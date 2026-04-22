<script setup lang="ts">
import { ref } from 'vue'
import Header           from './Header.vue'
import Footer           from './Footer.vue'
import CabinetSidebar   from './CabinetSidebar.vue'
import CabinetGroupBuys from './CabinetGroupBuys.vue'
import ProductCard       from './ProductCard.vue'
// —— Імпорт форми додавання товару ——
// Підключаємо компонент форми, яка відображається замість списку товарів
// коли продавець натискає «Додати товар».
import AddProductForm   from './AddProductForm.vue'

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

// ─── Крок 4 (US 4.2): Захист ролі ────────────────────────────────────────────
// Тимчасова змінна для тестування видимості вкладки «Мої товари».
// true  → користувач є продавцем, вкладка «Мої товари» відображається в меню.
// false → користувач — покупець, вкладка прихована.
// TODO: замінити на реальне значення з auth store / API після інтеграції бекенду.
const isUserSeller = ref(true)

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

// ─── Mock-дані для вкладки «Мої товари» (тільки для продавця) ────────────────
// Відображається лише якщо isUserSeller.value === true.
const mockMyProducts = ref([
  {
    id:           301,
    brand:        'TechShop UA',
    name:         'Механічна клавіатура AKKO 3068B',
    image:        'https://ae01.alicdn.com/kf/S6e5e3a0d70af43829b63a34d3d04f00cE.jpg',
    imageAlt:     'AKKO 3068B Keyboard',
    rating:       4,
    reviewCount:  22,
    groupLabel:   'Учасників',
    groupCurrent: 2,
    groupTotal:   5,
    price:        2999,
    oldPrice:     3799,
  },
  {
    id:           302,
    brand:        'TechShop UA',
    name:         'USB-C Hub 7-in-1 Baseus',
    image:        'https://baseusglobal.com/cdn/shop/products/baseus_usbc_hub_7in1_1200x1200.jpg',
    imageAlt:     'Baseus USB-C Hub',
    rating:       4,
    reviewCount:  47,
    groupLabel:   'Учасників',
    groupCurrent: 1,
    groupTotal:   3,
    price:        1199,
    oldPrice:     1599,
  },
])
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

          <!-- —— Режим перегляду списку товарів (isAddingProduct === false) —— -->
          <!-- Відображаємо заголовок, кнопку «Додати товар» та сітку карток -->
          <template v-if="!isAddingProduct">

            <!-- Заголовок вкладки продавця -->
            <div class="mb-6 flex flex-col gap-1">
              <div class="inline-flex gap-2 flex-wrap items-center">
                <span class="text-gray-100 text-2xl font-black font-['Unbounded'] leading-8">Мої товари</span>
                <span class="text-gray-600 text-2xl font-normal font-['Unbounded'] leading-8">/ {{ mockMyProducts.length }} активних</span>
                <!-- Бейдж ролі продавця -->
                <div class="px-2.5 py-[3px] bg-orange-500/10 rounded-full outline outline-1 outline-offset-[-1px] outline-orange-500/20">
                  <span class="text-orange-500 text-xs font-normal font-['Onest'] leading-4">🏪 Продавець</span>
                </div>
              </div>
              <span class="text-gray-600 text-sm font-normal font-['Onest'] leading-5">
                Товари, які ви виставили на продаж через групові покупки
              </span>
            </div>

            <!-- —— Кнопка «Додати товар» ——
                 При кліку переводимо isAddingProduct у true —
                 список зникне, з'явиться форма AddProductForm. -->
            <div class="mb-5">
              <button
                class="px-5 py-2.5 bg-orange-500 rounded-xl flex items-center gap-2 transition-all duration-150 hover:bg-orange-400 hover:shadow-[0px_4px_20px_0px_rgba(249,115,22,0.45)] active:scale-[0.98] focus:outline-none focus:ring-2 focus:ring-orange-500/50"
                @click="isAddingProduct = true"
              >
                <svg class="w-4 h-4 text-white" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M8 3v10M3 8h10" stroke-linecap="round"/>
                </svg>
                <span class="text-white text-sm font-semibold font-['Onest'] leading-5">Додати товар</span>
              </button>
            </div>

            <!-- Сітка карток існуючих товарів продавця -->
            <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
              <ProductCard
                v-for="item in mockMyProducts"
                :key="item.id"
                :product="item"
                :simple="true"
              />
            </div>

          </template>

          <!-- —— Режим форми (isAddingProduct === true) ——
               Повністю замінює список на форму AddProductForm.
               @close — подія від форми (скасування або збереження):
               повертає isAddingProduct у false → знову показуємо список. -->
          <AddProductForm
            v-else
            @close="isAddingProduct = false"
          />

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
                <span class="text-gray-600 text-xs font-normal font-['Onest']">m.kovalenko@gmail.com</span>
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
                  value="m.kovalenko@gmail.com"
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
