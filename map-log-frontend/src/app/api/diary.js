import api from './axios.js'

export const diaryApi = {
    /** 일기 작성 - POST /api/diaries (multipart) */
    createDiary(formData) {
        return api.post('/api/diaries', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    /** 일기 상세 조회 - GET /api/diaries/:diaryId */
    getDiary(diaryId) {
        return api.get(`/api/diaries/${diaryId}`)
    },

    /** 일기 수정 - PUT /api/diaries/:diaryId (multipart) */
    updateDiary(diaryId, formData) {
        return api.put(`/api/diaries/${diaryId}`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    /** 일기 삭제 - DELETE /api/diaries/:diaryId */
    deleteDiary(diaryId) {
        return api.delete(`/api/diaries/${diaryId}`)
    },

    /** 지도 범위 내 마커 조회 - GET /api/diaries/map */
    getMapMarkers({ swLat, swLng, neLat, neLng }) {
        return api.get('/api/diaries/map', { params: { swLat, swLng, neLat, neLng } })
    },

    /** 일기 공유 설정 - POST /api/diaries/:diaryId/share */
    shareDiary(diaryId, { friendIds }) {
        return api.post(`/api/diaries/${diaryId}/share`, { friendIds })
    },

    /** 일기 공유 해제 - DELETE /api/diaries/:diaryId/share/:userId */
    unshareDiary(diaryId, userId) {
        return api.delete(`/api/diaries/${diaryId}/share/${userId}`)
    },

    /** 스크랩 추가 - POST /api/scraps */
    addScrap(diaryId) {
        return api.post('/api/scraps', { diaryId })
    },

    /** 스크랩 취소 - DELETE /api/scraps/:diaryId */
    removeScrap(diaryId) {
        return api.delete(`/api/scraps/${diaryId}`)
    }
}
