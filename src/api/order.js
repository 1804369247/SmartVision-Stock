import { get, post, put, del } from './index'

export const orderApi = {
  getInboundList(params = {}) {
    return get('/order/inbound/list', params)
  },
  
  getInboundById(id) {
    return get(`/order/inbound/${id}`)
  },
  
  createInbound(data) {
    return post('/order/inbound', data)
  },
  
  auditInbound(id) {
    return put(`/order/inbound/${id}/audit`)
  },
  
  confirmInbound(id) {
    return put(`/order/inbound/${id}/confirm`)
  },
  
  getOutboundList(params = {}) {
    return get('/order/outbound/list', params)
  },
  
  getOutboundById(id) {
    return get(`/order/outbound/${id}`)
  },
  
  createOutbound(data) {
    return post('/order/outbound', data)
  },
  
  auditOutbound(id) {
    return put(`/order/outbound/${id}/audit`)
  },
  
  pickOutbound(id) {
    return put(`/order/outbound/${id}/pick`)
  },
  
  confirmOutbound(id) {
    return put(`/order/outbound/${id}/confirm`)
  }
}