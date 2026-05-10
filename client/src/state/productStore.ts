import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { apiClient } from '../api/axios.ts'

// ─── Тип відповіді бекенду ────────────────────────────────────────────────────

export interface ProductApiItem {
  id: number
  title: string
  groupPrice: number
  standardPrice: number
  groupTargetSize: number
  groupCurrentSize?: number
  images: string[] | null
  categoryId?: number
  description?: string
  stockQuantity?: number
  merchantId?: number
  attributes?: Record<string, string>
}

// ─── Тип фільтрів ─────────────────────────────────────────────────────────────

export interface ProductFilters {
  categoryId?: number
  keyword?: string
  minPrice?: number
  maxPrice?: number
  page: number
  size: number
}

// ─── Стор ────────────────────────────────────────────────────────────────────

export const useProductStore = defineStore('products', () => {

  // --- Стан -------------------------------------------------------------------

  const products      = ref<ProductApiItem[]>([])
  const myProducts    = ref<ProductApiItem[]>([])   // NEW: товари конкретного мерчанта
  const totalElements = ref<number>(0)
  const isLoading     = ref<boolean>(false)
  const error         = ref<string | null>(null)

  const filters = reactive<ProductFilters>({
    page: 0,
    size: 24,
  })

  // --- Дії --------------------------------------------------------------------

  async function fetchProducts(params: Partial<ProductFilters> = {}) {
    Object.assign(filters, params)

    isLoading.value = true
    error.value     = null

    try {
      const query: Record<string, string | number> = {
        page: filters.page,
        size: filters.size,
      }
      if (filters.categoryId !== undefined) query.categoryId = filters.categoryId
      if (filters.keyword)                  query.keyword    = filters.keyword
      if (filters.minPrice !== undefined)   query.minPrice   = filters.minPrice
      if (filters.maxPrice !== undefined)   query.maxPrice   = filters.maxPrice

      const response = await apiClient.get('/products', { params: query })

      products.value      = response.data.content      ?? []
      totalElements.value = response.data.totalElements ?? 0

    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Невідома помилка при завантаженні товарів'
      console.error('[productStore] fetchProducts error:', err)
    } finally {
      isLoading.value = false
    }
  }

  // ─── NEW: завантажити одне зображення, повернути URL ─────────────────────────
  async function uploadImage(file: File): Promise<string> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post(
      '/upload/image',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )

    const data = response.data

    // Бекенд може повернути: просто рядок, об'єкт { url }, або { additionalProp1: "..." }
    if (typeof data === 'string') return data
    if (data && typeof data === 'object') {
      if (typeof data.url === 'string') return data.url
      // Дістаємо перше рядкове значення з об'єкта
      const firstString = Object.values(data).find(v => typeof v === 'string')
      if (firstString) return firstString as string
    }
    throw new Error('Не вдалося отримати URL зображення від сервера')
  }

  // ─── NEW: створити товар ──────────────────────────────────────────────────────
  async function createProduct(payload: Omit<ProductApiItem, 'id'>): Promise<boolean> {
    try {
      await apiClient.post('/products', payload)
      return true
    } catch (err: unknown) {
      console.error('[productStore] createProduct error:', err)
      return false
    }
  }

  // ─── NEW: завантажити товари конкретного мерчанта ─────────────────────────────
  async function fetchMerchantProducts(merchantId: number): Promise<void> {
    isLoading.value = true
    error.value     = null
    try {
      const response  = await apiClient.get(`/products/merchant/${merchantId}`)
      myProducts.value = response.data.content ?? response.data ?? []
    } catch (err: unknown) {
      error.value      = err instanceof Error ? err.message : 'Помилка завантаження товарів магазину'
      myProducts.value = []
      console.error('[productStore] fetchMerchantProducts error:', err)
    } finally {
      isLoading.value = false
    }
  }

  function resetFilters() {
    filters.categoryId = undefined
    filters.keyword    = undefined
    filters.minPrice   = undefined
    filters.maxPrice   = undefined
    filters.page       = 0
    fetchProducts()
  }

  return {
    products,
    myProducts,
    totalElements,
    isLoading,
    error,
    filters,
    fetchProducts,
    resetFilters,
    uploadImage,
    createProduct,
    fetchMerchantProducts,
  }
})
