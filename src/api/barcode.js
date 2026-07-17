import { get, post } from './index-helpers'

export const barcodeApi = {
  /**
   * 通过条码查询商品
   * @param {string} barcode - 条码
   */
  getGoodsByBarcode(barcode) {
    return get(`/barcode/goods/${barcode}`)
  },

  /**
   * 通过条码查询库位
   * @param {string} barcode - 条码
   */
  getLocationByBarcode(barcode) {
    return get(`/barcode/location/${barcode}`)
  },

  /**
   * 通用扫码接口（自动识别类型）
   * @param {string} barcode - 条码
   */
  scanBarcode(barcode) {
    return post('/barcode/scan', { barcode })
  },

  /**
   * 为商品绑定条码
   * @param {number} goodsId - 商品ID
   * @param {string} barcode - 条码
   */
  bindGoodsBarcode(goodsId, barcode) {
    return post('/barcode/goods/bind', { goodsId, barcode })
  },

  /**
   * 为库位绑定条码
   * @param {number} locationId - 库位ID
   * @param {string} barcode - 条码
   */
  bindLocationBarcode(locationId, barcode) {
    return post('/barcode/location/bind', { locationId, barcode })
  }
}
