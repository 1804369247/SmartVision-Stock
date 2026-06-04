<template>
  <div class="module-page">
    <div class="page-header">
      <h3>{{ pageTitle }}</h3>
      <div class="header-actions">
        <el-button v-if="currentSubPage === 'inbound' || currentSubPage === 'outbound'" size="small" type="primary" @click="showOrderModal = true">+ 新增单据</el-button>
        <el-button size="small" @click="refreshData">🔄 刷新</el-button>
      </div>
    </div>

    <template v-if="currentSubPage === 'inbound' || currentSubPage === 'outbound'">
      <div class="form-section">
        <el-form :model="orderForm" label-width="80px" size="small">
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item :label="currentSubPage === 'inbound' ? '供应商' : '客户'">
                <el-select v-model="orderForm.supplierId" placeholder="请选择">
                  <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="备注">
                <el-input v-model="orderForm.remark" placeholder="请输入备注" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="items-section">
          <div class="items-header">
            <span>商品明细</span>
            <el-button size="small" type="primary" @click="addOrderItem">+ 添加商品</el-button>
          </div>
          
          <el-table :data="orderForm.items" border size="small">
            <el-table-column prop="goodsId" label="商品" width="200">
              <template #default="scope">
                <el-select v-model="scope.row.goodsId" placeholder="请选择商品">
                  <el-option v-for="g in goodsList" :key="g.id" :label="g.name" :value="g.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="batchNo" label="批次号" width="120">
              <template #default="scope">
                <el-input v-model="scope.row.batchNo" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80">
              <template #default="scope">
                <el-input-number v-model="scope.row.quantity" :min="1" size="small" />
              </template>
            </el-table-column>
            <el-table-column v-if="currentSubPage === 'inbound'" prop="locationId" label="目标库位" width="120">
              <template #default="scope">
                <el-select v-model="scope.row.locationId" placeholder="请选择库位">
                  <el-option v-for="l in emptyLocations" :key="l.id" :label="l.locationCode" :value="l.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column v-if="currentSubPage === 'outbound'" prop="goodsInstanceId" label="库存批次" width="120">
              <template #default="scope">
                <el-select v-model="scope.row.goodsInstanceId" placeholder="请选择批次">
                  <el-option v-for="i in goodsInstances" :key="i.id" :label="i.batchNo + '(' + i.quantity + '件)'" :value="i.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60">
              <template #default="scope">
                <el-button size="small" type="danger" @click="removeOrderItem(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="form-actions">
          <el-button type="primary" @click="submitOrder">提交单据</el-button>
          <el-button @click="resetForm">重置</el-button>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索单号" size="small" clearable @keyup.enter="search">
          <template #prefix>🔍</template>
        </el-input>
        <el-select v-model="searchStatus" placeholder="状态筛选" size="small" clearable>
          <el-option label="全部" value="" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="审核中" value="AUDITING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option v-if="currentSubPage === 'outboundList'" label="拣货中" value="PICKING" />
        </el-select>
      </div>

      <el-table :data="tableData" border size="small" class="data-table">
        <el-table-column prop="orderNo" label="单号" width="140" />
        <el-table-column prop="typeText" label="类型" width="100" />
        <el-table-column prop="supplierName" v-if="currentSubPage === 'inboundList'" label="供应商" />
        <el-table-column prop="warehouseName" label="仓库" width="100" />
        <el-table-column prop="totalQuantity" label="数量" width="80" />
        <el-table-column prop="statusText" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">{{ scope.row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150" />
        <el-table-column label="操作" width="240">
          <template #default="scope">
            <el-button size="small" @click="viewOrder(scope.row)">查看</el-button>
            <el-button size="small" type="primary" @click="printOrder(scope.row)">🖨️ 打印</el-button>
            <el-button v-if="scope.row.status === 'DRAFT'" size="small" type="success" @click="auditOrder(scope.row)">审核</el-button>
            <el-button v-if="scope.row.status === 'AUDITING'" size="small" type="warning" @click="confirmOrder(scope.row)">确认{{ currentSubPage === 'inboundList' ? '入库' : '出库' }}</el-button>
            <el-button v-if="scope.row.status === 'AUDITING' && currentSubPage === 'outboundList'" size="small" type="info" @click="pickOrder(scope.row)">拣货</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <el-dialog v-model="showOrderModal" title="查看单据" width="500px">
      <template v-if="viewingOrder">
        <div class="order-detail">
          <div class="detail-row"><span class="label">单号：</span><span>{{ viewingOrder.orderNo }}</span></div>
          <div class="detail-row"><span class="label">类型：</span><span>{{ viewingOrder.typeText }}</span></div>
          <div class="detail-row"><span class="label">状态：</span><el-tag :type="getStatusTagType(viewingOrder.status)">{{ viewingOrder.statusText }}</el-tag></div>
          <div class="detail-row"><span class="label">供应商：</span><span>{{ viewingOrder.supplierName || '-' }}</span></div>
          <div class="detail-row"><span class="label">数量：</span><span>{{ viewingOrder.totalQuantity }}</span></div>
          <div class="detail-row"><span class="label">创建时间：</span><span>{{ viewingOrder.createTime }}</span></div>
        </div>
        <div class="items-title">商品明细</div>
        <el-table :data="viewingOrder.items" border size="small">
          <el-table-column prop="goodsName" label="商品" />
          <el-table-column prop="batchNo" label="批次号" />
          <el-table-column prop="quantity" label="数量" />
          <el-table-column prop="locationCode" label="库位" />
        </el-table>
      </template>
    </el-dialog>

    <OrderPrint 
      :visible="showPrintModal"
      :order-type="currentSubPage === 'inboundList' ? 'inbound' : 'outbound'"
      :order-data="printOrderData"
      @close="showPrintModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import OrderPrint from './OrderPrint.vue'

const props = defineProps({
  subPage: { type: String, required: true }
})

const currentSubPage = computed(() => props.subPage)

const pageTitle = computed(() => {
  const titles = {
    inbound: '📥 采购入库',
    outbound: '📤 销售出库',
    inboundList: '📋 入库单查询',
    outboundList: '📋 出库单查询'
  }
  return titles[currentSubPage.value] || '管理'
})

const searchKeyword = ref('')
const searchStatus = ref('')
const tableData = ref([])
const showOrderModal = ref(false)
const viewingOrder = ref(null)

const showPrintModal = ref(false)
const printOrderData = ref({})

const suppliers = ref([])
const goodsList = ref([])
const emptyLocations = ref([])
const goodsInstances = ref([])

const orderForm = ref({
  supplierId: null,
  remark: '',
  items: []
})

const refreshData = async () => {
  try {
    if (currentSubPage.value === 'inbound' || currentSubPage.value === 'outbound') {
      const [suppRes, goodsRes, locRes, instRes] = await Promise.all([
        fetch('/api/basic/suppliers'),
        fetch('/api/goods'),
        fetch('/api/locations'),
        fetch('/api/instances')
      ])
      suppliers.value = await suppRes.json()
      goodsList.value = await goodsRes.json()
      emptyLocations.value = (await locRes.json()).filter(l => l.status === 0)
      goodsInstances.value = await instRes.json()
    } else {
      const url = currentSubPage.value === 'inboundList' ? '/api/order/inbound/list' : '/api/order/outbound/list'
      const res = await fetch(`${url}?page=0&size=50`)
      const result = await res.json()
      tableData.value = result.content
    }
  } catch (e) {
    ElMessage.error('加载数据失败')
  }
}

const search = () => {
  refreshData()
}

const addOrderItem = () => {
  orderForm.value.items.push({
    goodsId: null,
    batchNo: '',
    quantity: 1,
    locationId: null,
    goodsInstanceId: null
  })
}

const removeOrderItem = (index) => {
  orderForm.value.items.splice(index, 1)
}

const resetForm = () => {
  orderForm.value = { supplierId: null, remark: '', items: [] }
}

const submitOrder = async () => {
  if (!orderForm.value.items.length) {
    ElMessage.warning('请添加商品')
    return
  }

  const type = currentSubPage.value === 'inbound' ? 'purchase' : 'sale'
  
  try {
    const url = currentSubPage.value === 'inbound' ? '/api/order/inbound' : '/api/order/outbound'
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        type,
        supplierId: orderForm.value.supplierId,
        remark: orderForm.value.remark,
        items: orderForm.value.items
      })
    })
    
    const result = await res.json()
    if (result.success) {
      ElMessage.success('单据创建成功')
      resetForm()
    } else {
      ElMessage.error(result.message)
    }
  } catch (e) {
    ElMessage.error('提交失败')
  }
}

const viewOrder = async (order) => {
  try {
    const url = currentSubPage.value === 'inboundList' 
      ? `/api/order/inbound/${order.id}` 
      : `/api/order/outbound/${order.id}`
    const res = await fetch(url)
    const result = await res.json()
    if (result.success) {
      viewingOrder.value = result.data
      showOrderModal.value = true
    }
  } catch (e) {
    ElMessage.error('获取单据失败')
  }
}

const printOrder = async (order) => {
  try {
    const url = currentSubPage.value === 'inboundList' 
      ? `/api/order/inbound/${order.id}` 
      : `/api/order/outbound/${order.id}`
    const res = await fetch(url)
    const result = await res.json()
    if (result.success) {
      printOrderData.value = result.data
      showPrintModal.value = true
    }
  } catch (e) {
    ElMessage.error('获取单据失败')
  }
}

const auditOrder = async (order) => {
  try {
    const url = currentSubPage.value === 'inboundList' 
      ? `/api/order/inbound/${order.id}/audit` 
      : `/api/order/outbound/${order.id}/audit`
    const res = await fetch(url, { method: 'PUT' })
    const result = await res.json()
    if (result.success) {
      ElMessage.success('审核成功')
      refreshData()
    } else {
      ElMessage.error(result.message)
    }
  } catch (e) {
    ElMessage.error('审核失败')
  }
}

const pickOrder = async (order) => {
  try {
    const res = await fetch(`/api/order/outbound/${order.id}/pick`, { method: 'PUT' })
    const result = await res.json()
    if (result.success) {
      ElMessage.success('拣货成功')
      refreshData()
    } else {
      ElMessage.error(result.message)
    }
  } catch (e) {
    ElMessage.error('拣货失败')
  }
}

const confirmOrder = async (order) => {
  try {
    const url = currentSubPage.value === 'inboundList' 
      ? `/api/order/inbound/${order.id}/confirm` 
      : `/api/order/outbound/${order.id}/confirm`
    const res = await fetch(url, { method: 'PUT' })
    const result = await res.json()
    if (result.success) {
      const action = currentSubPage.value === 'inboundList' ? '入库' : '出库'
      ElMessage.success(`${action}成功，已更新3D库存模型！`)
      refreshData()
    } else {
      ElMessage.error(result.message)
    }
  } catch (e) {
    ElMessage.error(currentSubPage.value === 'inboundList' ? '入库失败' : '出库失败')
  }
}

const getStatusTagType = (status) => {
  const types = { DRAFT: 'info', AUDITING: 'warning', PICKING: 'primary', COMPLETED: 'success' }
  return types[status] || 'info'
}

onMounted(() => refreshData())
</script>

<style scoped>
.module-page { padding: 12px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-header h3 { color: #fff; font-size: 14px; margin: 0; }
.header-actions { display: flex; gap: 8px; }
.search-bar { display: flex; gap: 12px; margin-bottom: 12px; }
.search-bar .el-input { width: 180px; }
.search-bar .el-select { width: 120px; }
.data-table { flex: 1; overflow: auto; }

.form-section { background: rgba(30,30,50,0.7); border-radius: 10px; padding: 15px; }
.items-section { margin-top: 15px; }
.items-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; color: #ccc; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 15px; }

.order-detail { margin-bottom: 15px; }
.detail-row { display: flex; margin-bottom: 8px; }
.detail-row .label { color: #aaa; width: 80px; }
.items-title { color: #ccc; font-size: 13px; margin-bottom: 10px; }
</style>