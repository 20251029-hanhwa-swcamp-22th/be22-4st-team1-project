import api from './axios.js'

export const friendApi = {
    /** 친구 요청 - POST /api/friends */
    sendRequest(receiverId) {
        return api.post('/friends', { receiverId })
    },

    /** 친구 요청 응답 - PATCH /api/friends/:friendId */
    respond(friendId, action) {
        return api.patch(`/friends/${friendId}`, { status: action })
    },

    /** 친구 목록 조회 - GET /api/friends */
    getFriends() {
        return api.get('/friends')
    },

    /** 받은 친구 요청 목록 - GET /api/friends/pending */
    getPending() {
        return api.get('/friends/pending')
    }
}
