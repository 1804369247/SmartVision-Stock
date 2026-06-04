import { defineStore } from 'pinia'
import { ref } from 'vue'
import { orderApi } from '../api'

export const useOrderStore = defineStore('order', () => {
  const inboundOrders = ref([])
  const outboundOrders = ref([])
  const loading = ref(false)

  const fetchInboundOrders = async (params = {}) => {
    loading.value = true
    try {
      const result = await orderApi.getInboundList(params)
      inboundOrders.value = result.content || result
    } catch (error) {
      console.error('Failed to fetch inbound orders:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchOutboundOrders = async (params = {}) => {
    loading.value = true
    try {
      const result = await orderApi.getOutboundList(params)
      outboundOrders.value = result.content || result
    } catch (error) {
      console.error('Failed to fetch outbound orders:', error)
    } finally {
      loading.value = false
    }
  }

  const auditOrder = async (orderType, orderId) => {
    loading.value = true
    try {
      if (orderType === 'inbound') {
        await orderApi.auditInbound(orderId)
      } else {
        await orderApi.auditOutbound(orderId)
      }
      await refresh()
    } catch (error) {
      console.error('Failed to audit order:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  const confirmOrder = async (orderType, orderId) => {
    loading.value = true
    try {
      if (orderType === 'inbound') {
        await orderApi.confirmInbound(orderId)
      } else {
        await orderApi.confirmOutbound(orderId)
      }
      await refresh()
    } catch (error) {
      console.error('Failed to confirm order:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  const refresh = async () => {
    await Promise.all([fetchInboundOrders(), fetchOutboundOrders()])
  }

  return {
    inboundOrders,
    outboundOrders,
    loading,
    fetchInboundOrders,
    fetchOutboundOrders,
    auditOrder,
    confirmOrder,
    refresh
  }
})