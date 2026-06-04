import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { goodsApi } from '../api'

export const useGoodsStore = defineStore('goods', () => {
  const goodsList = ref([])
  const searchKeyword = ref('')
  const loading = ref(false)

  const filteredGoods = computed(() => {
    if (!searchKeyword.value) return goodsList.value
    const keyword = searchKeyword.value.toLowerCase()
    return goodsList.value.filter(item =>
      item.name.toLowerCase().includes(keyword) ||
      item.code.toLowerCase().includes(keyword) ||
      item.spec.toLowerCase().includes(keyword)
    )
  })

  const fetchGoods = async () => {
    loading.value = true
    try {
      goodsList.value = await goodsApi.getAll()
    } catch (error) {
      console.error('Failed to fetch goods:', error)
    } finally {
      loading.value = false
    }
  }

  const searchGoods = async (keyword) => {
    searchKeyword.value = keyword
    if (!keyword) {
      await fetchGoods()
    }
  }

  const refresh = async () => {
    searchKeyword.value = ''
    await fetchGoods()
  }

  return {
    goodsList,
    searchKeyword,
    loading,
    filteredGoods,
    fetchGoods,
    searchGoods,
    refresh
  }
})