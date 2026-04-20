<script setup lang="ts">
const paths = [
  'Головна',
  'Електроніка',
  'Смартфони',
  'Samsung',
  'Samsung Galaxy S24 Ultra 12/256GB',
]

const emit = defineEmits<{
  (e: 'navigate', index: number, label: string): void
}>()
</script>

<template>
  <nav
    aria-label="Breadcrumb"
    class="w-full bg-[#0d0f14] border-b border-[#1c1f2a]"
  >
    <div class="w-full max-w-[1536px] mx-auto px-6 py-2.5 inline-flex items-center gap-1.5 flex-wrap">
      <template v-for="(label, i) in paths" :key="label">

        <!-- Crumb -->
        <span
          class="text-xs font-normal font-['Onest'] leading-4 transition-colors duration-150"
          :class="
            i < paths.length - 1
              ? 'text-[#5a5f7a] hover:text-orange-500 cursor-pointer'
              : 'text-[#787d99] cursor-default'
          "
          :aria-current="i === paths.length - 1 ? 'page' : undefined"
          @click="i < paths.length - 1 && emit('navigate', i, label)"
        >
          {{ label }}
        </span>

        <!-- Separator -->
        <svg
          v-if="i < paths.length - 1"
          class="w-3 h-3 shrink-0 text-[#2a2d3e]"
          viewBox="0 0 12 12"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <path
            d="M4.5 2.5L7.5 6L4.5 9.5"
            stroke="currentColor"
            stroke-width="1.2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>

      </template>
    </div>
  </nav>
</template>
