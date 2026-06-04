import { get, post, put, del } from './index'

export const locationApi = {
  getAll() {
    return get('/locations')
  },
  
  getById(id) {
    return get(`/locations/${id}`)
  },
  
  create(data) {
    return post('/locations', data)
  },
  
  update(id, data) {
    return put(`/locations/${id}`, data)
  },
  
  delete(id) {
    return del(`/locations/${id}`)
  },
  
  getByArea(area) {
    return get('/locations/area', { area })
  }
}