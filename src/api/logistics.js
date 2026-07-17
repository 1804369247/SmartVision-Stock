import { get, post, put } from './index-helpers'

export const logisticsApi = {
  trackShipment(trackingNo) {
    return post('/logistics/track', null, { params: { trackingNo } })
  },

  createShipment(data) {
    return post('/logistics/shipment', data)
  },

  generateLabel(orderId) {
    return post('/logistics/label', null, { params: { orderId } })
  },

  getShippingCompanies() {
    return get('/logistics/companies')
  },

  estimateShippingCost(from, to, weight) {
    return post('/logistics/estimate', null, { params: { from, to, weight } })
  },

  getShipmentsByOrder(orderId) {
    return get(`/logistics/order/${orderId}`)
  },

  updateShipmentStatus(trackingNo, status) {
    return put(`/logistics/shipment/${trackingNo}/status`, null, { params: { status } })
  },

  getLogisticsStatistics() {
    return get('/logistics/statistics')
  }
}