<template>
  <div class="stock-count-page">
    <div class="page-header">
      <h3>库存盘点</h3>
      <div class="header-actions">
        <el-button size="small" type="primary" @click="showCreateDialog = true">➕ 创建盘点单</el-button>
        <el-button size="small" @click="loadData">🔄 刷新</el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input v-model="searchKeyword" placeholder="搜索盘点单号/仓库" size="small" clearable @keyup.enter="loadData">
        <template #prefix>🔍</template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态筛选" size="small" style="margin-left: 8px;">
        <el-option label="全部" value="" />
        <el-option label="待盘点" :value="0" />
        <el-option label="盘点中" :value="1" />
        <el-option label="已完成" :value="2" />
        <el-option label="已确认" :value="3" />
      </el-select>
    </div>

    <el-table :data="tableData" border size="small" class="data-table">
      <el-table-column prop="countNo" label="盘点单号" width="140" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="area" label="区域" width="100" />
      <el-table-column prop="countType" label="盘点类型" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.countType === 'FULL' ? 'warning' : 'info'" size="small">
            {{ scope.row.countType === 'FULL' ? '全盘' : '抽盘' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)" size="small">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalCount" label="总项数" width="80" />
      <el-table-column prop="completedCount" label="已完成" width="80" />
      <el-table-column prop="operatorName" label="操作人" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="viewDetail(scope.row)">详情</el-button>
          <el-button v-if="scope.row.status === 0" size="small" type="primary" @click="startCount(scope.row.id)">开始盘点</el-button>
          <el-button v-if="scope.row.status === 1" size="small" type="warning" @click="openEditDialog(scope.row)">录入数据</el-button>
          <el-button v-if="scope.row.status === 1" size="small" type="success" @click="completeCount(scope.row.id)">完成盘点</el-button>
          <el-button v-if="scope.row.status === 2" size="small" type="primary" @click="confirmCount(scope.row.id)">确认结果</el-button>
          <el-button v-if="scope.row.status === 0" size="small" type="danger" @click="deleteCount(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="totalElements > 0"
      :current-page="pagination.page + 1"
      :page-size="pagination.size"
      :total="totalElements"
      @current-change="handlePageChange"
      layout="prev, pager, next, jumper, ->, total"
      class="pagination"
    />

    <el-dialog v-model="showCreateDialog" title="创建盘点单" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="仓库名称" required>
          <el-input v-model="createForm.warehouseName" placeholder="请输入仓库名称" />
        </el-form-item>
        <el-form-item label="仓库ID">
          <el-input-number v-model="createForm.warehouseId" :min="0" />
        </el-form-item>
        <el-form-item label="区域">
          <el-input v-model="createForm.area" placeholder="可选，为空则盘点整个仓库" />
        </el-form-item>
        <el-form-item label="盘点类型" required>
          <el-select v-model="createForm.countType">
            <el-option label="全盘" value="FULL" />
            <el-option label="抽盘" value="SPOT" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人" required>
          <el-input v-model="createForm.operatorName" placeholder="请输入操作人姓名" />
        </el-form-item>
        <el-form-item label="操作人ID">
          <el-input-number v-model="createForm.operatorId" :min="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createCount">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="盘点详情" width="800px">
      <div class="detail-header-info">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="盘点单号">{{ detailData.countNo }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ detailData.warehouseName }}</el-descriptions-item>
          <el-descriptions-item label="区域">{{ detailData.area || '全部' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detailData.countType === 'FULL' ? '全盘' : '抽盘' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(detailData.status)" size="small">
              {{ getStatusText(detailData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总项数">{{ detailData.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="已完成">{{ detailData.completedCount }}</el-descriptions-item>
          <el-descriptions-item label="差异数量">{{ detailData.diffCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detailData.operatorName }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(detailData.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDate(detailData.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDate(detailData.completeTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div v-if="detailItems.length > 0" style="margin-top: 20px;">
        <h4>盘点明细</h4>
        <el-table :data="detailItems" border size="small" max-height="400">
          <el-table-column prop="locationCode" label="库位" width="100" />
          <el-table-column prop="area" label="区域" width="80" />
          <el-table-column prop="goodsCode" label="货物编码" width="120" />
          <el-table-column prop="goodsName" label="货物名称" min-width="150" />
          <el-table-column prop="batchNo" label="批次号" width="130" />
          <el-table-column prop="expectedQty" label="账面数量" width="100" />
          <el-table-column prop="actualQty" label="实际数量" width="100">
            <template #default="scope">
              <span v-if="scope.row.actualQty !== null">{{ scope.row.actualQty }}</span>
              <span v-else class="text-gray">未录入</span>
            </template>
          </el-table-column>
          <el-table-column prop="diffQty" label="差异数量" width="100">
            <template #default="scope">
              <span :class="{ 'text-danger': scope.row.diffQty !== 0 && scope.row.diffQty !== null }">
                {{ scope.row.diffQty !== null ? scope.row.diffQty : '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'COMPLETED' ? 'success' : 'info'" size="small">
                {{ scope.row.status === 'COMPLETED' ? '已完成' : '待盘点' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="录入盘点数据" width="800px">
      <el-table :data="editItemsData" border size="small" max-height="500">
        <el-table-column prop="locationCode" label="库位" width="100" />
        <el-table-column prop="goodsCode" label="货物编码" width="120" />
        <el-table-column prop="goodsName" label="货物名称" min-width="150" />
        <el-table-column prop="expectedQty" label="账面数量" width="100" />
        <el-table-column prop="actualQty" label="实际数量" width="100">
          <template #default="scope">
            <el-input-number v-model="scope.row.actualQty" :min="0" size="small" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveItems">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { stockCountApi } from '../api'

const searchKeyword = ref('')
const filterStatus = ref('')
const tableData = ref([])
const pagination = reactive({ page: 0, size: 10 })
const totalElements = ref(0)

const showCreateDialog = ref(false)
const createForm = reactive({
  warehouseName: '',
  warehouseId: null,
  area: '',
  countType: 'FULL',
  operatorName: '',
  operatorId: null,
  remark: ''
})

const showDetailDialog = ref(false)
const detailData = ref({})
const detailItems = ref([])

const showEditDialog = ref(false)
const editItemsData = ref([])
const currentCountId = ref(null)

const loadData = async () => {
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value !== '') params.status = filterStatus.value

    const result = await stockCountApi.getList(params)
    const data = result.data || result
    tableData.value = data.content || data
    totalElements.value = data.totalElements || data.length || 0
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const handlePageChange = (page) => {
  pagination.page = page - 1
  loadData()
}

const getStatusText = (status) => {
  const texts = ['待盘点', '盘点中', '已完成', '已确认']
  return texts[status] || '未知'
}

const getStatusTagType = (status) => {
  const types = ['info', 'warning', 'success', 'primary']
  return types[status] || 'info'
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const createCount = async () => {
  if (!createForm.warehouseName || !createForm.operatorName) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    await stockCountApi.create(createForm)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

const viewDetail = async (row) => {
  try {
    const result = await stockCountApi.get(row.id)
    const data = result.data || result
    detailData.value = data.count || data
    detailItems.value = data.items || []
    showDetailDialog.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const startCount = async (id) => {
  try {
    await stockCountApi.start(id)
    ElMessage.success('开始盘点')
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const openEditDialog = async (row) => {
  try {
    const result = await stockCountApi.get(row.id)
    const data = result.data || result
    editItemsData.value = (data.items || []).map(item => ({ ...item }))
    currentCountId.value = row.id
    showEditDialog.value = true
  } catch (e) {
    ElMessage.error('获取数据失败')
  }
}

const saveItems = async () => {
  try {
    await stockCountApi.updateItems(currentCountId.value, editItemsData.value)
    ElMessage.success('保存成功')
    showEditDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const completeCount = async (id) => {
  try {
    await ElMessageBox.confirm('确定要完成盘点吗？', '提示', { type: 'warning' })
    await stockCountApi.complete(id)
    ElMessage.success('盘点完成')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const confirmCount = async (id) => {
  try {
    await ElMessageBox.confirm('确定要确认盘点结果吗？确认后将更新库存数据。', '提示', { type: 'warning' })
    await stockCountApi.confirm(id)
    ElMessage.success('确认成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const deleteCount = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个盘点单吗？', '提示', { type: 'warning' })
    await stockCountApi.delete(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.stock-count-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-actions { display: flex; gap: 8px; }
.search-bar { display: flex; margin-bottom: 16px; }
.data-table { margin-bottom: 16px; }
.pagination { text-align: right; }
.text-gray { color: #909399; }
.text-danger { color: #f56c6c; }
.detail-header-info { margin-bottom: 20px; }
</style>