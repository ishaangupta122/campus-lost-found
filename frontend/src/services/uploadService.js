import api from './api.js'

// Sends one picture to the backend. Returns the path to store in the report's imagePath.
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/api/uploads', formData).then((res) => res.data.imagePath)
}
