<template>
  <div ref="containerRef" class="scene-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, inject } from 'vue'
import { useStockScene } from '../composables/useStockScene'

const containerRef = ref(null)
const tooltip = inject('tooltip')

const { scene, camera, renderer, controls, raycaster, mouse, shelves, dispose } = useStockScene()

let animationId = null
let hoveredMesh = null

const handleMouseMove = (event) => {
  const rect = containerRef.value.getBoundingClientRect()
  mouse.value.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.value.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.value.setFromCamera(mouse.value, camera.value)

  const allMeshes = []
  shelves.value.forEach(shelf => {
    shelf.cells.forEach(cell => {
      allMeshes.push(cell.mesh)
    })
  })

  const intersects = raycaster.value.intersectObjects(allMeshes)

  if (hoveredMesh) {
    hoveredMesh.material.emissive.setHex(0x000000)
    hoveredMesh.material.emissiveIntensity = 0
    hoveredMesh = null
  }

  if (intersects.length > 0) {
    const intersectedMesh = intersects[0].object
    hoveredMesh = intersectedMesh
    hoveredMesh.material.emissive.setHex(0xffffff)
    hoveredMesh.material.emissiveIntensity = 0.5

    tooltip.value.visible = true
    tooltip.value.x = event.clientX
    tooltip.value.y = event.clientY
    tooltip.value.text = intersectedMesh.userData.code
  } else {
    tooltip.value.visible = false
  }
}

const handleResize = () => {
  const width = window.innerWidth
  const height = window.innerHeight
  camera.value.aspect = width / height
  camera.value.updateProjectionMatrix()
  renderer.value.setSize(width, height)
}

const animate = () => {
  animationId = requestAnimationFrame(animate)
  controls.value.update()
  renderer.value.render(scene.value, camera.value)
}

onMounted(() => {
  if (containerRef.value) {
    containerRef.value.appendChild(renderer.value.domElement)
    renderer.value.setSize(window.innerWidth, window.innerHeight)
    animate()

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('resize', handleResize)
  dispose()
})
</script>

<style>
.scene-container {
  width: 100%;
  height: 100%;
}
</style>
