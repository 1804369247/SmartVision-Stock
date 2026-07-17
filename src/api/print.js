import { get, post } from './index-helpers'

export const printApi = {
  /** 获取可用打印机列表 */
  getPrinters() {
    return get('/print/printers')
  },

  /** 打印标签 */
  printLabel(data) {
    return post('/print/label', data)
  },

  /** 打印订单 */
  printOrder(orderId, type) {
    return post('/print/order', { orderId, type })
  }
  
  // 注意：以下批量打印功能暂未在后端实现，需要时请在 PrintController 中添加对应端点：
  // - POST /print/inventory/{sheetId}      — 打印盘点单
  // - POST /print/orders/batch             — 批量打印订单
  // - POST /print/goods/labels             — 商品标签批量打印
  // - POST /print/locations/labels         — 库位标签批量打印
}

export const qrCodeApi = {
  generate(content, width = 200, height = 200) {
    return get(`/qrcode/generate?content=${encodeURIComponent(content)}&width=${width}&height=${height}`, null, { responseType: 'blob' })
  },

  generateGoods(goodsId) {
    return get(`/qrcode/goods/${goodsId}`, null, { responseType: 'blob' })
  },

  generateLocation(locationId) {
    return get(`/qrcode/location/${locationId}`, null, { responseType: 'blob' })
  },

  generateBatch(batchId) {
    return get(`/qrcode/batch/${batchId}`, null, { responseType: 'blob' })
  },

  batchGenerateGoods(goodsIds) {
    return post('/qrcode/goods/batch', goodsIds)
  },

  batchGenerateLocations(locationIds) {
    return post('/qrcode/locations/batch', locationIds)
  },

  scan(imageData) {
    return post('/qrcode/scan', { image: imageData })
  }
}
