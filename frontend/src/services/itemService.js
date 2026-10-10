import api, { cleanParams } from './api.js'

// params can contain: q, type, category, location, status (the time filter `period` is applied in the browser)
export const listItems = ({ period, ...params } = {}) =>
  api.get('/api/items', { params: cleanParams(params) }).then((res) => res.data)

export const getMyItems = () => api.get('/api/items/my').then((res) => res.data)

export const getItem = (id) => api.get(`/api/items/${id}`).then((res) => res.data)

export const createItem = (data) => api.post('/api/items', data).then((res) => res.data)

export const updateItem = (id, data) => api.put(`/api/items/${id}`, data).then((res) => res.data)

export const closeItem = (id) => api.put(`/api/items/${id}/close`).then((res) => res.data)

export const deleteItem = (id) => api.delete(`/api/items/${id}`)

export const markItemReturned = (id) => api.put(`/api/items/${id}/returned`).then((res) => res.data)
