<script setup lang="ts">
import { ref } from 'vue'
import { apiClient } from '../../api/axios'

const props = withDefaults(
  defineProps<{
    productId: number
    initialRating?: number
    initialComment?: string | null
    isEditing?: boolean
  }>(),
  {
    initialRating: 0,
    initialComment: null,
    isEditing: false,
  },
)

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success'): void
}>()

const rating = ref(props.initialRating)
const comment = ref(props.initialComment ?? '')
const hoveredRating = ref(0)
const isSubmitting = ref(false)
const errorMsg = ref<string | null>(null)

async function submitReview() {
  if (rating.value === 0) {
    errorMsg.value = 'Будь ласка, оберіть оцінку'
    return
  }

  isSubmitting.value = true
  errorMsg.value = null

  try {
    const payload = {
      rating: rating.value,
      comment: comment.value || null,
    }

    if (props.isEditing) {
      await apiClient.put(`/reviews/${props.productId}`, payload)
    } else {
      await apiClient.post(`/reviews/${props.productId}`, payload)
    }

    emit('success')
  } catch (e: any) {
    errorMsg.value =
      e?.response?.data?.message ||
      e?.response?.data?.error ||
      'Не вдалося надіслати відгук. Спробуйте пізніше.'
    console.error('Помилка надсилання відгуку', e)
  } finally {
    isSubmitting.value = false
  }
}

function displayedRating(star: number): boolean {
  return star <= (hoveredRating.value || rating.value)
}
</script>

<template>
  <!-- Backdrop -->
  <Teleport to="body">
    <div
      class="fixed inset-0 z-[100] flex items-center justify-center"
      @click.self="emit('close')"
    >
      <!-- Overlay -->
      <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="emit('close')" />

      <!-- Modal -->
      <div
        class="relative z-10 w-full max-w-lg mx-4 bg-[#131720] rounded-2xl outline outline-1 outline-[#1c1f2a] shadow-[0_24px_80px_rgba(0,0,0,0.6)] flex flex-col overflow-hidden animate-modal-in"
      >
        <!-- Header -->
        <div class="flex items-center justify-between px-6 pt-6 pb-4">
          <h2 class="text-white text-lg font-bold font-['Unbounded'] tracking-wide">
            {{ isEditing ? 'Редагувати відгук' : 'Написати відгук' }}
          </h2>
          <button
            class="w-8 h-8 flex items-center justify-center rounded-lg text-gray-500 hover:text-white hover:bg-white/10 transition-colors"
            @click="emit('close')"
          >
            <svg class="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                clip-rule="evenodd"
              />
            </svg>
          </button>
        </div>

        <!-- Body -->
        <div class="px-6 pb-6 flex flex-col gap-5">
          <!-- Star rating -->
          <div class="flex flex-col gap-2">
            <label class="text-slate-400 text-sm font-semibold font-['Onest']">Ваша оцінка</label>
            <div
              class="flex gap-1.5"
              @mouseleave="hoveredRating = 0"
            >
              <button
                v-for="star in 5"
                :key="star"
                type="button"
                class="w-9 h-9 flex items-center justify-center rounded-lg transition-all duration-150 hover:scale-110 active:scale-95"
                :class="displayedRating(star) ? 'bg-yellow-400/10' : 'bg-white/5'"
                @mouseenter="hoveredRating = star"
                @click="rating = star"
              >
                <svg
                  class="w-5 h-5 transition-colors duration-150"
                  :class="displayedRating(star) ? 'text-yellow-400' : 'text-[#3d4158]'"
                  viewBox="0 0 16 16"
                  fill="currentColor"
                >
                  <path d="M8 1l1.8 3.6L14 5.3l-3 2.9.7 4.1L8 10.4l-3.7 1.9.7-4.1-3-2.9 4.2-.7z" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Comment -->
          <div class="flex flex-col gap-2">
            <label class="text-slate-400 text-sm font-semibold font-['Onest']">
              Коментар <span class="text-slate-600 font-normal">(необов'язково)</span>
            </label>
            <textarea
              v-model="comment"
              rows="4"
              placeholder="Розкажіть про свій досвід використання товару..."
              class="w-full px-4 py-3 bg-[#0f1117] text-white text-sm font-['Onest'] placeholder-slate-600 rounded-xl outline outline-1 outline-[#1c1f2a] focus:outline-orange-500/50 transition-colors resize-none"
            />
          </div>

          <!-- Error -->
          <p v-if="errorMsg" class="text-red-400 text-sm font-['Onest']">
            {{ errorMsg }}
          </p>

          <!-- Actions -->
          <div class="flex justify-end gap-3 pt-1">
            <button
              type="button"
              class="px-5 py-2.5 rounded-xl text-slate-400 text-sm font-semibold font-['Onest'] hover:text-white hover:bg-white/5 transition-colors"
              @click="emit('close')"
            >
              Скасувати
            </button>
            <button
              type="button"
              class="px-5 py-2.5 bg-gradient-to-r from-orange-500 to-amber-500 rounded-xl text-white text-sm font-bold font-['Onest'] tracking-wide transition-all duration-200 hover:from-orange-400 hover:to-amber-400 hover:shadow-[0px_4px_20px_rgba(249,115,22,0.4)] active:scale-[0.97] disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="isSubmitting"
              @click="submitReview"
            >
              {{ isSubmitting ? 'Надсилаємо...' : isEditing ? 'Зберегти зміни' : 'Опублікувати' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
@keyframes modal-in {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
.animate-modal-in {
  animation: modal-in 0.25s ease-out;
}
</style>
