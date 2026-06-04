<template>
  <div class="virtual-table-container">
    <RecycleScroller
      v-if="items.length > 0"
      :items="items"
      :item-size="rowHeight"
      class="virtual-scroller"
    >
      <template #default="{ item, index }">
        <div 
          class="table-row"
          :class="{ 'table-row-hover': hoverIndex === index }"
          @mouseenter="hoverIndex = index"
          @mouseleave="hoverIndex = -1"
          @click="$emit('row-click', item, index)"
        >
          <slot :item="item" :index="index"></slot>
        </div>
      </template>
    </RecycleScroller>
    <div v-else class="empty-state">
      <slot name="empty">暂无数据</slot>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RecycleScroller } from 'vue-virtual-scroller'

defineProps({
  items: {
    type: Array,
    default: () => []
  },
  rowHeight: {
    type: Number,
    default: 52
  }
})

defineEmits(['row-click'])

const hoverIndex = ref(-1)
</script>

<style scoped>
.virtual-table-container {
  height: 100%;
  overflow: hidden;
}

.virtual-scroller {
  height: 100%;
}

.table-row {
  display: flex;
  align-items: center;
  padding: 0 16px;
  height: 52px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: background-color 0.2s;
}

.table-row:hover,
.table-row-hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
}
</style>