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

export interface FlatCategory {
  id: number;
  name: string;
  parentId: number | null;
}

// ─── Pinia Store: categoryStore ──────────────────────────────────────────────
export const useCategoryStore = defineStore('category', () => {

  // ─── Стан ──────────────────────────────────────────────────────────────────
  const categories = ref<Category[]>([]);
  const flatCategories = ref<FlatCategory[]>([]);
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

  async function fetchFlatCategories() {
    isLoading.value = true;
    error.value = null;
    try {
      // Використовуємо size=1000, щоб отримати всі категорії для селекта без складної пагінації на фронті
      const response = await apiClient.get('/categories?page=0&size=1000');
      // Згідно зі Swagger, масив даних лежить у полі content
      flatCategories.value = response.data.content || [];
    } catch (err: any) {
      error.value = err?.message || 'Помилка при завантаженні категорій';
      console.error('fetchFlatCategories error:', err);
    } finally {
      isLoading.value = false;
    }
  }

  // ─── Дія: створити нову категорію ─────────────────────────────────────────
  async function createCategory(payload: { name: string; parentId: number | null }) {
    isLoading.value = true;
    error.value = null;
    try {
      await apiClient.post('/categories', payload);
    } catch (err: any) {
      error.value = err?.message || 'Помилка при створенні категорії';
      console.error('createCategory error:', err);
      throw err;
    } finally {
      isLoading.value = false;
    }
  }

  // ─── Дія: видалити категорію за ID ──────────────────────────────────────────
  async function deleteCategory(id: number) {
    isLoading.value = true;
    error.value = null;
    try {
      await apiClient.delete('/categories/' + id);
    } catch (err: any) {
      error.value = err?.message || 'Помилка при видаленні категорії';
      console.error('deleteCategory error:', err);
      throw err;
    } finally {
      isLoading.value = false;
    }
  }

  return { categories, flatCategories, isLoading, error, fetchCategories, fetchFlatCategories, createCategory, deleteCategory };
});
