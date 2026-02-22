import api from './axios.js'

export const feedApi = {
    /** 친구 피드 조회 - GET /api/feed */
    getFeed({ page = 0, size = 10 } = {}) {
        return api.get('/feed', { params: { page, size } })
    }
}
