import { get } from './index-helpers'

export const reportApi = {
  getInventoryReport(params) {
    return get('/reports/inventory', params)
  },

  getInoutFlowReport(params) {
    return get('/reports/inout-records', params)
  },

  getUtilizationReport(params) {
    return get('/reports/utilization', params)
  },

  getAlertsReport() {
    return get('/reports/alerts')
  },

  getKpi() {
    return get('/reports/kpi')
  }
}