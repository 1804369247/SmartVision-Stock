<template>
  <div class="history-panel">
    <div class="search-section">
      <el-input v-model="searchForm.goodsName" placeholder="货物名称" class="search-input" @keyup.enter="onSearch" clearable />
      <el-select v-model="searchForm.type" placeholder="类型" class="type-select" clearable @change="onSearch">
        <el-option label="全部" value="" />
        <el-option label="入库" value="IN" />
        <el-option label="出库" value="OUT" />
      </el-select>
      <el-button type="primary" @click="onSearch" :loading="loading">查询</el-button>
    </div>

    <el-table :data="records" :loading="loading" border size="small" class="records-table" empty-text="暂无出入库记录">
      <el-table-column prop="orderNo" label="单号" min-width="130" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="70" align="center">
        <template #default="{ row }">
          <el-tag :type="row.type === 'IN' ? 'success' : 'warning'" size="small" effect="dark">
            {{ row.type === 'IN' ? '入库' : '出库' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="goodsName" label="货物" min-width="90" show-overflow-tooltip />
      <el-table-column prop="quantity" label="数量" width="55" align="center" />
      <el-table-column prop="operator" label="操作员" width="65" align="center" />
      <el-table-column label="仓位" width="90" align="center">
        <template #default="{ row }">
          <span class="loc-link" @click="locateCell(row)">{{ row.toLocationCode || row.fromLocationCode || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="locateCell(row)">定位</el-button>
          <el-button size="small" link @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.currentPage"
        :page-sizes="[10, 20, 50]"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        size="small"
      />
    </div>

    <el-dialog v-model="detailVisible" title="记录详情" width="460px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="单号">{{ selectedRecord?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="selectedRecord?.type === 'IN' ? 'success' : 'warning'" size="small">
            {{ selectedRecord?.type === 'IN' ? '入库' : '出库' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="货物">{{ selectedRecord?.goodsName }}</el-descriptions-item>
        <el-descriptions-item label="批次">{{ selectedRecord?.batchNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ selectedRecord?.quantity }}</el-descriptions-item>
        <el-descriptions-item label="操作员">{{ selectedRecord?.operator }}</el-descriptions-item>
        <el-descriptions-item label="时间" :span="2">{{ formatDateTime(selectedRecord?.operateTime) }}</el-descriptions-item>
        <el-descriptions-item label="源仓位">{{ selectedRecord?.fromLocationCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目标仓位">{{ selectedRecord?.toLocationCode || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { logApi } from '../api/log'

const records = ref([])
const loading = ref(false)
const searchForm = reactive({ goodsName: '', type: '' })
const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const detailVisible = ref(false)
const selectedRecord = ref(null)

const emit = defineEmits(['locateCell'])

const formatDateTime = (dt) => {
  if (!dt) return ''
  return new Date(dt).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const loadRecords = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.currentPage - 1,
      size: pagination.pageSize
    }
    if (searchForm.goodsName) params.goodsName = searchForm.goodsName
    if (searchForm.type) params.type = searchForm.type
    const res = await logApi.getInoutRecords(params)
    if (res && res.data) {
      records.value = res.data.content || []
      pagination.total = res.data.totalElements || 0
    } else {
      records.value = res?.content || []
      pagination.total = res?.totalElements || 0
    }
  } catch (e) {
    console.error('loadRecords error:', e)
    ElMessage.error('加载记录失败，请检查后端服务')
  }
  loading.value = false
}

const onSearch = () => {
  pagination.currentPage = 1
  loadRecords()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadRecords()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadRecords()
}

const locateCell = (row) => {
  const code = row.toLocationCode || row.fromLocationCode
  if (code) emit('locateCell', code)
}

const showDetail = (row) => {
  selectedRecord.value = row
  detailVisible.value = true
}

const refresh = () => loadRecords()

defineExpose({ refresh })

onMounted(() => loadRecords())
</script>

<style scoped>
.history-panel { height: 100%; display: flex; flex-direction: column; }
.search-section { display: flex; gap: 8px; margin-bottom: 12px; }
.search-input { flex: 1; }
.type-select { width: 100px; }
.records-table { flex: 1; min-height: 0; }
.pagination-wrap { margin-top: 12px; display: flex; justify-content: center; }
.loc-link { color: #409eff; cursor: pointer; text-decoration: underline; }
.loc-link:hover { color: #66b1ff; }
</style>
