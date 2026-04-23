// src/state/productStore.ts
// Стор для товарів: завантаження з бекенду, стан фільтрів, пагінація

import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { apiClient } from '../api/axios.ts'

// ─── Тип відповіді бекенду (за Swagger Влада) ────────────────────────────────

export interface ProductApiItem {
  id: number
  title: string
  groupPrice: number        // ціна зі знижкою для групи
  standardPrice: number     // базова (перекреслена) ціна
  groupTargetSize: number   // скільки людей потрібно
  groupCurrentSize?: number // скільки вже приєдналось (якщо є)
  images: string[] | null   // масив URL зображень або null
  categoryId?: number
  description?: string
}

// ─── Тип фільтрів — відповідає query-параметрам GET /api/v1/products ─────────

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
  const totalElements = ref<number>(0)
  const isLoading     = ref<boolean>(false)
  const error         = ref<string | null>(null)

  // Активні фільтри: зберігаємо тут, щоб Sidebar міг їх оновлювати,
  // а ProductGrid автоматично реагував через watch у батьківському компоненті
  const filters = reactive<ProductFilters>({
    page: 0,
    size: 24,
  })

  // --- Дії --------------------------------------------------------------------

  /**
   * Завантажує товари з бекенду.
   * Приймає часткові параметри — решта береться з поточного стану `filters`.
   */
  async function fetchProducts(params: Partial<ProductFilters> = {}) {
    // Об'єднуємо поточні фільтри з новими параметрами
    Object.assign(filters, params)

    isLoading.value = true
    error.value     = null

    try {
      // Формуємо query-параметри: прибираємо undefined, щоб не засмічувати URL
      const query: Record<string, string | number> = {
        page: filters.page,
        size: filters.size,
      }
      if (filters.categoryId !== undefined) query.categoryId = filters.categoryId
      if (filters.keyword)                  query.keyword    = filters.keyword
      if (filters.minPrice !== undefined)   query.minPrice   = filters.minPrice
      if (filters.maxPrice !== undefined)   query.maxPrice   = filters.maxPrice

      const response = await apiClient.get('/products', { params: query })

      // Spring Page: дані в response.data.content, загальна кількість в totalElements
      products.value      = response.data.content      ?? []
      totalElements.value = response.data.totalElements ?? 0

    } catch (err: unknown) {
      // Фіксуємо помилку у стані, щоб ProductGrid міг її показати
      if (err instanceof Error) {
        error.value = err.message
      } else {
        error.value = 'Невідома помилка при завантаженні товарів'
      }
      console.error('[productStore] fetchProducts error:', err)
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Скидає фільтри до початкового стану та перезавантажує першу сторінку
   */
  function resetFilters() {
    filters.categoryId = undefined
    filters.keyword    = undefined
    filters.minPrice   = undefined
    filters.maxPrice   = undefined
    filters.page       = 0
    fetchProducts()
  }

  return {
    // стан
    products,
    totalElements,
    isLoading,
    error,
    filters,
    // дії
    fetchProducts,
    resetFilters,
  }
})
