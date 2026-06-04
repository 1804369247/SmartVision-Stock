<template>
  <div class="module-page">
    <div class="page-header">
      <h3>{{ pageTitle }}</h3>
      <div class="header-actions">
        <el-button size="small" type="primary" @click="showAddModal = true">+ 新增</el-button>
        <el-button size="small" @click="refreshData">🔄 刷新</el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input v-model="searchKeyword" placeholder="搜索" size="small" clearable @keyup.enter="search">
        <template #prefix>🔍</template>
      </el-input>
    </div>

    <el-table :data="tableData" border size="small" class="data-table">
      <template v-if="currentSubPage === 'goods'">
        <el-table-column prop="code" label="编码" width="100" />
        <el-table-column prop="name" label="货物名称" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="warningThreshold" label="安全库存" width="80" />
        <el-table-column prop="defaultShelfLife" label="保质期(天)" width="100" />
        <el-table-column prop="storageRule" label="存放要求" width="120" />
        <el-table-column prop="defaultSupplier" label="默认供应商" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" @click="editItem(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteItem(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </template>

      <template v-else-if="currentSubPage === 'suppliers'">
        <el-table-column prop="supplierCode" label="供应商编码" width="120" />
        <el-table-column prop="name" label="供应商名称" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="120" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" @click="editItem(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteItem(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </template>

      <template v-else-if="currentSubPage === 'locations'">
        <el-table-column prop="locationCode" label="库位编码" width="100" />
        <el-table-column prop="area" label="分区" width="60" />
        <el-table-column prop="attribute" label="属性" width="100">
          <template #default="scope">{{ getAttributeText(scope.row.attribute) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">{{ getStatusText(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column prop="goodsName" label="货物" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" @click="editItem(scope.row)">编辑</el-button>
            <el-button size="small" type="warning" @click="locateLocation(scope.row)">定位</el-button>
          </template>
        </el-table-column>
      </template>
    </el-table>

    <el-dialog v-model="showAddModal" :title="editingItem ? '编辑' : '新增'" width="450px">
      <el-form :model="formData" label-width="100px" size="small">
        <template v-if="currentSubPage === 'goods'">
          <el-form-item label="货物名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入货物名称" />
          </el-form-item>
          <el-form-item label="规格" prop="spec">
            <el-input v-model="formData.spec" placeholder="请输入规格" />
          </el-form-item>
          <el-form-item label="单位" prop="unit">
            <el-select v-model="formData.unit" placeholder="请选择单位">
              <el-option label="件" value="件" />
              <el-option label="箱" value="箱" />
              <el-option label="个" value="个" />
              <el-option label="kg" value="kg" />
              <el-option label="m" value="m" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-input v-model="formData.category" placeholder="请输入分类" />
          </el-form-item>
          <el-form-item label="安全库存" prop="warningThreshold">
            <el-input-number v-model="formData.warningThreshold" :min="0" />
          </el-form-item>
          <el-form-item label="保质期(天)" prop="defaultShelfLife">
            <el-input-number v-model="formData.defaultShelfLife" :min="0" />
          </el-form-item>
          <el-form-item label="存放要求" prop="storageRule">
            <el-select v-model="formData.storageRule" placeholder="请选择存放要求">
              <el-option label="常温" value="常温" />
              <el-option label="冷藏" value="冷藏" />
              <el-option label="冷冻" value="冷冻" />
              <el-option label="避光" value="避光" />
            </el-select>
          </el-form-item>
          <el-form-item label="默认供应商" prop="defaultSupplier">
            <el-input v-model="formData.defaultSupplier" placeholder="请输入默认供应商" />
          </el-form-item>
        </template>

        <template v-else-if="currentSubPage === 'suppliers'">
          <el-form-item label="供应商名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入供应商名称" />
          </el-form-item>
          <el-form-item label="联系人" prop="contact">
            <el-input v-model="formData.contact" placeholder="请输入联系人" />
          </el-form-item>
          <el-form-item label="电话" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入电话" />
          </el-form-item>
          <el-form-item label="地址" prop="address">
            <el-input v-model="formData.address" placeholder="请输入地址" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" placeholder="请输入备注" />
          </el-form-item>
        </template>

        <template v-else-if="currentSubPage === 'locations'">
          <el-form-item label="库位编码" prop="locationCode">
            <el-input v-model="formData.locationCode" placeholder="请输入库位编码" />
          </el-form-item>
          <el-form-item label="分区" prop="area">
            <el-select v-model="formData.area">
              <el-option label="A区" value="A" />
              <el-option label="B区" value="B" />
              <el-option label="C区" value="C" />
            </el-select>
          </el-form-item>
          <el-form-item label="属性" prop="attribute">
            <el-select v-model="formData.attribute">
              <el-option label="普通" value="NORMAL" />
              <el-option label="冷藏" value="COLD" />
              <el-option label="危险品" value="DANGEROUS" />
              <el-option label="高价值" value="VALUABLE" />
            </el-select>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="formData.description" placeholder="请输入描述" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" @click="saveItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  subPage: { type: String, required: true }
})

const emit = defineEmits(['locate'])

const currentSubPage = computed(() => props.subPage)

const pageTitle = computed(() => {
  const titles = {
    goods: '📦 货物档案管理',
    suppliers: '🏬 供应商管理',
    locations: '🏭 库位管理'
  }
  return titles[currentSubPage.value] || '管理'
})

const searchKeyword = ref('')
const tableData = ref([])
const showAddModal = ref(false)
const editingItem = ref(null)
const formData = ref({})

const refreshData = async () => {
  try {
    let url = ''
    if (currentSubPage.value === 'goods') url = '/api/goods'
    else if (currentSubPage.value === 'suppliers') url = '/api/basic/suppliers'
    else if (currentSubPage.value === 'locations') url = '/api/locations'
    
    const res = await fetch(url)
    tableData.value = await res.json()
  } catch (e) {
    ElMessage.error('加载数据失败')
  }
}

const search = () => {
  refreshData()
}

const editItem = (item) => {
  editingItem.value = item
  formData.value = { ...item }
  showAddModal.value = true
}

const deleteItem = async (item) => {
  if (!confirm(`确定删除${currentSubPage.value === 'goods' ? '货物' : currentSubPage.value === 'suppliers' ? '供应商' : '库位'}: ${item.name || item.locationCode}?`)) return
  
  try {
    let url = ''
    if (currentSubPage.value === 'goods') url = `/api/basic/goods/${item.id}`
    else if (currentSubPage.value === 'suppliers') url = `/api/basic/suppliers/${item.id}`
    
    const res = await fetch(url, { method: 'DELETE' })
    const result = await res.json()
    if (result.success) {
      ElMessage.success('删除成功')
      refreshData()
    } else {
      ElMessage.error(result.message)
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const saveItem = async () => {
  try {
    let url = ''
    let method = 'POST'
    
    if (currentSubPage.value === 'goods') {
      url = editingItem.value ? `/api/basic/goods/${editingItem.value.id}` : '/api/basic/goods'
      method = editingItem.value ? 'PUT' : 'POST'
    } else if (currentSubPage.value === 'suppliers') {
      url = editingItem.value ? `/api/basic/suppliers/${editingItem.value.id}` : '/api/basic/suppliers'
      method = editingItem.value ? 'PUT' : 'POST'
    } else if (currentSubPage.value === 'locations') {
      ElMessage.info('库位管理暂不支持新增/编辑')
      showAddModal.value = false
      return
    }
    
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData.value)
    })
    
    const result = await res.json()
    if (result.success) {
      ElMessage.success(editingItem.value ? '修改成功' : '新增成功')
      showAddModal.value = false
      editingItem.value = null
      formData.value = {}
      refreshData()
    } else {
      ElMessage.error(result.message)
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const locateLocation = (item) => {
  emit('locate', item.locationCode)
}

const getAttributeText = (attr) => {
  const map = { NORMAL: '普通', COLD: '冷藏', DANGEROUS: '危险品', VALUABLE: '高价值' }
  return map[attr] || attr
}

const getStatusText = (status) => {
  const map = { 0: '空闲', 1: '正常', 2: '预警', 3: '异常' }
  return map[status] || status
}

onMounted(() => refreshData())
</script>

<style scoped>
.module-page { padding: 12px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-header h3 { color: #fff; font-size: 14px; margin: 0; }
.header-actions { display: flex; gap: 8px; }
.search-bar { margin-bottom: 12px; }
.search-bar .el-input { width: 200px; }
.data-table { flex: 1; overflow: auto; }
</style>