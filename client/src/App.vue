<script setup lang="ts">
import { onMounted } from 'vue';
import { useCategoryStore } from './state/categoryStore';
import { useUserStore } from './state/userStore'; // ДОДАНО

// Ініціалізуємо сховища
const categoryStore = useCategoryStore();
const userStore = useUserStore(); // ДОДАНО

// Завантажуємо дані один раз при старті додатку
onMounted(async () => { // ЗМІНЕНО НА async
  categoryStore.fetchCategories();
  await userStore.fetchMe(); // ДОДАНО: перевіряємо авторизацію при старті
});

// Зараз ми дивимося сторінку товару.
// Якщо захочеш знову побачити головну — просто поміняй назву в template нижче
//import ProductPage from './views/ProductPage.vue';
</script>

<template>
  <router-view/>
</template>
