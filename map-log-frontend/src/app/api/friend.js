import api from './axios.js'

export const friendApi = {
    /** 친구 요청 - POST /api/friends */
    sendRequest(receiverId) {
        return api.post('/api/friends', { receiverId })
    },

    /** 친구 요청 응답 - PATCH /api/friends/:friendId */
    respond(friendId, action) {
        return api.patch(`/api/friends/${friendId}`, { action })
    },

    /** 친구 목록 조회 - GET /api/friends */
    getFriends() {
        return api.get('/api/friends')
    },

    /** 받은 친구 요청 목록 - GET /api/friends/pending */
    getPending() {
        return api.get('/api/friends/pending')
    }
}
