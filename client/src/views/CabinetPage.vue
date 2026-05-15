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
import { useUserStore } from '../state/userStore'
import { useProductStore } from '../state/productStore'

const userStore = useUserStore()
const productStore = useProductStore()

const route = useRoute()
const router = useRouter()

const activeTab = computed({
  get: () => (route.query.tab as string) || 'orders',
  set: (tab) => router.replace({ query: { ...route.query, tab } })
})
const isAddingProduct = ref(false)
const isCreatingStoreForm = ref(false)
const editingProductId = ref<number | null>(null)

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

// Mock-дані для замовлень та обраного
const mockOrders = ref([
  { id: 101, brand: 'Apple', name: 'MacBook Air M3 13"', image: 'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/mba13-midnight-select-202402?wid=904&hei=840&fmt=jpeg&qlt=90', rating: 5, reviewCount: 214, groupCurrent: 3, groupTotal: 3, price: 52999, oldPrice: 59999 },
  { id: 102, brand: 'Sony', name: 'WH-1000XM5 Headphones', image: 'https://www.bhphotovideo.com/images/images2500x2500/sony_wh1000xm5_b_wh_1000xm5_wireless_noise_canceling_over_ear_1668478.jpg', rating: 4, reviewCount: 98, groupCurrent: 2, groupTotal: 4, price: 9499, oldPrice: 11999 }
])

const mockFavorites = ref([
  { id: 201, brand: 'Logitech', name: 'MX Master 3S', image: 'https://resource.logitech.com/w_386,c_limit,q_auto,f_auto,dpr_1.0/d_transparent.gif/content/dam/logitech/en/products/mice/mx-master-3s/gallery/mx-master-3s-mouse-top-view-graphite.png', rating: 5, reviewCount: 341, groupCurrent: 2, groupTotal: 3, price: 3299, oldPrice: 4199 }
])

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
  if (!confirm('Ви дійсно хочете видалити цей товар?')) return
  const success = await productStore.deleteProduct(id)
  if (success && userStore.sellerStore?.id) {
    await productStore.fetchMerchantProducts(userStore.sellerStore.id)
  }
}

async function handleChangeProductStatus(payload: { id: number; status: string }) {
  const success = await productStore.changeProductStatus(payload.id, payload.status)
  if (success && userStore.sellerStore?.id) {
    await productStore.fetchMerchantProducts(userStore.sellerStore.id)
  }
}

function openEditForm(id: number) {
  editingProductId.value = id
  isAddingProduct.value = true
}

async function handleDeleteStore() {
  if (!userStore.sellerStore?.id) return
  if (!confirm('Ви дійсно хочете видалити свій магазин? Цю дію неможливо скасувати.')) return

  await userStore.deleteStore(userStore.sellerStore.id)
}

async function handleStoreStatus(newStatus: string) {
  if (!userStore.sellerStore?.id) return
  const actionText = newStatus === 'CLOSED' ? 'закрити' : 'активувати'
  if (!confirm(`Ви дійсно хочете ${actionText} свій магазин?`)) return

  const success = await userStore.changeStoreStatus(userStore.sellerStore.id, newStatus)
  if (success) {
    await userStore.fetchUserStore(userStore.user!.id) // Оновлюємо дані магазину
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

        <div v-if="activeTab === 'orders'">
          <div class="mb-6"><span class="text-gray-100 text-xl font-black font-['Unbounded']">Мої замовлення</span></div>
          <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
            <ProductCard v-for="item in mockOrders" :key="item.id" :product="(item as any)" :simple="true" />
          </div>
        </div>

        <div v-else-if="activeTab === 'wishlist'">
          <div class="mb-6"><span class="text-gray-100 text-xl font-black font-['Unbounded']">Обране</span></div>
          <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
            <ProductCard v-for="item in mockFavorites" :key="item.id" :product="(item as any)" :simple="true" />
          </div>
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
                      class="px-4 py-2 bg-gray-500/15 text-gray-300 rounded-xl text-xs font-semibold hover:bg-gray-500/25 transition-all" 
                      @click="handleStoreStatus('CLOSED')"
                    >
                      Закрити магазин
                    </button>

                    <button 
                      v-if="userStore.sellerStore?.status === 'CLOSED'" 
                      class="px-4 py-2 bg-emerald-500/15 text-emerald-400 rounded-xl text-xs font-semibold hover:bg-emerald-500/25 transition-all" 
                      @click="handleStoreStatus('ACTIVE')"
                    >
                      Активувати магазин
                    </button>

                    <button 
                      class="px-4 py-2 bg-red-500/15 text-red-400 rounded-xl text-xs font-semibold hover:bg-red-500/25 transition-all" 
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
                <ProductCard v-for="product in productStore.myProducts" :key="product.id" :product="product" :simple="true" :isCabinet="true" :isStoreActive="userStore.sellerStore?.status === 'ACTIVE'" @delete="handleDeleteProduct" @change-status="handleChangeProductStatus" @edit="openEditForm" />
              </div>
              <div v-else class="text-center py-16 text-gray-500">Ви ще не додали жодного товару.</div>
            </template>
            <AddProductForm v-else :productId="editingProductId ?? undefined" @close="handleFormClose" />
          </template>
        </div>

        <CabinetStores v-else-if="activeTab === 'stores' && userStore.user?.role === 'ADMIN'" />
        <CabinetCategories v-else-if="activeTab === 'categories' && userStore.user?.role === 'ADMIN'" />

        <div v-else-if="activeTab === 'settings'">
          <div class="p-6 bg-gray-900 rounded-2xl">
            <div class="flex items-center gap-4">
              <div class="w-16 h-16 bg-orange-500 rounded-full flex justify-center items-center text-white text-xl font-bold">
                {{ (userStore.user?.email?.[0] || 'M').toUpperCase() }}
              </div>
              <div>
                <div class="text-white font-bold">{{ userStore.user?.role }}</div>
                <div class="text-gray-500 text-sm">{{ userStore.user?.email }}</div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </main>
    <Footer />
  </div>
</template>
