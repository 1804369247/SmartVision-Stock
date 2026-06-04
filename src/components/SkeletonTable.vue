<template>
  <div class="skeleton-table">
    <div v-for="i in rowCount" :key="i" class="skeleton-row">
      <div v-for="j in colCount" :key="j" class="skeleton-cell">
        <div class="skeleton-content"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  rowCount: {
    type: Number,
    default: 5
  },
  colCount: {
    type: Number,
    default: 6
  }
})
</script>

<style scoped>
.skeleton-table {
  background: #1a1a2e;
  border-radius: 8px;
  overflow: hidden;
}

.skeleton-row {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.skeleton-row:last-child {
  border-bottom: none;
}

.skeleton-cell {
  flex: 1;
  margin-right: 16px;
}

.skeleton-cell:last-child {
  margin-right: 0;
}

.skeleton-content {
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.05) 25%,
    rgba(255, 255, 255, 0.1) 50%,
    rgba(255, 255, 255, 0.05) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s ease-in-out infinite;
  height: 20px;
  border-radius: 4px;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>