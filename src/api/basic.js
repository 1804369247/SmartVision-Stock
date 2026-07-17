import { get, post, put, del } from './index-helpers'

export const basicApi = {
  // ===== 供应商管理 =====
  getSuppliers() {
    return get('/basic/suppliers', null, { cache: false })
  },

  createSupplier(data) {
    return post('/basic/suppliers', data)
  },

  updateSupplier(id, data) {
    return put(`/basic/suppliers/${id}`, data)
  },

  deleteSupplier(id) {
    return del(`/basic/suppliers/${id}`)
  },

  // ===== 仓库管理 =====
  getWarehouses() {
    return get('/basic/warehouses', null, { cache: false })
  },

  createWarehouse(data) {
    return post('/basic/warehouses', data)
  },

  updateWarehouse(id, data) {
    return put(`/basic/warehouses/${id}`, data)
  },

  deleteWarehouse(id) {
    return del(`/basic/warehouses/${id}`)
  }
}
