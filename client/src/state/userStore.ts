import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

// ─── Ключ для localStorage ────────────────────────────────────────────────────
const STORAGE_KEY = 'sellora_user'

// ─── Тип даних користувача ────────────────────────────────────────────────────
interface UserData {
  email: string
}

// ─── Допоміжна функція: зчитуємо збережений стан при старті ──────────────────
function loadFromStorage(): { user: UserData | null; isAuthenticated: boolean } {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { user: null, isAuthenticated: false }
    const parsed = JSON.parse(raw)
    return {
      user:            parsed.user            ?? null,
      isAuthenticated: parsed.isAuthenticated ?? false,
    }
  } catch {
    // У разі пошкодженого JSON — повертаємо порожній стан
    return { user: null, isAuthenticated: false }
  }
}

// ─── Pinia Store: userStore ───────────────────────────────────────────────────
export const useUserStore = defineStore('user', () => {

  // ─── Початковий стан із localStorage ─────────────────────────────────────
  const saved = loadFromStorage()

  const user            = ref<UserData | null>(saved.user)
  const isAuthenticated = ref<boolean>(saved.isAuthenticated)

  // ─── Автоматичне збереження стану при будь-якій зміні ────────────────────
  // watch() стежить за user та isAuthenticated і одразу синхронізує localStorage
  watch(
    [user, isAuthenticated],
    ([newUser, newAuth]) => {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ user: newUser, isAuthenticated: newAuth }),
      )
    },
    { deep: true },
  )

  // ─── Дія: зберегти дані після входу або реєстрації ───────────────────────
  function login(userData: UserData) {
    user.value            = userData
    isAuthenticated.value = true
  }

  // ─── Дія: вийти з акаунту ─────────────────────────────────────────────────
  function logout() {
    user.value            = null
    isAuthenticated.value = false
    // Видаляємо дані з localStorage повністю
    localStorage.removeItem(STORAGE_KEY)
  }

  return { user, isAuthenticated, login, logout }
})
