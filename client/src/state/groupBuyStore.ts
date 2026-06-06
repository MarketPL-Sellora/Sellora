import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiClient } from '../api/axios'

export interface GroupBuySession {
  uuid: string
  productId: number
  productTitle: string
  productImage: string
  price: number
  targetSize: number
  currentMembersCount: number
  status: string
  expiresAt: string
  isAvailable: boolean
}

export interface GroupBuyCheckoutPayload {
  buyer_name: string
  buyer_surname: string
  buyer_phone: string
  buyer_email: string
  delivery_type: 'BRANCH' | 'COURIER' | 'PICKUP'
  carrier_id: number | null
  delivery_address: Record<string, string> | null
  payment_method: 'ONLINE_CARD' | 'CASH_ON_DELIVERY'
  order_comment: string | null
  promo_code: null
  product_id: number
  quantity: number
}

export interface GroupBuyOrderResponse {
  id?: number
  payment_url?: string
  [key: string]: unknown
}

export const useGroupBuyStore = defineStore('groupBuy', () => {
  const session    = ref<GroupBuySession | null>(null)
  const mySessions = ref<GroupBuySession[]>([])
  const isLoading  = ref(false)
  const error      = ref<string | null>(null)

  // ─── Перевірка участі через серверний масив mySessions ──────────────────────

  function isJoined(uuid: string): boolean {
    return mySessions.value.some(s => s.uuid === uuid)
  }

  async function fetchSession(uuid: string): Promise<GroupBuySession | null> {
    isLoading.value = true
    error.value     = null
    try {
      const response = await apiClient.get<GroupBuySession>(`/group-buy/sessions/${uuid}`)
      session.value  = response.data
      return response.data
    } catch (err: unknown) {
      error.value   = err instanceof Error ? err.message : 'Помилка завантаження сесії'
      session.value = null
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function createSession(payload: GroupBuyCheckoutPayload): Promise<GroupBuyOrderResponse | null> {
    isLoading.value = true
    error.value     = null
    try {
      const response = await apiClient.post<GroupBuyOrderResponse>('/group-buy/sessions', payload)

      // Оновлюємо масив mySessions з бекенду
      await fetchMySessions()

      return response.data
    } catch (err: any) {
      error.value   = err.response?.data?.message || (err instanceof Error ? err.message : 'Помилка створення сесії')
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function joinSession(uuid: string, payload: GroupBuyCheckoutPayload): Promise<GroupBuyOrderResponse | null> {
    isLoading.value = true
    error.value     = null
    try {
      const response = await apiClient.post<GroupBuyOrderResponse>(`/group-buy/sessions/${uuid}/join`, payload)

      // Оновлюємо масив mySessions з бекенду
      await fetchMySessions()

      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || (err instanceof Error ? err.message : 'Не вдалося приєднатися')
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function fetchMySessions(): Promise<void> {
    isLoading.value = true
    error.value     = null
    try {
      const response = await apiClient.get<GroupBuySession[]>('/group-buy/sessions/user')
      mySessions.value = response.data
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Помилка завантаження сесій'
      mySessions.value = []
    } finally {
      isLoading.value = false
    }
  }

  function clearSession() {
    session.value = null
    error.value   = null
  }

  return {
    session, mySessions, isLoading, error,
    fetchSession, createSession, joinSession, clearSession,
    fetchMySessions, isJoined,
  }
})
