import { get, post, put } from './index-helpers'

export const pickApi = {
  /** 创建波次拣货 */
  createWavePick(orderIds) {
    return post('/pick/wave', orderIds)
  },

  /** 获取所有活动波次 */
  getActiveWaves() {
    return get('/pick/waves/active')
  },

  /** 获取波次详情 */
  getWavePickDetail(waveId) {
    return get(`/pick/wave/${waveId}`)
  },

  /** 获取波次下的拣货任务列表 */
  getPickTasks(waveId) {
    // 仅在 waveId 有效时携带该参数，避免拼出 waveId=undefined
    return get('/pick/tasks', waveId != null && waveId !== '' ? { waveId } : {})
  },

  /** 获取单个拣货任务详情 */
  getPickTaskDetail(taskId) {
    return get(`/pick/tasks/${taskId}`)
  },

  /** 完成单个拣货任务 */
  completePickTask(taskId, data) {
    return put(`/pick/tasks/${taskId}/complete`, data)
  },

  /** 优化拣货路径 */
  optimizePickPath(waveId) {
    return post(`/pick/path/optimize?waveId=${waveId}`)
  },

  /** 取消波次拣货 */
  cancelWavePick(waveId) {
    return post(`/pick/wave/${waveId}/cancel`)
  },

  /** 获取波次统计信息 */
  getPickStatistics(waveId) {
    return get(`/pick/statistics/${waveId}`)
  }
}
