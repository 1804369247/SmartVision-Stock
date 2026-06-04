import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { locationApi } from '../api'

export const useLocationStore = defineStore('location', () => {
  const locations = ref([])
  const selectedLocation = ref(null)
  const loading = ref(false)

  const occupiedCount = computed(() => {
    return locations.value.filter(l => l.status === 1).length
  })

  const emptyCount = computed(() => {
    return locations.value.filter(l => l.status === 0).length
  })

  const utilizationRate = computed(() => {
    if (locations.value.length === 0) return 0
    return ((occupiedCount.value / locations.value.length) * 100).toFixed(1)
  })

  const fetchLocations = async () => {
    loading.value = true
    try {
      locations.value = await locationApi.getAll()
    } catch (error) {
      console.error('Failed to fetch locations:', error)
    } finally {
      loading.value = false
    }
  }

  const selectLocation = (location) => {
    selectedLocation.value = location
  }

  const clearSelection = () => {
    selectedLocation.value = null
  }

  const refresh = async () => {
    await fetchLocations()
  }

  return {
    locations,
    selectedLocation,
    loading,
    occupiedCount,
    emptyCount,
    utilizationRate,
    fetchLocations,
    selectLocation,
    clearSelection,
    refresh
  }
})