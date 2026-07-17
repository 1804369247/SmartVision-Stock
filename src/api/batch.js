import { get, post, put } from './index-helpers'

export const batchApi = {
  createBatch(sku, batchNo, quantity, expiryDate) {
    return post('/batch/create', null, { params: { sku, batchNo, quantity, expiryDate } })
  },

  getBatch(batchId) {
    return get(`/batch/${batchId}`)
  },

  getBatchesBySku(sku) {
    return get(`/batch/sku/${sku}`)
  },

  selectBatchByFIFO(sku, quantity) {
    return post('/batch/select/fifo', null, { params: { sku, quantity } })
  },

  selectBatchByLIFO(sku, quantity) {
    return post('/batch/select/lifo', null, { params: { sku, quantity } })
  },

  selectBatchByFEFO(sku, quantity) {
    return post('/batch/select/fefo', null, { params: { sku, quantity } })
  },

  getBatchHistory(sku) {
    return get(`/batch/history/${sku}`)
  },

  updateBatchQuantity(batchId, quantity) {
    return put(`/batch/${batchId}/quantity`, null, { params: { quantity } })
  },

  getBatchTraceability(batchId) {
    return get(`/batch/${batchId}/traceability`)
  },

  getExpiringBatches(daysThreshold = 30) {
    return get('/batch/expiring', { daysThreshold })
  },

  getBatchStatistics() {
    return get('/batch/statistics')
  }
}