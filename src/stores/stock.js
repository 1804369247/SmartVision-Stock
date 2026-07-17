import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { stockApi } from '../api'

export const useStockStore = defineStore('stock', () => {
  const inventory = ref([])
  const instances = ref([])
  const loading = ref(false)

  const totalQuantity = computed(() => {
    return inventory.value.reduce((sum, item) => sum + (item.quantity || 0), 0)
  })

  const totalSKU = computed(() => {
    return inventory.value.length
  })

  const fetchInventory = async () => {
    loading.value = true
    try {
      const response = await stockApi.getInventory()
      inventory.value = response.data?.data || response.data?.content || response.data || []
    } catch (error) {
      console.error('Failed to fetch inventory:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchInstances = async () => {
    loading.value = true
    try {
      const response = await stockApi.getInstances()
      instances.value = response.data?.content || response.data || []
    } catch (error) {
      console.error('Failed to fetch instances:', error)
    } finally {
      loading.value = false
    }
  }

  const refresh = async () => {
    await Promise.all([fetchInventory(), fetchInstances()])
  }

  return {
    inventory,
    instances,
    loading,
    totalQuantity,
    totalSKU,
    fetchInventory,
    fetchInstances,
    refresh
  }
})