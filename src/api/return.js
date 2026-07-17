import { get, post, put } from './index-helpers'

export const returnApi = {
  /** 创建退货申请 */
  createReturnRequest(data) {
    return post('/return/create', data)
  },

  /** 获取退货详情 */
  getReturnRequest(returnId) {
    return get(`/return/${returnId}`)
  },

  /** 获取退货列表（可按状态筛选） */
  getReturnRequests(status) {
    return get(`/return/list?status=${status || ''}`)
  },

  /** 质检退货 */
  inspectReturn(returnId, data) {
    return put(`/return/${returnId}/inspect`, data)
  },

  /** 确认退货入库 */
  confirmReturn(returnId) {
    return put(`/return/${returnId}/confirm`)
  },

  /** 拒绝退货 */
  rejectReturn(returnId, reason) {
    return put(`/return/${returnId}/reject`, { reason })
  },

  /** 处理退款（后端使用 POST） */
  processRefund(returnId) {
    return post(`/return/${returnId}/refund`)
  },

  /** 获取退货统计 */
  getReturnStatistics() {
    return get('/return/statistics')
  },

  /** 获取客户退货历史（后端使用路径参数） */
  getReturnHistory(customerId) {
    return get(`/return/history/${customerId}`)
  }
}
