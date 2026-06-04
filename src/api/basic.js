import { get, post, put, del } from './index'

export const basicApi = {
  getSuppliers() {
    return get('/basic/suppliers')
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
  
  getCustomers() {
    return get('/basic/customers')
  },
  
  createCustomer(data) {
    return post('/basic/customers', data)
  },
  
  updateCustomer(id, data) {
    return put(`/basic/customers/${id}`, data)
  },
  
  deleteCustomer(id) {
    return del(`/basic/customers/${id}`)
  },
  
  getWarehouses() {
    return get('/basic/warehouses')
  }
}