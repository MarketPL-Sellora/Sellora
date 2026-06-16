<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import dayjs from 'dayjs'
import { apiClient } from '../../api/axios'
import { useUserStore } from '../../state/userStore'
import ReviewModal from '../modals/ReviewModal.vue'

interface ReviewItem {
  id: number
  user_id: number
  email: string
  rating: number
  comment: string | null
  created_at: string
}

const props = defineProps<{
  apiProduct?: any
  description?: string
  attributes?: Record<string, string>
}>()

const userStore = useUserStore()

// ─── Types ────────────────────────────────────────────────────────────────────

interface Tab {
  id: string
  label: string
}

// ─── Tabs ─────────────────────────────────────────────────────────────────────

const tabs: Tab[] = [
  { id: 'desc',    label: 'ОПИС'           },
  { id: 'specs',   label: 'ХАРАКТЕРИСТИКИ' },
  { id: 'reviews', label: 'ВІДГУКИ'        },
]

const activeTab = ref('desc')

// ─── Description paragraphs ───────────────────────────────────────────────────

const descriptionParagraphs = computed(() => {
  const desc = props.description?.trim()
  if (!desc) return ['Опис відсутній']
  return desc.split(/\n\s*\n/).filter(p => p.trim().length > 0)
})

// ─── Product features (from attributes) ─────────────────────────────────────

const productFeatures = computed(() => {
  if (!props.attributes) return []
  return Object.entries(props.attributes).map(([key, value]) => ({ key, value }))
})

// ─── Product images ───────────────────────────────────────────────────────────

const displayImages = computed(() => {
  const imgs = props.apiProduct?.images || []
  return imgs.slice(0, 2)
})

// ─── Reviews ──────────────────────────────────────────────────────────────────

const productReviews = computed(() => (props.apiProduct?.reviews as ReviewItem[]) || [])
const reviewCount = computed(() => props.apiProduct?.reviews_count || 0)

const canReview = ref(false)
const isReviewModalOpen = ref(false)
const reviewErrorReason = ref<string | null>(null)
const editingReview = ref<any>(null)

async function checkReviewStatus() {
  if (!userStore.isAuthenticated || !props.apiProduct?.id) return
  try {
    const res = await apiClient.get(`/reviews/${props.apiProduct.id}/check`)
    canReview.value = res.data.can_review
    reviewErrorReason.value = res.data.reason
  } catch (e) {
    console.error('Помилка перевірки статусу відгуку', e)
  }
}

function formatReviewDate(iso: string) {
  return dayjs(iso).format('DD.MM.YYYY')
}

function openEditModal(review: any) {
  editingReview.value = review
  isReviewModalOpen.value = true
}

function openCreateModal() {
  editingReview.value = null
  isReviewModalOpen.value = true
}

async function deleteReview() {
  if (!props.apiProduct?.id) return
  if (!confirm('Ви впевнені, що хочете видалити свій відгук?')) return

  try {
    await apiClient.delete(`/reviews/${props.apiProduct.id}`)
    emit('review-updated')
  } catch (e: any) {
    alert(e.response?.data?.message || 'Помилка при видаленні відгуку')
  }
}

onMounted(() => {
  checkReviewStatus()
})

watch(() => props.apiProduct, () => {
  checkReviewStatus()
})

// ─── Emits ────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  (e: 'tab-change', id: string): void
  (e: 'review-updated'): void
}>()

function setTab(id: string) {
  activeTab.value = id
  emit('tab-change', id)
}
</script>

<template>
  <div class="w-full inline-flex flex-col justify-start items-start gap-8">

    <!-- ── Tab bar ────────────────────────────────────────────────────────── -->
    <div class="self-stretch border-b border-[#1c1f2a] flex overflow-x-auto scrollbar-none whitespace-nowrap">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="px-6 py-3.5 relative inline-flex justify-center items-center gap-1 text-sm font-normal font-['Onest'] leading-5 transition-colors duration-150 focus:outline-none"
        :class="
          activeTab === tab.id
            ? 'text-white'
            : 'text-[#5a5f7a] hover:text-white'
        "
        @click="setTab(tab.id)"
      >
        {{ tab.label }}
        <span
          v-if="tab.id === 'reviews'"
          class="transition-colors duration-150"
          :class="activeTab === tab.id ? 'text-orange-400' : 'text-orange-400/60'"
        >
          ({{ reviewCount }})
        </span>

        <!-- Active underline -->
        <span
          v-if="activeTab === tab.id"
          class="absolute bottom-0 left-0 right-0 h-0.5 bg-orange-500 rounded-t"
        />
      </button>
    </div>

    <!-- ── ОПИС tab ───────────────────────────────────────────────────────── -->
    <div
      v-if="activeTab === 'desc'"
      class="self-stretch inline-flex justify-center items-start gap-8 flex-wrap"
    >
      <!-- Left: text + banners -->
      <div class="flex-1 min-w-0 self-stretch inline-flex flex-col justify-start items-start gap-6">

        <!-- Description -->
        <div class="self-stretch flex flex-col gap-3">
          <h2 class="text-white text-xl font-normal font-['Unbounded'] leading-7">
            Опис товару
          </h2>
          <p
            v-for="(para, i) in descriptionParagraphs"
            :key="i"
            class="text-[#787d99] text-sm font-normal font-['Onest'] leading-6"
            :class="i > 0 ? 'pt-1' : ''"
          >
            {{ para }}
          </p>
        </div>

        <!-- Dynamic product images (ambient blur + 16:9) -->
        <div 
          v-for="(img, idx) in displayImages" 
          :key="idx"
          class="self-stretch aspect-video relative rounded-2xl overflow-hidden outline outline-1 outline-white/5"
        >
          <!-- Dark base -->
          <div class="absolute inset-0 bg-[#0d0f14]" />

          <!-- Ambient blur background -->
          <img
            :src="img"
            alt=""
            aria-hidden="true"
            class="absolute inset-0 w-full h-full object-cover scale-125 blur-[60px] opacity-25 saturate-[1.4] pointer-events-none select-none"
          />

          <!-- Vignette overlay -->
          <div
            class="absolute inset-0 pointer-events-none z-[1]"
            style="background: radial-gradient(ellipse 80% 80% at 50% 50%, transparent 40%, rgba(13,15,20,0.95) 100%);"
          />

          <!-- Centered product image -->
          <div class="absolute inset-0 z-10 flex items-center justify-center p-6">
            <img 
              :src="img" 
              :alt="'Фото товару ' + (Number(idx) + 1)" 
              class="max-w-full max-h-full object-contain select-none transition-all duration-700 hover:scale-[1.03] drop-shadow-[0_12px_32px_rgba(0,0,0,0.5)]" 
              draggable="false"
              loading="lazy" 
            />
          </div>
        </div>
      </div>

      <!-- Right: key features -->
      <div class="w-full lg:w-80 shrink-0">
        <div class="self-stretch p-5 bg-[#161820] rounded-2xl outline outline-1 outline-offset-[-1px] outline-[#1c1f2a] flex flex-col gap-4">
          <h3 class="text-white text-base font-normal font-['Onest'] leading-6">
            Ключові особливості
          </h3>
          <div class="flex flex-col gap-3">
            <div v-if="productFeatures.length === 0" class="text-[#5a5f7a] text-sm font-['Onest']">
              Характеристики не вказані
            </div>
            <div
              v-for="feat in productFeatures.slice(0, 8)"
              :key="feat.key"
              class="inline-flex justify-start items-start gap-3"
            >
              <div class="flex flex-col gap-0.5 min-w-0 py-1">
                <span class="text-[#c8ccdf] text-sm font-normal font-['Onest'] leading-5">{{ feat.key }}</span>
                <span class="text-[#5a5f7a] text-xs font-normal font-['Onest'] leading-4">{{ feat.value }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── ХАРАКТЕРИСТИКИ tab ─────────────────────────────────────────────── -->
    <div
      v-else-if="activeTab === 'specs'"
      class="self-stretch flex flex-col gap-4"
    >
      <div v-if="productFeatures.length === 0" class="text-[#5a5f7a] text-sm font-normal font-['Onest'] leading-6">
        Характеристики відсутні.
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-4">
        <div 
          v-for="feat in productFeatures" 
          :key="'spec-' + feat.key"
          class="flex flex-col sm:flex-row sm:items-end justify-between border-b border-[#1c1f2a] pb-2"
        >
          <span class="text-[#787d99] text-sm font-normal font-['Onest']">{{ feat.key }}</span>
          <span class="text-white text-sm font-medium font-['Onest'] text-right">{{ feat.value }}</span>
        </div>
      </div>
    </div>

    <!-- ── ВІДГУКИ tab ────────────────────────────────────────────────────── -->
    <div
      v-else-if="activeTab === 'reviews'"
      class="self-stretch flex flex-col gap-6"
    >

      <!-- Header with button -->
      <div class="flex justify-between items-center">
        <h3 class="text-white text-lg font-bold font-['Unbounded'] tracking-wide">
          Відгуки покупців ({{ reviewCount }})
        </h3>
        <button
          v-if="canReview"
          @click="openCreateModal"
          class="px-4 py-2 bg-orange-500/10 text-orange-400 hover:bg-orange-500/20 rounded-lg text-sm font-bold font-['Onest'] transition-colors"
        >
          Написати відгук
        </button>
      </div>

      <!-- Reviews list -->
      <div v-if="productReviews.length > 0" class="flex flex-col gap-4">
        <div v-for="review in productReviews" :key="review.id" class="p-4 bg-[#161820] rounded-xl outline outline-1 outline-[#1c1f2a] flex flex-col gap-3">
          <div class="flex justify-between items-start">
            <div class="flex flex-col gap-1">
              <span class="text-gray-300 text-sm font-bold">{{ review.email }}</span>
              <div class="flex gap-0.5">
                <svg v-for="i in review.rating" :key="'f'+i" class="w-3.5 h-3.5 text-yellow-400" viewBox="0 0 16 16" fill="currentColor">
                  <path d="M8 1l1.8 3.6L14 5.3l-3 2.9.7 4.1L8 10.4l-3.7 1.9.7-4.1-3-2.9 4.2-.7z"/>
                </svg>
                <svg v-for="i in (5 - review.rating)" :key="'e'+i" class="w-3.5 h-3.5 text-[#3d4158]" viewBox="0 0 16 16" fill="currentColor">
                  <path d="M8 1l1.8 3.6L14 5.3l-3 2.9.7 4.1L8 10.4l-3.7 1.9.7-4.1-3-2.9 4.2-.7z"/>
                </svg>
              </div>
            </div>

            <div class="flex flex-col items-end gap-2">
              <span class="text-gray-500 text-xs">{{ formatReviewDate(review.created_at) }}</span>

              <div v-if="userStore.user?.id === review.user_id || userStore.user?.role === 'ADMIN'" class="flex gap-2">
                <button
                  v-if="userStore.user?.id === review.user_id"
                  @click="openEditModal(review)"
                  class="p-1.5 bg-white/5 hover:bg-orange-500/20 text-gray-400 hover:text-orange-400 rounded transition-colors"
                  title="Редагувати"
                >
                  <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button
                  @click="deleteReview"
                  class="p-1.5 bg-white/5 hover:bg-red-500/20 text-gray-400 hover:text-red-400 rounded transition-colors"
                  title="Видалити"
                >
                  <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2M10 11v6M14 11v6"/></svg>
                </button>
              </div>
            </div>
          </div>
          <p v-if="review.comment" class="text-gray-400 text-sm leading-relaxed">
            {{ review.comment }}
          </p>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="py-8 text-center bg-[#161820] rounded-xl outline outline-1 outline-[#1c1f2a]">
        <p class="text-gray-500 text-sm">Ще немає відгуків. Будьте першим!</p>
      </div>

    </div>

    <!-- ReviewModal -->
    <ReviewModal
      v-if="isReviewModalOpen && apiProduct"
      :product-id="apiProduct.id"
      :is-editing="!!editingReview"
      :initial-rating="editingReview?.rating"
      :initial-comment="editingReview?.comment"
      @close="isReviewModalOpen = false"
      @success="() => {
        isReviewModalOpen = false;
        emit('review-updated');
      }"
    />

  </div>
</template>
