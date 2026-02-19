// Vite 설정 파일 - Vue 플러그인을 사용하도록 설정합니다
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // Vue 플러그인: .vue 파일을 처리할 수 있게 해줍니다
  plugins: [vue()],
})
