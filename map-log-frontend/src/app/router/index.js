import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/app/stores/auth.js'

const routes = [
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
    {
        path: '/:pathMatch(.*)*',
        redirect: '/map'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach(async (to) => {
    const auth = useAuthStore()

    if (auth.isAuthenticated && (!auth.user?.userId || !auth.user?.role)) {
        try {
            await auth.hydrateUser()
        } catch (_) {
            auth.clear()
        }
    }

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
        return { name: 'Login', query: { redirect: to.fullPath } }
    }

    if (to.meta.requiresAdmin && !auth.isAdmin) {
        return { name: 'Map' }
    }

    if (to.meta.guest && auth.isAuthenticated) {
        return { name: 'Map' }
    }
})

export default router
