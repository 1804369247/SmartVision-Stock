import { get, post, del } from './index-helpers'

export const stockCountApi = {
  create(data) {
    return post('/stock-count/create', data)
  },

  getList(params = {}) {
    return get('/stock-count/list', params)
  },

  get(id) {
    return get(`/stock-count/${id}`)
  },

  start(id) {
    return post(`/stock-count/${id}/start`)
  },

  updateItems(id, items) {
    return post(`/stock-count/${id}/items`, items)
  },

  complete(id) {
    return post(`/stock-count/${id}/complete`)
  },

  confirm(id) {
    return post(`/stock-count/${id}/confirm`)
  },

  delete(id) {
    return del(`/stock-count/${id}`)
  }
}