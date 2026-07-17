import { get, post, put, del } from './index-helpers'

export const warehouseApi = {
  /** 获取所有仓库 */
  getAllWarehouses() {
    return get('/warehouses')
  },

  /** 获取仓库详情 */
  getWarehouse(id) {
    return get(`/warehouses/${id}`)
  },

  /** 创建仓库 */
  createWarehouse(data) {
    return post('/warehouses', data)
  },

  /** 更新仓库 */
  updateWarehouse(id, data) {
    return put(`/warehouses/${id}`, data)
  },

  /** 删除仓库 */
  deleteWarehouse(id) {
    return del(`/warehouses/${id}`)
  },

  /** 发起库存调拨（后端使用 POST + query 参数） */
  transferStock(data) {
    return post('/warehouses/transfer', null, {
      params: {
        sku: data.sku,
        quantity: data.quantity,
        fromWarehouseId: data.fromWarehouseId,
        toWarehouseId: data.toWarehouseId
      }
    })
  },

  /** 获取调拨申请列表 */
  getTransferRequests(status) {
    return get(`/warehouses/transfers?status=${status || ''}`)
  },

  /** 批准调拨（后端 POST /transfer/{id}/approve） */
  approveTransfer(transferId) {
    return post(`/warehouses/transfer/${transferId}/approve`)
  },

  /** 执行调拨（后端 POST /transfer/{id}/execute） */
  executeTransfer(transferId) {
    return post(`/warehouses/transfer/${transferId}/execute`)
  },

  /** 获取仓库库存 */
  getWarehouseInventory(warehouseId) {
    return get(`/warehouses/${warehouseId}/inventory`)
  },

  /** 获取 SKU 在全部仓库的共享库存（后端使用路径参数） */
  getSharedInventory(sku) {
    return get(`/warehouses/shared/${sku}`)
  },

  /** 获取仓库统计 */
  getWarehouseStatistics() {
    return get('/warehouses/statistics')
  }
}
