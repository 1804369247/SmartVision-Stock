import { get, post, put, del } from './index-helpers'

export const stockApi = {
  getInventory(params = {}) {
    return get('/reports/inventory', params)
  },

  getInventoryByLocation(locationId) {
    return get('/reports/inventory', { area: locationId })
  },

  getInventoryByGoods(goodsId) {
    return get('/reports/inventory', { goodsName: goodsId })
  },

  getGoods() {
    return get('/goods')
  },

  getLocations() {
    return get('/locations')
  },

  adjust(data) {
    return post('/adjust', data)
  },

  move(data) {
    return post('/move', data)
  },

  getInstances(params = {}) {
    return get('/instances', params)
  }
}
