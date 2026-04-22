<script setup lang="ts">
// —— Імпорт router-link для навігації ——
import { RouterLink } from 'vue-router'

// —— Інтерфейс одного пункту хлібних крихт ——
interface BreadcrumbItem {
  name: string    // Текст, що відображається у крихті
  path?: string   // Маршрут для переходу; відсутній у поточної (останньої) сторінки
}

// —— Props ——
// items — масив пунктів; порядок елементів відповідає глибині навігації.
// Останній елемент вважається поточною сторінкою і не є клікабельним.
const props = defineProps<{
  items: BreadcrumbItem[]
}>()

// —— Допоміжне обчислення ——
// Повертає true, якщо переданий індекс є останнім у масиві.
// Використовується у шаблоні для вибору між <router-link> та простим текстом.
function isLast(index: number): boolean {
  return index === props.items.length - 1
}
</script>

<template>
  <nav
    aria-label="Breadcrumb"
    class="w-full bg-[#0d0f14] border-b border-[#1c1f2a]"
  >
    <div class="w-full max-w-[1536px] mx-auto px-6 py-2.5 inline-flex items-center gap-1.5 flex-wrap">

      <!-- —— Цикл по масиву items ——
           :key="index" безпечний, бо порядок крихт ніколи не змінюється динамічно -->
      <template v-for="(item, index) in items" :key="index">

        <!-- —— Клікабельна крихта (усі, крім останньої) ——
             Якщо у елемента є поле path — рендеримо router-link для SPA-навігації.
             Колір тьмяний за замовчуванням, помаранчевий при наведенні. -->
        <RouterLink
          v-if="!isLast(index) && item.path"
          :to="item.path"
          class="text-xs font-normal font-['Onest'] leading-4 text-[#5a5f7a] transition-colors duration-150 hover:text-orange-500"
        >
          {{ item.name }}
        </RouterLink>

        <!-- —— Поточна сторінка (остання крихта) ——
             Не клікабельна; aria-current="page" сигналізує про активну сторінку
             для скрін-рідерів. Колір трохи світліший, щоб відрізнятись від попередніх. -->
        <span
          v-else
          class="text-xs font-normal font-['Onest'] leading-4 text-[#787d99] cursor-default"
          :aria-current="isLast(index) ? 'page' : undefined"
        >
          {{ item.name }}
        </span>

        <!-- —— Роздільник-стрілка між крихтами ——
             Відображається після кожного елемента, окрім останнього.
             aria-hidden="true" — декоративний елемент, прихований від скрін-рідерів. -->
        <svg
          v-if="!isLast(index)"
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
