<script setup lang="ts">
import { onMounted } from 'vue';
import { useCategoryStore } from './state/categoryStore';

// Ініціалізуємо сховище категорій
const categoryStore = useCategoryStore();

// Завантажуємо категорії один раз при старті додатку
onMounted(() => {
  categoryStore.fetchCategories();
});

// Зараз ми дивимося сторінку товару.
// Якщо захочеш знову побачити головну — просто поміняй назву в template нижче
//import ProductPage from './components/ProductPage.vue';
</script>

<template>
  <router-view/>
</template>
