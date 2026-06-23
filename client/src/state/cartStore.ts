import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiClient } from '../api/axios'
import { toast } from 'vue3-toastify'

export interface CartItem {
  productId: number
  title: string
  brand?: string
  image?: string
  price: number
  oldPrice?: number
  quantity: number
  subTotal: number
  description?: string
  merchantName?: string
  stockQuantity?: number
  merchantId?: number
  storeName?: string
}

export interface CartData {
  items: CartItem[]
  totalAmount: number
}

export interface GroupedStore {
  storeName: string
  items: CartItem[]
}

export const useCartStore = defineStore('cart', () => {
  const isLoading = ref(false)
  const cart = ref<CartData | null>(null)
  const isCartLoading = ref(false)

  // --- Selection state ---
  const selectedProductIds = ref<number[]>([])
  const selectedMerchantId = ref<number | null>(null)

  const showConflictModal = ref(false)
  const pendingConflictData = ref<{ type: 'item' | 'store', merchantId: number, data: any } | null>(null)

  function resolveConflict(confirmReset: boolean) {
    if (confirmReset && pendingConflictData.value) {
      selectedProductIds.value = []
      selectedMerchantId.value = null

      if (pendingConflictData.value.type === 'item') {
        const item = pendingConflictData.value.data as CartItem
        selectedProductIds.value.push(item.productId)
        selectedMerchantId.value = pendingConflictData.value.merchantId
      } else if (pendingConflictData.value.type === 'store') {
        const items = pendingConflictData.value.data as CartItem[]
        const storeProductIds = items.map(i => i.productId)
        for (const id of storeProductIds) {
          selectedProductIds.value.push(id)
        }
        selectedMerchantId.value = pendingConflictData.value.merchantId
      }
    }
    showConflictModal.value = false
    pendingConflictData.value = null
  }

  // --- Computed: group items by merchantId ---
  const groupedItems = computed<Record<number, GroupedStore>>(() => {
    if (!cart.value?.items?.length) return {}

    const groups: Record<number, GroupedStore> = {}

    for (const item of cart.value.items) {
      const mId = item.merchantId ?? 0
      const name = item.storeName ?? item.merchantName ?? 'Невідомий магазин'

      if (!groups[mId]) {
        groups[mId] = { storeName: name, items: [] }
      }
      groups[mId].items.push(item)
    }

    return groups
  })

  // --- Computed: total only for selected items ---
  const selectedTotalAmount = computed<number>(() => {
    if (!cart.value?.items?.length || !selectedProductIds.value.length) return 0

    return cart.value.items
      .filter(item => selectedProductIds.value.includes(item.productId))
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
  })

  // --- Toggle single item selection ---
  function toggleItemSelection(item: CartItem) {
    const itemMerchantId = item.merchantId ?? 0
    const isAlreadySelected = selectedProductIds.value.includes(item.productId)

    if (isAlreadySelected) {
      // Remove from selection
      selectedProductIds.value = selectedProductIds.value.filter(id => id !== item.productId)

      // If no items left, clear the merchant lock
      if (selectedProductIds.value.length === 0) {
        selectedMerchantId.value = null
      }
      return
    }

    // Adding — check merchant conflict
    if (
      selectedMerchantId.value !== null &&
      selectedMerchantId.value !== itemMerchantId &&
      selectedProductIds.value.length > 0
    ) {
      pendingConflictData.value = { type: 'item', merchantId: itemMerchantId, data: item }
      showConflictModal.value = true
      return
    }

    selectedProductIds.value.push(item.productId)
    selectedMerchantId.value = itemMerchantId
  }

  // --- Toggle all items in a store group ---
  function toggleStoreSelection(merchantId: number, items: CartItem[]) {
    const storeProductIds = items.map(i => i.productId)
    const allSelected = storeProductIds.every(id => selectedProductIds.value.includes(id))

    if (allSelected) {
      // Deselect all items in this store
      selectedProductIds.value = selectedProductIds.value.filter(id => !storeProductIds.includes(id))

      if (selectedProductIds.value.length === 0) {
        selectedMerchantId.value = null
      }
      return
    }

    // Selecting — check merchant conflict
    if (
      selectedMerchantId.value !== null &&
      selectedMerchantId.value !== merchantId &&
      selectedProductIds.value.length > 0
    ) {
      pendingConflictData.value = { type: 'store', merchantId, data: items }
      showConflictModal.value = true
      return
    }

    // Add missing product IDs
    for (const id of storeProductIds) {
      if (!selectedProductIds.value.includes(id)) {
        selectedProductIds.value.push(id)
      }
    }
    selectedMerchantId.value = merchantId
  }

  // --- Cleanup: remove stale product IDs from selection ---
  function cleanupSelection() {
    if (!cart.value?.items?.length) {
      selectedProductIds.value = []
      selectedMerchantId.value = null
      return
    }

    const existingIds = new Set(cart.value.items.map(i => i.productId))
    selectedProductIds.value = selectedProductIds.value.filter(id => existingIds.has(id))

    if (selectedProductIds.value.length === 0) {
      selectedMerchantId.value = null
    }
  }

  async function fetchCart() {
    isCartLoading.value = true
    try {
      const response = await apiClient.get<CartData>('/cart')
      cart.value = response.data
      cleanupSelection()
    } catch (error) {
      cart.value = null
    } finally {
      isCartLoading.value = false
    }
  }

  async function addToCart(productId: number): Promise<boolean> {
    isLoading.value = true
    try {
      await apiClient.post('/cart', { productId, quantity: 1 })
      await fetchCart()
      return true
    } catch (error: any) {
      console.error('Помилка при додаванні в кошик:', error)
      toast.error(error.response?.data?.message || 'Помилка при додаванні в кошик')
      return false
    } finally {
      isLoading.value = false
    }
  }

  async function updateQuantity(productId: number, newQuantity: number): Promise<boolean> {
    try {
      await apiClient.patch(`/cart/${productId}/quantity`, { new_quantity: newQuantity })
      return true
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Помилка при зміні кількості')
      return false
    }
  }

  async function removeItem(productId: number): Promise<boolean> {
    try {
      await apiClient.delete(`/cart/${productId}`)
      await fetchCart()
      return true
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Помилка видалення')
      return false
    }
  }

  async function clearCart(): Promise<boolean> {
    try {
      await apiClient.delete('/cart')
      cart.value = null
      selectedProductIds.value = []
      selectedMerchantId.value = null
      return true
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Помилка очищення')
      return false
    }
  }

  return {
    isLoading,
    cart,
    isCartLoading,
    selectedProductIds,
    selectedMerchantId,
    groupedItems,
    selectedTotalAmount,
    fetchCart,
    addToCart,
    updateQuantity,
    removeItem,
    clearCart,
    toggleItemSelection,
    toggleStoreSelection,
    showConflictModal,
    pendingConflictData,
    resolveConflict,
  }
})
