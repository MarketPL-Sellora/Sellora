import { defineStore } from 'pinia';
import { ref } from 'vue';
import { apiClient } from '../api/axios'

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

export const useCategoryStore = defineStore('category', () => {

  const categories = ref<Category[]>([]);
  const flatCategories = ref<FlatCategory[]>([]);
  const isLoading = ref<boolean>(false);
  const error = ref<string | null>(null);

  async function fetchCategories() {
    isLoading.value = true;
    error.value = null;

    try {
      const response = await apiClient.get('/categories/tree')
      categories.value = response.data;
    } catch (err: any) {
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
      const response = await apiClient.get('/categories?page=0&size=1000');
      flatCategories.value = response.data.content || [];
    } catch (err: any) {
      error.value = err?.message || 'Помилка при завантаженні категорій';
      console.error('fetchFlatCategories error:', err);
    } finally {
      isLoading.value = false;
    }
  }

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
