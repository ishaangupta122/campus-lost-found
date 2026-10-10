import api, { cleanParams } from './api.js'

export const getDashboard = () => api.get('/api/admin/dashboard').then((res) => res.data)

export const getPendingClaims = () => api.get('/api/admin/claims/pending').then((res) => res.data)

// decision is "APPROVED" or "REJECTED"
export const decideClaim = (claimId, decision) =>
  api.put(`/api/admin/claims/${claimId}/decision`, { decision }).then((res) => res.data)

// params can contain: q, type, category, location, status
export const listAllItems = (params) => api.get('/api/admin/items', { params: cleanParams(params) }).then((res) => res.data)

export const closeItemAsAdmin = (id) => api.put(`/api/admin/items/${id}/close`).then((res) => res.data)

export const markReturnedAsAdmin = (id) => api.put(`/api/admin/items/${id}/returned`).then((res) => res.data)
