import { get, post, del } from './index-helpers'

export const notificationApi = {
  getList(userId, params = {}) {
    return get('/notification/list', { userId, ...params })
  },

  getUnreadCount(userId) {
    return get('/notification/unread-count', { userId })
  },

  markAsRead(id) {
    return post(`/notification/${id}/read`)
  },

  markAllAsRead(userId) {
    return post('/notification/read-all', null, { userId })
  },

  delete(id) {
    return del(`/notification/${id}`)
  },

  send(data) {
    return post('/notification/send', data)
  }
}