<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '../../state/userStore'
import { useGroupBuyStore } from '../../state/groupBuyStore'
import { useProductStore } from '../../state/productStore'
import { apiClient } from '../../api/axios'

const ordersCount = ref(0)

// ─── Types ────────────────────────────────────────────────────────────────────

// Тип іконки розширено: додано 'seller' для вкладки «Мої товари» (US 4.2)
interface MenuItem {
  id: string
  label: string
  count?: number
  icon: 'orders' | 'groups' | 'wishlist' | 'settings' | 'seller'
}

// ─── Props ────────────────────────────────────────────────────────────────────
// activeTab    — id активної вкладки, керується батьківським CabinetPage
// isUserSeller — якщо true, відображається пункт «Мої товари» (US 4.2)
const props = defineProps<{
  activeTab:    string
  isUserSeller: boolean
}>()

// ─── Ініціалізація store та router ───────────────────────────────────────────
const userStore     = useUserStore()
const groupBuyStore = useGroupBuyStore()
const productStore  = useProductStore()

// ─── Завантаження сесій при монтуванні (щоб лічильник був актуальним) ──────────
onMounted(() => {
  groupBuyStore.fetchMySessions()
  productStore.fetchFavoritesCount()

  if (props.isUserSeller && userStore.sellerStore?.id) {
    productStore.fetchMerchantProducts(userStore.sellerStore.id)
  }

  // Отримуємо кількість замовлень
  apiClient.get('/orders', { params: { page: 0, size: 1 } })
    .then(res => {
      ordersCount.value = res.data?.totalElements || res.data?.content?.length || 0
    })
    .catch(err => console.error('Failed to fetch orders count', err))
})


// ─── Menu items ───────────────────────────────────────────────────────────────
// Базові пункти меню (доступні всім користувачам)
const baseMenuItems = computed<MenuItem[]>(() => [
  { id: 'orders',   label: 'Мої замовлення',      count: ordersCount.value,                                      icon: 'orders'   },
  { id: 'groups',   label: 'Мої групові покупки', count: groupBuyStore.mySessions.length,  icon: 'groups'   },
  { id: 'wishlist', label: 'Обране',              count: productStore.favoritesCount,             icon: 'wishlist' },
  { id: 'settings', label: 'Налаштування',                                                       icon: 'settings' },
])

// Крок 4 (US 4.2): Пункт «Мої товари» додається до меню тільки для продавця.
// computed автоматично перераховується, якщо props.isUserSeller зміниться.
const menuItems = computed<MenuItem[]>(() => {
  const base = baseMenuItems.value
  const items = !props.isUserSeller ? [...base] : [
    ...base.slice(0, 3),
    { id: 'my-products', label: 'Мої товари', count: productStore.myProducts.length, icon: 'seller' as const },
    { id: 'store-orders', label: 'Замовлення магазину', icon: 'orders' as const },
    { id: 'requisites', label: 'Платіжні реквізити', icon: 'orders' as const },
    ...base.slice(3),
  ]

  // Додаємо пункт «Категорії» тільки для адміна (перед «Налаштування»)
  const isAdmin = userStore.user?.role === 'ADMIN'
  if (isAdmin) {
    const settingsIndex = items.findIndex(i => i.id === 'settings')
    items.splice(settingsIndex, 0, 
      { id: 'stores', label: 'Магазини', icon: 'groups' },
      { id: 'categories', label: 'Категорії', icon: 'settings' },
      { id: 'shipping-carriers', label: 'Поштові служби', icon: 'orders' },
      { id: 'promo-codes', label: 'Промокоди', icon: 'seller' }
    )
  }

  return items
})

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'navigate', id: string): void
}>()

// ─── Навігація по вкладках ────────────────────────────────────────────────────
// Надсилаємо вибраний id у батьківський компонент через emit.
// Батьківський CabinetPage оновлює свій activeTab і передає його назад через prop.
function navigate(id: string) {
  emit('navigate', id)
}

// ─── Логіка виходу ────────────────────────────────────────────────────────────
// Показуємо підтвердження перед виходом, щоб уникнути випадкового натискання.
async function handleLogout() {
  if (window.confirm('Ви точно хочете вийти?')) {
    try {
      await userStore.logout() // Обов'язково чекаємо, поки токен успішно видалиться
    } catch (err) {
      console.error('Помилка при виході:', err)
    } finally {
      localStorage.removeItem('pendingFavorite')
      window.location.href = '/' // Жорсткий рефреш тільки ПІСЛЯ логауту
    }
  }
}
</script>

<template>
  <div class="w-full inline-flex flex-col justify-start items-start">
    <div class="self-stretch p-4 bg-gray-900 rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1e2d3d] flex flex-col gap-3">

      <!-- ── Avatar + user info ────────────────────────────────────────── -->
      <div class="self-stretch flex flex-col items-center gap-2 pt-4 pb-2">
        <!-- Gradient avatar ring -->
        <div class="w-14 h-14 p-0.5 bg-gradient-to-br from-orange-500 to-violet-600 rounded-[30px] flex justify-center items-center shrink-0">
          <div class="w-full h-full bg-[#1a2235] rounded-[28px] overflow-hidden flex justify-center items-center">
            <img v-if="userStore.user?.avatarUrl" :src="userStore.user.avatarUrl" alt="Аватар" class="w-full h-full object-cover" />
            <span v-else class="text-orange-500 text-xl font-bold font-['Unbounded'] leading-7">
              {{ userStore.user?.email?.[0]?.toUpperCase() || 'U' }}
            </span>
          </div>
        </div>

        <!-- Name -->
        <!-- Відображаємо частину пошти до символу @ як ім'я користувача -->
        <span class="text-gray-100 text-base font-bold font-['Onest'] leading-5 text-center">
          {{ userStore.user?.email?.split('@')[0] || 'Користувач' }}
        </span>

        <!-- Email -->
        <!-- Відображаємо повну поштову адресу користувача -->
        <span class="text-gray-600 text-xs font-normal font-['Onest'] leading-4 text-center">
          {{ userStore.user?.email }}
        </span>

      </div>

      <div class="self-stretch h-px bg-[#1e2d3d]" />

      <!-- ── Navigation items ──────────────────────────────────────────── -->
      <nav class="self-stretch flex flex-col gap-0.5">
        <button
          v-for="item in menuItems"
          :key="item.id"
          class="self-stretch px-4 py-2.5 rounded-[9.6px] inline-flex justify-start items-center gap-3 transition-all duration-150 focus:outline-none"
          :class="
            props.activeTab === item.id
              ? 'bg-orange-500/10 text-orange-500'
              : 'text-gray-400 hover:bg-white/5 hover:text-white'
          "
          @click="navigate(item.id)"
        >
          <!-- Icon -->
          <span class="w-3.5 h-3.5 shrink-0 flex items-center justify-center">
            <!-- Orders: clipboard list -->
            <svg v-if="item.icon === 'orders'" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1" class="w-3.5 h-3.5">
              <rect x="2" y="1.5" width="10" height="11" rx="1.5"/>
              <path d="M4.5 5h5M4.5 7.5h5M4.5 10h3" stroke-linecap="round"/>
              <path d="M5 1.5V3a1 1 0 002 0V1.5" stroke-linecap="round"/>
            </svg>

            <!-- Groups: user-group -->
            <svg v-else-if="item.icon === 'groups'" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1" class="w-3.5 h-3.5">
              <circle cx="5.5" cy="4.5" r="2"/>
              <path d="M1 12a4.5 4.5 0 019 0" stroke-linecap="round"/>
              <circle cx="10.5" cy="4.5" r="1.5"/>
              <path d="M13 12c0-2-1-3.5-2.5-4" stroke-linecap="round"/>
            </svg>

            <!-- Wishlist: heart -->
            <svg v-else-if="item.icon === 'wishlist'" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1" class="w-3.5 h-3.5">
              <path d="M7 11.5S1.5 8 1.5 4.5A2.5 2.5 0 017 3.2a2.5 2.5 0 015.5 1.3C12.5 8 7 11.5 7 11.5z" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>

            <!-- Settings: cog -->
            <svg v-else-if="item.icon === 'settings'" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1" class="w-3.5 h-3.5">
              <circle cx="7" cy="7" r="2"/>
              <path d="M7 1v1.5M7 11.5V13M1 7h1.5M11.5 7H13M2.93 2.93l1.06 1.06M9.9 9.9l1.1 1.1M2.93 11.07l1.06-1.06M9.9 4.1l1.1-1.1" stroke-linecap="round"/>
            </svg>

            <!-- Seller / My products: tag -->
            <!-- US 4.2: Іконка відображається тільки для пункту «Мої товари» продавця -->
            <svg v-else-if="item.icon === 'seller'" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1" class="w-3.5 h-3.5">
              <path d="M1.5 1.5h3l2 2h6v8h-11z" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M5 8h4M7 6v4" stroke-linecap="round"/>
            </svg>
          </span>

          <!-- Label -->
          <span class="text-sm font-normal font-['Onest'] leading-5">{{ item.label }}</span>

          <!-- Count badge -->
          <div v-if="item.count != null && item.count > 0" class="flex-1 inline-flex justify-end">
            <div
              class="px-2 py-[1.6px] rounded-full flex items-center justify-center min-w-[24px]"
              :class="props.activeTab === item.id ? 'bg-orange-500/20' : 'bg-gray-800'"
            >
              <span
                class="text-xs font-medium font-['Onest'] leading-4 text-center"
                :class="props.activeTab === item.id ? 'text-orange-500' : 'text-gray-500'"
              >
                {{ item.count > 99 ? '99+' : item.count }}
              </span>
            </div>
          </div>
        </button>

        <!-- Divider -->
        <div class="self-stretch h-4 flex items-center">
          <div class="self-stretch w-full h-px bg-[#1e2d3d]" />
        </div>

        <!-- Logout -->
        <!-- Кнопка виходу: викликає handleLogout з підтвердженням -->
        <button
          class="self-stretch px-4 py-2.5 rounded-[9.6px] inline-flex justify-start items-center gap-3 text-red-400/70 transition-all duration-150 hover:bg-red-500/5 hover:text-red-400 active:scale-[0.98] focus:outline-none"
          @click="handleLogout"
        >
          <svg class="w-3.5 h-3.5 shrink-0" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.1">
            <path d="M5 12H3a1 1 0 01-1-1V3a1 1 0 011-1h2" stroke-linecap="round"/>
            <path d="M9.5 9.5L13 7l-3.5-2.5M13 7H5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="text-sm font-normal font-['Onest'] leading-5">Вихід</span>
        </button>
      </nav>

    </div>
  </div>
</template>
