import { get, post, put, del } from './index'

export const goodsApi = {
  getAll() {
    return get('/goods')
  },
  
  getById(id) {
    return get(`/goods/${id}`)
  },
  
  create(data) {
    return post('/goods', data)
  },
  
  update(id, data) {
    return put(`/goods/${id}`, data)
  },
  
  delete(id) {
    return del(`/goods/${id}`)
  },
  
  search(keyword) {
    return get('/goods/search', { keyword })
  }
}