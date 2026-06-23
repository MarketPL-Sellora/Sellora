<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header           from '../components/layout/Header.vue'
import Footer           from '../components/layout/Footer.vue'
import CabinetSidebar   from '../components/layout/CabinetSidebar.vue'
import CabinetGroupBuys from '../components/cabinet/CabinetGroupBuys.vue'
import ProductCard      from '../components/product/ProductCard.vue'
import AddProductForm   from '../components/forms/AddProductForm.vue'
import CreateStoreForm  from '../components/forms/CreateStoreForm.vue'
import CabinetCategories from '../components/cabinet/CabinetCategories.vue'
import CabinetStores from '../components/cabinet/CabinetStores.vue'
import CabinetShippingCarriers from '../components/cabinet/CabinetShippingCarriers.vue'
import CabinetPromoCodes from '../components/cabinet/CabinetPromoCodes.vue'
import CabinetRequisites from '../components/cabinet/CabinetRequisites.vue'
import CabinetSettings from '../components/cabinet/CabinetSettings.vue'
import CabinetOrders from '../components/cabinet/CabinetOrders.vue'
import CabinetStoreOrders from '../components/cabinet/CabinetStoreOrders.vue'
import ProductGrid from '../components/product/ProductGrid.vue'
import { useUserStore } from '../state/userStore'
import { useProductStore } from '../state/productStore'
import { useConfirmStore } from '../state/confirmStore'

const userStore = useUserStore()
const productStore = useProductStore()
const confirmStore = useConfirmStore()

const route = useRoute()
const router = useRouter()

const activeTab = computed({
  get: () => (route.query.tab as string) || 'orders',
  set: (tab) => router.replace({ query: { ...route.query, tab } })
})
const isAddingProduct = ref(false)
const isCreatingStoreForm = ref(false)
const editingProductId = ref<number | null>(null)
const processingActionId = ref<number | string | null>(null)

// Магазин вважається існуючим, якщо він є в сторі
const hasStore = computed(() => !!userStore.sellerStore)
const isUserSeller = ref(true)

async function loadSellerStore() {
  if (userStore.user?.id) {
    await userStore.fetchUserStore(userStore.user.id)
  }
}

onMounted(loadSellerStore)

// Якщо юзер залогінився вже після того, як кабінет відкрився
watch(() => userStore.user?.id, (id) => {
  if (id) userStore.fetchUserStore(id)
})

// Коли з'являється магазин (sellerStore.id), автоматично вантажимо його товари
watch(() => userStore.sellerStore?.id, (merchantId) => {
  if (merchantId) {
    productStore.fetchMerchantProducts(merchantId)
  }
}, { immediate: true })

// Mock-дані для обраного

watch(activeTab, (newTab) => {
  if (newTab === 'wishlist') {
    productStore.fetchProducts({ onlyFavorites: true, page: 0 })
  } else {
    // Очищаємо товари, якщо юзер вийшов з вкладки, щоб не було "моргання" старих даних
    productStore.products = []
    productStore.totalElements = 0
  }
}, { immediate: true })

async function handleStoreCreated() {
  isCreatingStoreForm.value = false
  if (userStore.user?.id) {
    await userStore.fetchUserStore(userStore.user.id)
  }
}

// Функція для закриття форми і оновлення списку
async function handleFormClose() {
  isAddingProduct.value = false
  editingProductId.value = null
  if (userStore.sellerStore?.id) {
    await productStore.fetchMerchantProducts(userStore.sellerStore.id)
  }
}

function editStore() {
  isCreatingStoreForm.value = true
}

async function handleDeleteProduct(id: number) {
  const isConfirmed = await confirmStore.ask('Увага', 'Ви дійсно хочете видалити цей товар?')
  if (!isConfirmed) return
  processingActionId.value = id
  try {
    const success = await productStore.deleteProduct(id)
    if (success && userStore.sellerStore?.id) {
      await productStore.fetchMerchantProducts(userStore.sellerStore.id)
    }
  } finally {
    processingActionId.value = null
  }
}

async function handleChangeProductStatus(payload: { id: number; status: string }) {
  processingActionId.value = payload.id
  try {
    const success = await productStore.changeProductStatus(payload.id, payload.status)
    if (success && userStore.sellerStore?.id) {
      await productStore.fetchMerchantProducts(userStore.sellerStore.id)
    }
  } finally {
    processingActionId.value = null
  }
}

function openEditForm(id: number) {
  editingProductId.value = id
  isAddingProduct.value = true
}

async function handleDeleteStore() {
  if (!userStore.sellerStore?.id) return
  const isConfirmed = await confirmStore.ask('Увага', 'Ви дійсно хочете видалити свій магазин? Цю дію неможливо скасувати.')
  if (!isConfirmed) return
  processingActionId.value = 'store'
  try {
    await userStore.deleteStore(userStore.sellerStore.id)
  } finally {
    processingActionId.value = null
  }
}

async function handleStoreStatus(newStatus: string) {
  if (!userStore.sellerStore?.id) return
  const actionText = newStatus === 'CLOSED' ? 'закрити' : 'активувати'
  const isConfirmed = await confirmStore.ask('Увага', `Ви дійсно хочете ${actionText} свій магазин?`)
  if (!isConfirmed) return
  processingActionId.value = 'store'
  try {
    const success = await userStore.changeStoreStatus(userStore.sellerStore.id, newStatus)
    if (success) {
      await userStore.fetchUserStore(userStore.user!.id) // Оновлюємо дані магазину
    }
  } finally {
    processingActionId.value = null
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#0f1117] flex flex-col font-['Onest'] text-white">
    <Header :showCategories="false" />

    <main class="flex-1 w-full max-w-[1536px] mx-auto px-4 md:px-6 py-6 md:py-12 flex flex-col lg:flex-row gap-6 md:gap-8 items-start">
      <div class="w-full lg:w-1/4 lg:sticky lg:top-6 shrink-0">
        <CabinetSidebar :activeTab="activeTab" :isUserSeller="isUserSeller" @navigate="activeTab = $event" />
      </div>

      <div class="w-full lg:w-3/4 flex flex-col gap-6">

        <CabinetOrders v-if="activeTab === 'orders'" />

        <CabinetStoreOrders v-else-if="activeTab === 'store-orders' && hasStore" />

        <div v-else-if="activeTab === 'wishlist'">
          <div class="mb-6"><span class="text-gray-100 text-xl font-black font-['Unbounded']">Обране</span></div>
          <ProductGrid />
        </div>

        <CabinetGroupBuys v-else-if="activeTab === 'groups'" />

        <div v-else-if="activeTab === 'my-products' && isUserSeller">

          <div v-if="userStore.isLoadingStore" class="flex justify-center py-20 animate-pulse text-slate-500">Завантажуємо магазин...</div>

          <template v-else-if="!hasStore && !isCreatingStoreForm">
            <div class="flex flex-col items-center justify-center py-20 gap-6 text-center">
              <div class="w-24 h-24 bg-orange-500/10 rounded-3xl flex justify-center items-center">
                <svg class="w-12 h-12 text-orange-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
              </div>
              <span class="text-gray-100 text-xl font-bold">У вас ще немає магазину</span>
              <button class="px-8 py-3 bg-orange-500 rounded-xl text-white font-semibold hover:bg-orange-400" @click="isCreatingStoreForm = true">Відкрити магазин</button>
            </div>
          </template>

          <CreateStoreForm v-else-if="isCreatingStoreForm" :initialData="hasStore ? userStore.sellerStore : null" @created="handleStoreCreated" @cancel="isCreatingStoreForm = false" />

          <template v-else-if="hasStore">
            <template v-if="!isAddingProduct">
              <div class="bg-[#131720] rounded-2xl p-6 mb-8 outline outline-1 outline-[#1a2235]">
                <div class="flex flex-col sm:flex-row gap-6 items-center">
                  <div class="w-24 h-24 rounded-full overflow-hidden bg-[#0d1117] outline outline-2 outline-[#2a3a52] flex justify-center items-center">
                    <img v-if="userStore.sellerStore?.logoUrl" :src="userStore.sellerStore.logoUrl" class="w-full h-full object-cover" />
                    <svg v-else class="w-10 h-10 text-[#3d5070]" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/></svg>
                  </div>
                  <div class="flex-1">
                    <div class="flex items-center gap-3">
                      <h2 class="text-gray-100 text-xl font-bold">{{ userStore.sellerStore?.name }}</h2>
                      <span v-if="userStore.sellerStore?.status === 'ACTIVE'" class="px-2 py-0.5 bg-emerald-500/20 text-emerald-400 text-[10px] font-bold rounded uppercase tracking-wide">Активний</span>
                      <span v-else-if="userStore.sellerStore?.status === 'PENDING'" class="px-2 py-0.5 bg-yellow-500/20 text-yellow-400 text-[10px] font-bold rounded uppercase tracking-wide">Очікує</span>
                      <span v-else-if="userStore.sellerStore?.status === 'BLOCKED'" class="px-2 py-0.5 bg-red-500/20 text-red-400 text-[10px] font-bold rounded uppercase tracking-wide">Заблоковано</span>
                      <span v-else-if="userStore.sellerStore?.status === 'CLOSED'" class="px-2 py-0.5 bg-gray-500/20 text-gray-400 text-[10px] font-bold rounded uppercase tracking-wide">Закрито</span>
                    </div>
                    <div class="flex gap-4 mt-2">
                      <span v-if="userStore.sellerStore?.address" class="text-gray-400 text-xs">📍 {{ userStore.sellerStore.address }}</span>
                      <span v-if="userStore.sellerStore?.contactPhone" class="text-gray-400 text-xs">📞 {{ userStore.sellerStore.contactPhone }}</span>
                    </div>
                  </div>
                  <div class="flex flex-wrap items-center gap-2">
                    <button class="px-4 py-2 border border-[#1e2535] rounded-xl text-xs text-gray-400 hover:bg-white/5 transition-all" @click="editStore">Редагувати</button>

                    <button 
                      v-if="userStore.sellerStore?.status === 'ACTIVE'" 
                      class="px-4 py-2 bg-gray-500/15 text-gray-300 rounded-xl text-xs font-semibold hover:bg-gray-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed" 
                      :disabled="processingActionId === 'store'"
                      @click="handleStoreStatus('CLOSED')"
                    >
                      Закрити магазин
                    </button>

                    <button 
                      v-if="userStore.sellerStore?.status === 'CLOSED'" 
                      class="px-4 py-2 bg-emerald-500/15 text-emerald-400 rounded-xl text-xs font-semibold hover:bg-emerald-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed" 
                      :disabled="processingActionId === 'store'"
                      @click="handleStoreStatus('ACTIVE')"
                    >
                      Активувати магазин
                    </button>

                    <button 
                      class="px-4 py-2 bg-red-500/15 text-red-400 rounded-xl text-xs font-semibold hover:bg-red-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed" 
                      :disabled="processingActionId === 'store'"
                      @click="handleDeleteStore"
                    >
                      Видалити
                    </button>
                  </div>
                </div>
              </div>

              <div class="mb-5 flex justify-between items-center">
                <span class="text-gray-100 text-lg font-bold">Мої товари ({{ productStore.myProducts.length }})</span>
                <button class="px-5 py-2.5 bg-orange-500 rounded-xl text-white font-semibold hover:bg-orange-400 disabled:opacity-50 disabled:cursor-not-allowed" :disabled="userStore.sellerStore?.status !== 'ACTIVE'" @click="editingProductId = null; isAddingProduct = true">Додати товар</button>
              </div>

              <div v-if="productStore.myProducts.length > 0" class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
                <ProductCard v-for="product in productStore.myProducts" :key="product.id" :product="product" :simple="true" :isCabinet="true" :isStoreActive="userStore.sellerStore?.status === 'ACTIVE'" :isProcessing="processingActionId === product.id" @delete="handleDeleteProduct" @change-status="handleChangeProductStatus" @edit="openEditForm" />
              </div>
              <div v-else class="text-center py-16 text-gray-500">Ви ще не додали жодного товару.</div>
            </template>
            <AddProductForm v-else :productId="editingProductId ?? undefined" @close="handleFormClose" />
          </template>
        </div>

        <CabinetStores v-else-if="activeTab === 'stores' && userStore.user?.role === 'ADMIN'" />
        <CabinetCategories v-else-if="activeTab === 'categories' && userStore.user?.role === 'ADMIN'" />
        <CabinetShippingCarriers v-else-if="activeTab === 'shipping-carriers' && userStore.user?.role === 'ADMIN'" />
        <CabinetPromoCodes v-else-if="activeTab === 'promo-codes' && userStore.user?.role === 'ADMIN'" />
        <CabinetRequisites v-else-if="activeTab === 'requisites' && hasStore" />

        <div v-else-if="activeTab === 'settings'">
          <div class="mb-6"><span class="text-gray-100 text-xl font-black font-['Unbounded']">Налаштування</span></div>
          <CabinetSettings />
        </div>

      </div>
    </main>
    <Footer />
  </div>
</template>
