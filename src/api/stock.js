import { get, post, put, del } from './index'

export const stockApi = {
  getInventory() {
    return get('/inventory')
  },
  
  getInventoryByLocation(locationId) {
    return get('/inventory/location', { locationId })
  },
  
  getInventoryByGoods(goodsId) {
    return get('/inventory/goods', { goodsId })
  },
  
  inbound(data) {
    return post('/inbound', data)
  },
  
  outbound(data) {
    return post('/outbound', data)
  },
  
  move(data) {
    return post('/move', data)
  },
  
  getInstances() {
    return get('/instances')
  }
}