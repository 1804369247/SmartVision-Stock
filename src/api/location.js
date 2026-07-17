import { get, post, put, del } from './index-helpers'

export const locationApi = {
  getAll() {
    return get('/locations', null, { cache: false })
  },

  getById(id) {
    return get(`/locations/${id}`, null, { cache: false })
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
    return get('/locations/area', { area }, { cache: false })
  }
}
