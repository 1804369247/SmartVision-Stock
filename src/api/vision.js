import { get, post, put, del } from './index-helpers'

export const visionApi = {
  uploadImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    return post('/vision/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  
  uploadMultipleImages(files) {
    const formData = new FormData()
    files.forEach(file => formData.append('files', file))
    return post('/vision/upload/multiple', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  
  getImage(imageId) {
    return get(`/vision/image/${imageId}`, null, { responseType: 'blob' })
  },
  
  recognize(uploadId, angleCount = 4) {
    return post(`/vision/recognize?uploadId=${uploadId}&angleCount=${angleCount}`)
  },
  
  detectObjects(imageId) {
    return post(`/vision/detect?imageId=${imageId}`)
  },
  
  analyzeShelf(uploadId) {
    return post(`/vision/analyze/shelf?uploadId=${uploadId}`)
  },
  
  measureDistance(imageId, points) {
    return post(`/vision/measure?imageId=${imageId}`, points)
  },
  
  reconstruct(uploadId) {
    return post(`/vision/reconstruct?uploadId=${uploadId}`)
  },
  
  getReconstructionProgress(taskId) {
    return get(`/vision/reconstruct/progress/${taskId}`)
  },
  
  cancelReconstruction(taskId) {
    return post(`/vision/reconstruct/cancel/${taskId}`)
  },
  
  exportModel(modelId, format = 'obj') {
    return get(`/vision/model/export/${modelId}?format=${format}`, null, { responseType: 'blob' })
  },
  
  optimizeModel(modelId, quality = 80) {
    return post(`/vision/model/optimize/${modelId}?quality=${quality}`)
  }
}

