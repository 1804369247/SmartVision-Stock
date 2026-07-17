import { get, post, put, del } from './index-helpers'

export const modelApi = {
  getModel(modelId) {
    return get(`/models/${modelId}`)
  },

  listModels(params = {}) {
    return get('/models', params)
  },

  createModel(data) {
    return post('/models', data)
  },

  updateModel(modelId, data) {
    return put(`/models/${modelId}`, data)
  },

  deleteModel(modelId) {
    return del(`/models/${modelId}`)
  },

  mergeModel(modelId, sceneId) {
    return put(`/models/${modelId}/merge`, null, { params: { sceneId } })
  },

  getMetadata(modelId) {
    return get(`/models/${modelId}/metadata`)
  },

  getReconstructionProgress(taskId) {
    return get(`/vision/reconstruct/progress/${taskId}`)
  },

  cancelReconstruction(taskId) {
    return post(`/vision/reconstruct/cancel/${taskId}`)
  },

  exportModel(modelId, format = 'obj') {
    return get(`/vision/model/export/${modelId}`, { format }, { responseType: 'blob' })
  },

  optimizeModel(modelId, quality = 80) {
    return post(`/vision/model/optimize/${modelId}`, null, { params: { quality } })
  }
}