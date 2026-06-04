import { get } from './index'

export const exportApi = {
  exportInventory(params = {}) {
    return get('/export/inventory', params)
  },
  
  exportInboundList(params = {}) {
    return get('/export/inbound', params)
  },
  
  exportOutboundList(params = {}) {
    return get('/export/outbound', params)
  },
  
  exportStockReport(params = {}) {
    return get('/export/stock-report', params)
  }
}