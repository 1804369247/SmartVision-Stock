import { get } from './index-helpers'

export const exportApi = {
  /**
   * 导出库存报表 (Excel)
   * @param {Object} params - { status, attribute, area }
   */
  exportInventory(params = {}) {
    return get('/export/inventory', params, { responseType: 'blob' })
  },

  /**
   * 导出入出库记录 (Excel)
   * @param {Object} params - { goodsName, type, startTime, endTime, operator }
   */
  exportInoutRecords(params = {}) {
    return get('/export/inout-records', params, { responseType: 'blob' })
  },

  /**
   * 导出仓库统计报表 (Excel)
   */
  exportStatistics() {
    return get('/export/statistics', null, { responseType: 'blob' })
  }
}
