import { get, post, put, del } from './index-helpers'

export const systemApi = {
  // 登录已由 AuthController (/api/auth/login) 统一处理
  login(data) {
    return post('/auth/login', data)
  },

  // 注册
  register(data) {
    return post('/auth/register', data)
  },
  
  logout() {
    return post('/auth/logout')
  },
  
  getCurrentUser() {
    return get('/auth/me')
  },
  
  refreshToken(refreshToken) {
    return post('/auth/refresh', { refreshToken })
  },
  
  getUsers(params = {}) {
    return get('/system/users', params)
  },
  
  getAllUsers() {
    return get('/system/users/all')
  },
  
  getUser(id) {
    return get(`/system/users/${id}`)
  },
  
  createUser(data) {
    return post('/system/users', data)
  },
  
  updateUser(id, data) {
    return put(`/system/users/${id}`, data)
  },
  
  deleteUser(id) {
    return del(`/system/users/${id}`)
  },
  
  changePassword(id, data) {
    return post(`/system/users/${id}/password/change`, data)
  },
  
  resetPassword(id) {
    return post(`/system/users/${id}/password/reset`)
  },
  
  updateUserStatus(id, status) {
    return put(`/system/users/${id}/status`, { status })
  },
  
  getRoles(params = {}) {
    return get('/system/roles', params)
  },
  
  getAllRoles() {
    return get('/system/roles/all')
  },
  
  getRole(id) {
    return get(`/system/roles/${id}`)
  },
  
  createRole(data) {
    return post('/system/roles', data)
  },
  
  updateRole(id, data) {
    return put(`/system/roles/${id}`, data)
  },
  
  deleteRole(id) {
    return del(`/system/roles/${id}`)
  },
  
  updateRoleStatus(id, status) {
    return put(`/system/roles/${id}/status`, { status })
  },
  
  getLogs(params = {}) {
    return get('/system/logs', params)
  },
  
  getRecentLogs() {
    return get('/system/logs/recent')
  }
}