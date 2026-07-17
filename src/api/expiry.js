import { get, post } from './index-helpers'

export const expiryApi = {
  checkExpiringProducts(daysThreshold = 30) {
    return get('/expiry/check', { daysThreshold })
  },

  getActiveAlerts() {
    return get('/expiry/alerts/active')
  },

  getExpiryAlert(alertId) {
    return get(`/expiry/alert/${alertId}`)
  },

  processAlert(alertId, action) {
    return post(`/expiry/alert/${alertId}/process`, null, { action })
  },

  getExpiryStatistics() {
    return get('/expiry/statistics')
  },

  getExpiredProducts() {
    return get('/expiry/expired')
  },

  markAsExpired(batchId) {
    return post(`/expiry/${batchId}/mark-expired`)
  }
}