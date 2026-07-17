<template>
  <div class="help-center-page">
    <div class="page-header">
      <h3>帮助中心</h3>
      <el-input v-model="searchKeyword" placeholder="搜索帮助文档" size="small" clearable style="width: 300px;">
        <template #prefix>🔍</template>
      </el-input>
    </div>

    <div class="help-categories">
      <div
        v-for="category in categories"
        :key="category.id"
        class="category-card"
        :class="{ active: activeCategory === category.id }"
        @click="activeCategory = category.id"
      >
        <span class="category-icon">{{ category.icon }}</span>
        <span class="category-name">{{ category.name }}</span>
      </div>
    </div>

    <div class="help-content">
      <div v-if="activeCategory" class="article-list">
        <div
          v-for="article in filteredArticles"
          :key="article.id"
          class="article-item"
          @click="viewArticle(article)"
        >
          <div class="article-header">
            <span class="article-title">{{ article.title }}</span>
            <span class="article-date">{{ article.date }}</span>
          </div>
          <p class="article-preview">{{ article.preview }}</p>
        </div>
      </div>

      <div v-else class="welcome-content">
        <div class="welcome-icon">📚</div>
        <h2>欢迎来到帮助中心</h2>
        <p>请选择左侧的分类，查看相关帮助文档</p>
        <div class="quick-links">
          <el-button size="small" @click="activeCategory = 'basic'">基础操作</el-button>
          <el-button size="small" @click="activeCategory = 'inventory'">库存管理</el-button>
          <el-button size="small" @click="activeCategory = 'order'">订单管理</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="showArticle" title="帮助文档" width="700px">
      <div v-if="currentArticle" class="article-detail">
        <h2>{{ currentArticle.title }}</h2>
        <div class="article-meta">
          <span>{{ currentArticle.date }}</span>
          <span>分类：{{ currentArticle.category }}</span>
        </div>
        <div class="article-body" v-html="currentArticle.content"></div>
      </div>
      <template #footer>
        <el-button @click="showArticle = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const searchKeyword = ref('')
const activeCategory = ref('')
const showArticle = ref(false)
const currentArticle = ref(null)

const categories = [
  { id: 'basic', name: '基础操作', icon: '🔧' },
  { id: 'inventory', name: '库存管理', icon: '📦' },
  { id: 'order', name: '订单管理', icon: '📝' },
  { id: 'goods', name: '货物管理', icon: '📋' },
  { id: 'report', name: '报表查询', icon: '📊' },
  { id: 'system', name: '系统设置', icon: '⚙️' }
]

const articles = [
  {
    id: 1,
    category: 'basic',
    title: '如何登录系统',
    date: '2024-01-15',
    preview: '了解如何使用用户名和密码登录系统，以及忘记密码时的处理方法。',
    content: '<p><strong>登录系统步骤：</strong></p><ol><li>打开浏览器，访问系统登录页面</li><li>输入用户名和密码</li><li>点击登录按钮</li><li>如果登录失败，请检查用户名和密码是否正确</li></ol><p><strong>忘记密码：</strong></p><p>请联系管理员重置密码。</p>'
  },
  {
    id: 2,
    category: 'basic',
    title: '如何修改个人信息',
    date: '2024-01-14',
    preview: '了解如何修改个人信息和密码。',
    content: '<p><strong>修改个人信息：</strong></p><ol><li>点击右上角头像，选择"个人信息"</li><li>在基本信息标签页修改姓名、邮箱、手机号</li><li>点击保存修改</li></ol><p><strong>修改密码：</strong></p><ol><li>切换到修改密码标签页</li><li>输入原密码和新密码</li><li>点击修改密码</li></ol>'
  },
  {
    id: 3,
    category: 'inventory',
    title: '如何进行库存盘点',
    date: '2024-01-13',
    preview: '了解库存盘点的完整流程，包括创建盘点单、录入数据和确认结果。',
    content: '<p><strong>库存盘点流程：</strong></p><ol><li>进入库存盘点页面</li><li>点击创建盘点单，填写相关信息</li><li>点击开始盘点</li><li>录入实际盘点数量</li><li>完成盘点并确认结果</li></ol>'
  },
  {
    id: 4,
    category: 'inventory',
    title: '如何处理效期预警',
    date: '2024-01-12',
    preview: '了解如何查看和处理效期预警信息。',
    content: '<p><strong>处理效期预警：</strong></p><ol><li>进入效期预警页面</li><li>查看临期商品列表</li><li>点击处理按钮，选择处理方式</li><li>填写备注并确认</li></ol>'
  },
  {
    id: 5,
    category: 'order',
    title: '如何创建入库单',
    date: '2024-01-11',
    preview: '了解入库单的创建流程。',
    content: '<p><strong>创建入库单：</strong></p><ol><li>进入入库管理页面</li><li>点击新建入库单</li><li>填写供应商、仓库等信息</li><li>添加入库商品明细</li><li>提交审核</li></ol>'
  },
  {
    id: 6,
    category: 'order',
    title: '如何创建出库单',
    date: '2024-01-10',
    preview: '了解出库单的创建流程。',
    content: '<p><strong>创建出库单：</strong></p><ol><li>进入出库管理页面</li><li>点击新建出库单</li><li>填写客户、仓库等信息</li><li>添加出库商品明细</li><li>提交审核</li></ol>'
  },
  {
    id: 7,
    category: 'goods',
    title: '如何添加货物信息',
    date: '2024-01-09',
    preview: '了解如何添加和管理货物信息。',
    content: '<p><strong>添加货物：</strong></p><ol><li>进入货物管理页面</li><li>点击添加货物</li><li>填写货物名称、编码、规格等信息</li><li>上传货物图片（可选）</li><li>点击保存</li></ol>'
  },
  {
    id: 8,
    category: 'report',
    title: '如何查询库存报表',
    date: '2024-01-08',
    preview: '了解如何查询和导出库存报表。',
    content: '<p><strong>查询库存报表：</strong></p><ol><li>进入报表管理页面</li><li>选择报表类型</li><li>设置查询条件</li><li>点击查询</li><li>点击导出按钮导出报表</li></ol>'
  },
  {
    id: 9,
    category: 'system',
    title: '如何管理用户权限',
    date: '2024-01-07',
    preview: '了解如何管理系统用户和权限设置。',
    content: '<p><strong>用户管理：</strong></p><ol><li>进入系统设置页面</li><li>选择用户管理</li><li>可以添加、编辑、删除用户</li><li>设置用户角色和权限</li></ol>'
  }
]

const filteredArticles = computed(() => {
  let result = articles.filter(a => a.category === activeCategory.value)
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(a =>
      a.title.toLowerCase().includes(kw) ||
      a.preview.toLowerCase().includes(kw)
    )
  }
  return result
})

const viewArticle = (article) => {
  currentArticle.value = article
  showArticle.value = true
}
</script>

<style scoped>
.help-center-page { padding: 20px; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.help-categories { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.category-card {
  display: flex; align-items: center; gap: 8px; padding: 12px 20px;
  background: #f5f7fa; border-radius: 8px; cursor: pointer; transition: all 0.3s;
}
.category-card:hover { background: #eef2f7; }
.category-card.active { background: #409eff; color: #fff; }
.category-icon { font-size: 18px; }
.category-name { font-size: 14px; }
.help-content { flex: 1; }
.welcome-content { text-align: center; padding: 60px 20px; }
.welcome-icon { font-size: 64px; margin-bottom: 20px; }
.welcome-content h2 { margin-bottom: 16px; }
.welcome-content p { color: #909399; margin-bottom: 30px; }
.quick-links { display: flex; justify-content: center; gap: 12px; }
.article-list { display: flex; flex-direction: column; gap: 16px; }
.article-item {
  padding: 16px; border: 1px solid #ebeef5; border-radius: 8px;
  cursor: pointer; transition: all 0.3s;
}
.article-item:hover { border-color: #409eff; box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1); }
.article-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.article-title { font-weight: bold; }
.article-date { font-size: 12px; color: #909399; }
.article-preview { color: #606266; font-size: 14px; margin: 0; }
.article-detail { padding: 10px; }
.article-detail h2 { margin-bottom: 16px; }
.article-meta { color: #909399; font-size: 12px; margin-bottom: 20px; display: flex; gap: 20px; }
.article-body { line-height: 1.8; }
</style>