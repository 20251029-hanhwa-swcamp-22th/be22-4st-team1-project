<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import Layout from './components/Layout.vue'
import { useAuthStore } from './stores/auth.js'

const auth = useAuthStore()
const route = useRoute()

// 인증이 필요한 레이아웃을 사용하는지 여부
const useLayout = computed(() => auth.isAuthenticated && route.meta.layout !== false)
</script>

<template>
  <Layout v-if="useLayout">
    <RouterView />
  </Layout>
  <RouterView v-else />
</template>
