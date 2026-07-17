import { get, post } from './index-helpers'

export const logApi = {
  /** 分页查询操作日志（支持 module/operatorId 筛选） */
  getOperationLogs(params = {}) {
    return get('/logs/operation', params)
  },

  /** 获取单条日志详情 */
  getOperationLog(id) {
    return get(`/logs/operation/${id}`)
  },

  /** 获取近期操作日志（通过 OperationLogController /api/system/logs/recent） */
  getRecentLogs() {
    return get('/system/logs/recent')
  },

  /** 导出操作日志为 CSV（后端使用 POST） */
  exportLogs(params = {}) {
    return post('/logs/operation/export', null, { params, responseType: 'blob' })
  },

  /** 分页查询出入库记录 */
  getInoutRecords(params = {}) {
    return get('/inout/records', params)
  }
}
