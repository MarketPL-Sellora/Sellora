<script setup lang="ts">
import { onMounted } from 'vue';
import { useCategoryStore } from './state/categoryStore';
import { useUserStore } from './state/userStore'; // ДОДАНО
import { useGroupBuyStore } from './state/groupBuyStore'; // ДОДАНО
import GlobalConfirmModal from './components/modals/GlobalConfirmModal.vue';

// Ініціалізуємо сховища
const categoryStore = useCategoryStore();
const userStore = useUserStore(); // ДОДАНО
const groupBuyStore = useGroupBuyStore(); // ДОДАНО

// Завантажуємо дані один раз при старті додатку
onMounted(async () => { // ЗМІНЕНО НА async
  categoryStore.fetchCategories();
  await userStore.fetchMe(); // ДОДАНО: перевіряємо авторизацію при старті
  if (userStore.isAuthenticated) {
    groupBuyStore.fetchMySessions();
  }
});

// Зараз ми дивимося сторінку товару.
// Якщо захочеш знову побачити головну — просто поміняй назву в template нижче
//import ProductPage from './views/ProductPage.vue';
</script>

<template>
  <router-view/>
  <GlobalConfirmModal />
</template>
