# SmartVision Stock 前端代码审查报告

> 审查范围：src/ 目录下所有前端代码
> 审查日期：2026-06-26
> 审查维度：逻辑错误、边界情况、错误处理、异步竞态、内存泄漏、XSS/安全、性能、硬编码 Magic Number、死代码

---

## 一、高危问题（建议立即修复）

### 1. src/components/OrderManagement.vue — 响应式引用丢失 .value

- **位置**：第 469 行、第 514 行、第 518 行
- **问题**：currentSubPage 是 computed 返回的 ref 对象，在 script setup 中模板外访问必须使用 .value。直接使用 currentSubPage 进行相等判断会导致条件永远为 false（因为比较的是对象引用），进而导致 api 和 action 选择错误，引发严重业务逻辑错误。
- **影响**：printOrder、confirmOrder 两个核心操作会调用错误的 API 端点，出库/入库确认逻辑颠倒。
- **修复**：统一改为 currentSubPage.value。

### 2. src/components/OrderPrint.vue — document.body.innerHTML 篡改导致应用状态丢失

- **位置**：第 343–364 行
- **问题**：直接替换 document.body.innerHTML 会销毁 Vue 应用挂载的 DOM 和事件监听，导致应用状态完全丢失。使用 window.location.reload() 强制刷新页面，在 SPA 中极其粗暴，用户体验差且会丢失未保存状态。如果打印过程中用户切换标签页或关闭窗口，setTimeout 回调可能无法执行，导致页面空白。
- **修复**：使用 iframe 或新窗口打开打印内容，避免修改当前 body。

### 3. src/components/PrintManager.vue — window.open + document.write 组合存在 XSS 风险与内存泄漏

- **位置**：第 811 行、第 985 行
- **问题**：document.write 的内容如果包含用户可控数据（如打印模板中的商品名称、备注），存在反射型 XSS 风险。新窗口打开后没有关闭引用，如果打印失败可能导致内存泄漏。第 985 行未判断 win 是否为 null（弹窗被浏览器拦截时）。
- **修复**：对用户输入进行 HTML 转义；在 document.write 前检查 win 存在性；打印完成后关闭窗口。

### 4. src/components/ReportCenter.vue — window.open 直接打开后端 URL 未校验

- **位置**：第 351 行
- **问题**：url 来自后端 API 返回，如果后端被攻破返回恶意 URL，用户会被诱导打开钓鱼网站。且未检查 url 是否为空或合法。
- **修复**：校验 URL 是否为同源或白名单域名，或强制使用相对路径下载。

