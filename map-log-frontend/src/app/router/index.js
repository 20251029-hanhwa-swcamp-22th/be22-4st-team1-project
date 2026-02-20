import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/app/stores/auth.js'

const routes = [
    // ── 인증 불필요 ──
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/app/views/LoginView.vue'),
        meta: { layout: false, guest: true }
    },
    {
        path: '/signup',
        name: 'SignUp',
        component: () => import('@/app/views/SignUpView.vue'),
        meta: { layout: false, guest: true }
    },

    // ── 인증 필요 ──
    {
        path: '/',
        redirect: '/map'
    },
    {
        path: '/map',
        name: 'Map',
        component: () => import('@/app/views/MapView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/diaries/:diaryId',
        name: 'DiaryDetail',
        component: () => import('@/app/views/DiaryDetailView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/feed',
        name: 'Feed',
        component: () => import('@/app/views/FeedView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/friends',
        name: 'Friends',
        component: () => import('@/app/views/FriendView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/notifications',
        name: 'Notifications',
        component: () => import('@/app/views/NotificationsView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/mypage',
        name: 'MyPage',
        component: () => import('@/app/views/MyPageView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/admin',
        name: 'Admin',
        component: () => import('@/app/views/AdminView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
    },

    // ── fallback ──
    {
        path: '/:pathMatch(.*)*',
        redirect: '/map'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// ── 네비게이션 가드 ──
router.beforeEach((to) => {
    const auth = useAuthStore()

    // 인증 필요 라우트인데 비로그인 상태
    if (to.meta.requiresAuth && !auth.isAuthenticated) {
        return { name: 'Login', query: { redirect: to.fullPath } }
    }

    // ADMIN 전용 라우트
    if (to.meta.requiresAdmin && !auth.isAdmin) {
        return { name: 'Map' }
    }

    // 이미 로그인 상태에서 로그인/회원가입 접근 시
    if (to.meta.guest && auth.isAuthenticated) {
        return { name: 'Map' }
    }
})

export default router
