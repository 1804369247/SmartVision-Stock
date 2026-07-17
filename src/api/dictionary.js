import { get } from './index-helpers'

export const dictionaryApi = {
  getGoodsCategory() {
    return get('/dictionary/goods-category')
  },

  getLocationAttribute() {
    return get('/dictionary/location-attribute')
  },

  getLocationStatus() {
    return get('/dictionary/location-status')
  },

  getOrderStatus() {
    return get('/dictionary/order-status')
  },

  getStockCountStatus() {
    return get('/dictionary/stock-count-status')
  },

  getStorageRule() {
    return get('/dictionary/storage-rule')
  },

  getNotificationType() {
    return get('/dictionary/notification-type')
  },

  getAll() {
    return get('/dictionary/all')
  }
}