import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiClient } from '../api/axios'

export interface CartItem {
  productId: number
  name: string
  brand?: string
  image?: string
  price: number
  oldPrice?: number
  quantity: number
  totalPrice: number
  merchantName?: string
  stockQuantity?: number
}

export interface CartData {
  items: CartItem[]
  totalAmount: number
}

export const useCartStore = defineStore('cart', () => {
  const isLoading = ref(false)
  const cart = ref<CartData | null>(null)
  const isCartLoading = ref(false)

  async function fetchCart() {
    isCartLoading.value = true
    try {
      const response = await apiClient.get<CartData>('/cart')
      cart.value = response.data
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
      alert(error.response?.data?.message || 'Помилка при додаванні в кошик')
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
      alert(error.response?.data?.message || 'Помилка при зміні кількості')
      return false
    }
  }

  async function removeItem(productId: number): Promise<boolean> {
    try {
      await apiClient.delete(`/cart/${productId}`)
      await fetchCart()
      return true
    } catch (error: any) {
      alert(error.response?.data?.message || 'Помилка видалення')
      return false
    }
  }

  async function clearCart(): Promise<boolean> {
    try {
      await apiClient.delete('/cart')
      cart.value = null
      return true
    } catch (error: any) {
      alert(error.response?.data?.message || 'Помилка очищення')
      return false
    }
  }

  return {
    isLoading,
    cart,
    isCartLoading,
    fetchCart,
    addToCart,
    updateQuantity,
    removeItem,
    clearCart,
  }
})
