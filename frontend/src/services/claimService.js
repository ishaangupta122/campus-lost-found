import api from './api.js'

export const submitClaim = (itemId, message) => api.post(`/api/items/${itemId}/claims`, { message }).then((res) => res.data)

export const getMyClaims = () => api.get('/api/claims/my').then((res) => res.data)

export const getClaim = (id) => api.get(`/api/claims/${id}`).then((res) => res.data)
