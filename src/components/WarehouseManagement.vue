<template>
  <div class="warehouse-management">
    <el-card class="header-card">
      <div class="header">
        <h2>多仓库管理</h2>
        <el-button type="primary" @click="showCreateDialog = true">新建仓库</el-button>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ statistics.totalWarehouses || 0 }}</div>
          <div class="stat-label">仓库总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ statistics.activeWarehouses || 0 }}</div>
          <div class="stat-label">活跃仓库</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ statistics.pendingTransfers || 0 }}</div>
          <div class="stat-label">待调拨</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ statistics.totalInventoryItems || 0 }}</div>
          <div class="stat-label">库存商品</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="main-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="仓库列表" name="warehouses">
          <el-table :data="warehouses" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="code" label="仓库编码" width="120" />
            <el-table-column prop="name" label="仓库名称" />
            <el-table-column prop="address" label="地址" />
            <el-table-column prop="manager" label="负责人" width="100" />
            <el-table-column prop="phone" label="联系电话" width="120" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button size="small" @click="viewWarehouse(row.id)">查看</el-button>
                <el-button size="small" @click="editWarehouse(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteWarehouse(row.id)">删除</el-button>
                <el-button size="small" @click="viewInventory(row.id)">库存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="库存调拨" name="transfer">
          <div class="transfer-header">
            <el-button type="primary" @click="showTransferDialog = true">创建调拨</el-button>
            <el-select v-model="transferStatus" placeholder="筛选状态">
              <el-option label="全部" value="" />
              <el-option label="待审批" value="PENDING" />
              <el-option label="已完成" value="COMPLETED" />
            </el-select>
          </div>
          <el-table :data="transferRequests" style="width: 100%">
            <el-table-column prop="transferId" label="调拨单号" width="180" />
            <el-table-column label="源仓库" width="120">
              <template #default="{ row }">
                {{ getWarehouseName(row.sourceWarehouseId) }}
              </template>
            </el-table-column>
            <el-table-column label="目标仓库" width="120">
              <template #default="{ row }">
                {{ getWarehouseName(row.targetWarehouseId) }}
              </template>
            </el-table-column>
            <el-table-column prop="totalQuantity" label="数量" width="80" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button size="small" @click="viewTransferDetail(row.transferId)">详情</el-button>
                <el-button size="small" type="success" @click="approveTransfer(row.transferId)"
                           :disabled="row.status !== 'PENDING'">审批通过</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="共享库存" name="shared">
          <div class="shared-header">
            <el-input v-model="searchSku" placeholder="搜索SKU" style="width: 200px" @keyup.enter="searchInventory" />
            <el-button @click="searchInventory">搜索</el-button>
          </div>
          <el-table :data="sharedInventory" style="width: 100%">
            <el-table-column prop="sku" label="SKU" width="150" />
            <el-table-column prop="totalQuantity" label="总库存" width="100" />
            <el-table-column label="分布详情">
              <template #default="{ row }">
                <el-button size="small" @click="viewDistribution(row.sku)">查看分布</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showCreateDialog" :title="isEdit ? '编辑仓库' : '新建仓库'" width="500px">
      <el-form :model="warehouseForm" label-width="100px">
        <el-form-item label="仓库编码" prop="code">
          <el-input v-model="warehouseForm.code" />
        </el-form-item>
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="warehouseForm.name" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="warehouseForm.address" />
        </el-form-item>
        <el-form-item label="负责人" prop="manager">
          <el-input v-model="warehouseForm.manager" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="warehouseForm.phone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="saveWarehouse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTransferDialog" title="创建库存调拨" width="600px">
      <el-form :model="transferForm" label-width="100px">
        <el-form-item label="源仓库">
          <el-select v-model="transferForm.fromWarehouseId" placeholder="选择源仓库">
            <el-option v-for="wh in warehouses" :key="wh.id" :label="wh.name" :value="wh.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标仓库">
          <el-select v-model="transferForm.toWarehouseId" placeholder="选择目标仓库">
            <el-option v-for="wh in warehouses" :key="wh.id" :label="wh.name" :value="wh.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="SKU">
          <el-input v-model="transferForm.sku" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input v-model="transferForm.quantity" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTransferDialog = false">取消</el-button>
        <el-button type="primary" @click="createTransfer">创建调拨</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showInventoryDialog" title="仓库库存" width="600px">
      <el-table :data="currentInventory" style="width: 100%">
        <el-table-column prop="sku" label="SKU" />
        <el-table-column prop="quantity" label="库存数量" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="showDistributionDialog" title="库存分布" width="500px">
      <el-table :data="distributionData" style="width: 100%">
        <el-table-column prop="warehouseName" label="仓库" />
        <el-table-column prop="quantity" label="数量" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { warehouseApi } from '../api/warehouse'

const activeTab = ref('warehouses')
const warehouses = ref([])
const transferRequests = ref([])
const sharedInventory = ref([])
const statistics = ref({})

const showCreateDialog = ref(false)
const showTransferDialog = ref(false)
const showInventoryDialog = ref(false)
const showDistributionDialog = ref(false)

const isEdit = ref(false)
const warehouseForm = ref({
  id: null,
  code: '',
  name: '',
  address: '',
  manager: '',
  phone: ''
})

const transferForm = ref({
  fromWarehouseId: '',
  toWarehouseId: '',
  sku: '',
  quantity: 0
})

const transferStatus = ref('')
const searchSku = ref('')
const currentInventory = ref([])
const distributionData = ref([])

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleString()
}

const getWarehouseName = (id) => {
  const warehouse = warehouses.value.find(w => w.id === id)
  return warehouse ? warehouse.name : `仓库${id}`
}

const getStatusType = (status) => {
  const types = {
    'PENDING': 'warning',
    'COMPLETED': 'success'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'PENDING': '待审批',
    'COMPLETED': '已完成'
  }
  return texts[status] || status
}

const loadWarehouses = async () => {
  try {
    const res = await warehouseApi.getAllWarehouses()
    warehouses.value = res.data?.data || res.data?.content || res.data || []
  } catch (error) {
    warehouses.value = []
  }
}

const loadTransferRequests = async () => {
  try {
    const res = await warehouseApi.getTransferRequests(transferStatus.value)
    transferRequests.value = res.data?.data || res.data?.content || res.data || []
  } catch (error) {
    transferRequests.value = []
  }
}

const loadStatistics = async () => {
  try {
    const res = await warehouseApi.getWarehouseStatistics()
    statistics.value = res.data || {}
  } catch (error) {
    statistics.value = {}
  }
}

const saveWarehouse = async () => {
  try {
    if (isEdit.value) {
      await warehouseApi.updateWarehouse(warehouseForm.value.id, warehouseForm.value)
      ElMessage.success('仓库更新成功')
    } else {
      await warehouseApi.createWarehouse(warehouseForm.value)
      ElMessage.success('仓库创建成功')
    }
    showCreateDialog.value = false
    resetForm()
    loadWarehouses()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const resetForm = () => {
  warehouseForm.value = {
    id: null,
    code: '',
    name: '',
    address: '',
    manager: '',
    phone: ''
  }
  isEdit.value = false
}

const editWarehouse = (row) => {
  isEdit.value = true
  warehouseForm.value = { ...row }
  showCreateDialog.value = true
}

const deleteWarehouse = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该仓库吗？', {
      title: '提示',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await warehouseApi.deleteWarehouse(id)
    ElMessage.success('删除成功')
    loadWarehouses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const viewWarehouse = (id) => {
  ElMessage.info(`查看仓库 ${id}`)
}

const viewInventory = async (warehouseId) => {
  try {
    const res = await warehouseApi.getWarehouseInventory(warehouseId)
    currentInventory.value = res.data?.inventory || []
  } catch (error) {
    currentInventory.value = []
  }
  showInventoryDialog.value = true
}

const createTransfer = async () => {
  if (!transferForm.value.fromWarehouseId || !transferForm.value.toWarehouseId) {
    ElMessage.warning('请选择仓库')
    return
  }
  if (!transferForm.value.sku || transferForm.value.quantity <= 0) {
    ElMessage.warning('请填写SKU和数量')
    return
  }
  try {
    await warehouseApi.transferStock({
      sku: transferForm.value.sku,
      quantity: transferForm.value.quantity,
      fromWarehouseId: transferForm.value.fromWarehouseId,
      toWarehouseId: transferForm.value.toWarehouseId
    })
    ElMessage.success('调拨创建成功')
    showTransferDialog.value = false
    transferForm.value = { fromWarehouseId: '', toWarehouseId: '', sku: '', quantity: 0 }
    loadTransferRequests()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const approveTransfer = async (transferId) => {
  try {
    await warehouseApi.approveTransfer(transferId)
    ElMessage.success('审批通过')
    loadTransferRequests()
  } catch (error) {
    ElMessage.error('审批失败')
  }
}

const viewTransferDetail = (transferId) => {
  ElMessage.info(`查看调拨 ${transferId}`)
}

const searchInventory = async () => {
  if (!searchSku.value) {
    sharedInventory.value = []
    return
  }
  try {
    const res = await warehouseApi.getSharedInventory(searchSku.value)
    sharedInventory.value = res.data ? [res.data] : []
  } catch (error) {
    sharedInventory.value = []
  }
}

const viewDistribution = (sku) => {
  try {
    warehouseApi.getSharedInventory(sku).then(res => {
      const data = res.data
      if (data && data.distribution) {
        distributionData.value = data.distribution.map(d => ({
          warehouseName: getWarehouseName(d.warehouseId),
          quantity: d.quantity
        }))
      } else {
        distributionData.value = []
      }
      showDistributionDialog.value = true
    })
  } catch (error) {
    distributionData.value = []
    showDistributionDialog.value = true
  }
}

onMounted(() => {
  loadWarehouses()
  loadTransferRequests()
  loadStatistics()
})
</script>

<style scoped>
.warehouse-management {
  padding: 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.main-card {
  margin-top: 20px;
}

.transfer-header, .shared-header {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  align-items: center;
}
</style>