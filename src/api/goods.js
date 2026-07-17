import { get, post, put, del } from './index-helpers'

const BASE = '/basic/goods'

export const goodsApi = {
  /** 获取货物列表 */
  getAll(params = {}) {
    return get(`${BASE}`, params, { cache: false })
  },

  /** 根据ID获取货物详情 */
  getById(id) {
    return get(`${BASE}/${id}`, null, { cache: false })
  },

  /** 新增货物（走 BasicController） */
  create(data) {
    return post(BASE, data)
  },

  /** 更新货物（走 BasicController） */
  update(id, data) {
    return put(`${BASE}/${id}`, data)
  },

  /** 删除货物（走 BasicController） */
  delete(id) {
    return del(`${BASE}/${id}`)
  },

  /** 搜索货物 */
  search(keyword) {
    return get(`${BASE}/search`, { keyword }, { cache: false })
  }
}
