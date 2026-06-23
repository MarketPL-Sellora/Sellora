import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useConfirmStore = defineStore('confirm', () => {
  const isOpen = ref(false)
  const title = ref('')
  const message = ref('')
  let resolvePromise: ((value: boolean) => void) | null = null

  const ask = (t: string, m: string): Promise<boolean> => {
    title.value = t
    message.value = m
    isOpen.value = true
    return new Promise((resolve) => {
      resolvePromise = resolve
    })
  }

  const confirm = () => {
    isOpen.value = false
    if (resolvePromise) resolvePromise(true)
  }

  const cancel = () => {
    isOpen.value = false
    if (resolvePromise) resolvePromise(false)
  }

  return { isOpen, title, message, ask, confirm, cancel }
})
