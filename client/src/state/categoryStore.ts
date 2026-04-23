import { defineStore } from 'pinia';
import { ref } from 'vue';
// Видаляємо: import axios from 'axios'
import { apiClient } from '../api/axios' // Або шлях до твого файлу з налаштованим Axios

// ─── Інтерфейс категорії (згідно зі Swagger) ─────────────────────────────────
export interface Category {
  id: number;
  name: string;
  parentId: number | null;
  children: Category[];
}

// ─── Pinia Store: categoryStore ──────────────────────────────────────────────
export const useCategoryStore = defineStore('category', () => {

  // ─── Стан ──────────────────────────────────────────────────────────────────
  const categories = ref<Category[]>([]);
  const isLoading = ref<boolean>(false);
  const error = ref<string | null>(null);

  // ─── Дія: завантажити дерево категорій з бекенду ───────────────────────────
  async function fetchCategories() {
    isLoading.value = true;
    error.value = null;

    try {
      const response = await apiClient.get('/categories/tree')
      categories.value = response.data;
    } catch (err: any) {
      // Записуємо повідомлення про помилку в стан та виводимо в консоль
      error.value = err?.message || 'Помилка при завантаженні категорій';
      console.error('fetchCategories error:', err);
    } finally {
      isLoading.value = false;
    }
  }

  return { categories, isLoading, error, fetchCategories };
});
