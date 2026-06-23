import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { apiClient } from '../api/axios.ts'
import { toast } from 'vue3-toastify'


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
  isFavorite?: boolean
}


export interface ProductFilters {
  sortBy?: string
  sortDir?: string
  categoryId?: number
  keyword?: string
  minPrice?: number
  maxPrice?: number
  status?: string
  groupMode?: string
  storeId?: number
  onlyFavorites?: boolean
  page: number
  size: number
}


export const useProductStore = defineStore('products', () => {


  const products = ref<ProductApiItem[]>([])
  const myProducts = ref<ProductApiItem[]>([])
  const currentProduct = ref<ProductApiItem | null>(null)
  const totalElements = ref<number>(0)
  const favoritesCount = ref<number>(0)
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  const filters = reactive<ProductFilters>({
    page: 0,
    size: 24,
  })


  async function fetchProducts(params: Partial<ProductFilters> = {}) {
    Object.assign(filters, params)

    isLoading.value = true
    error.value = null

    try {
      const query: Record<string, string | number | boolean> = {
        page: filters.page,
        size: filters.size,
      }
      if (filters.categoryId !== undefined) query.categoryId = filters.categoryId
      if (filters.keyword) query.keyword = filters.keyword
      if (filters.minPrice !== undefined) query.minPrice = filters.minPrice
      if (filters.maxPrice !== undefined) query.maxPrice = filters.maxPrice
      if (filters.status) query.status = filters.status
      if (filters.groupMode) query.groupMode = filters.groupMode
      if (filters.storeId !== undefined) query.storeId = filters.storeId
      if (filters.onlyFavorites !== undefined) query.onlyFavorites = filters.onlyFavorites
      if (filters.sortBy) query.sortBy = filters.sortBy
      if (filters.sortDir) query.sortDir = filters.sortDir

      const response = await apiClient.get('/products', { params: query })

      const newProducts = response.data.content ?? [];
      if (filters.page === 0) {
        products.value = newProducts;
      } else {
        products.value = [...products.value, ...newProducts];
      }
      totalElements.value = response.data.totalElements ?? 0;

    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Невідома помилка при завантаженні товарів'
      console.error('[productStore] fetchProducts error:', err)
    } finally {
      isLoading.value = false
    }
  }

  async function uploadImage(file: File): Promise<string> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post(
      '/uploads/image',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )

    const data = response.data

    if (typeof data === 'string') return data
    if (data && typeof data === 'object') {
      if (typeof data.url === 'string') return data.url
      const firstString = Object.values(data).find(v => typeof v === 'string')
      if (firstString) return firstString as string
    }
    throw new Error('Не вдалося отримати URL зображення від сервера')
  }

  async function createProduct(payload: Omit<ProductApiItem, 'id'>): Promise<boolean> {
    try {
      await apiClient.post('/products', payload)
      return true
    } catch (err: unknown) {
      console.error('[productStore] createProduct error:', err)
      return false
    }
  }

  async function fetchMerchantProducts(merchantId: number): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      const response = await apiClient.get(`/products/merchant/${merchantId}`)
      myProducts.value = response.data.content ?? response.data ?? []
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Помилка завантаження товарів магазину'
      myProducts.value = []
      console.error('[productStore] fetchMerchantProducts error:', err)
    } finally {
      isLoading.value = false
    }
  }


  async function fetchProductById(id: number): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      const response = await apiClient.get('/products/' + id, { params: { full: true } })
      currentProduct.value = response.data
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Помилка завантаження товару'
      currentProduct.value = null
      console.error('[productStore] fetchProductById error:', err)
    } finally {
      isLoading.value = false
    }
  }


  async function deleteProduct(id: number): Promise<boolean> {
    try {
      await apiClient.delete('/products/' + id)
      return true
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Помилка видалення товару')
      return false
    }
  }


  async function changeProductStatus(id: number, status: string): Promise<boolean> {
    try {
      await apiClient.patch('/products/' + id + '/status', { status })
      return true
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Помилка зміни статусу')
      return false
    }
  }


  async function updateProduct(id: number, payload: Omit<ProductApiItem, 'id'>): Promise<boolean> {
    try {
      await apiClient.put('/products/' + id, payload)
      return true
    } catch (err: unknown) {
      console.error('[productStore] updateProduct error:', err)
      return false
    }
  }

  function resetFilters() {
    filters.categoryId = undefined
    filters.keyword = undefined
    filters.minPrice = undefined
    filters.maxPrice = undefined
    filters.groupMode = undefined
    filters.storeId = undefined
    filters.onlyFavorites = undefined
    filters.sortBy = undefined
    filters.sortDir = undefined
    filters.page = 0
    fetchProducts()
  }


  async function fetchFavoritesCount() {
    try {
      const response = await apiClient.get('/products', { params: { onlyFavorites: true, size: 1 } })
      favoritesCount.value = response.data.totalElements ?? 0
    } catch (err) {
      console.error('[productStore] fetchFavoritesCount error:', err)
    }
  }

  async function addToFavorites(productId: number): Promise<void> {
    await apiClient.post(`/favorites/${productId}`)
    favoritesCount.value++
  }

  async function removeFromFavorites(productId: number): Promise<void> {
    await apiClient.delete(`/favorites/${productId}`)
    favoritesCount.value = Math.max(0, favoritesCount.value - 1)
  }

  return {
    products,
    myProducts,
    currentProduct,
    totalElements,
    favoritesCount,
    isLoading,
    error,
    filters,
    fetchProducts,
    fetchProductById,
    resetFilters,
    uploadImage,
    createProduct,
    fetchMerchantProducts,
    deleteProduct,
    changeProductStatus,
    updateProduct,
    fetchFavoritesCount,
    addToFavorites,
    removeFromFavorites,
  }
})
