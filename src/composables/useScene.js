import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

export const STATUS_COLORS = {
  EMPTY: 0x3a3a5c,      // 空库位 - 深灰蓝
  NORMAL: 0x2d8c4e,     // 正常库存 - 绿色
  WARNING: 0xe6a23c,    // 库存预警 - 橙色
  ERROR: 0xf56c6c,      // 异常库位 - 红色
  FROZEN: 0x00bfff      // 冻结库位 - 冰蓝色
}

export const STATUS_LABELS = {
  0: '空库位',
  1: '正常库存',
  2: '库存预警',
  3: '异常库位'
}

export const ATTRIBUTE_COLORS = {
  NORMAL: null,
  COLD: 0x4488ff,
  DANGEROUS: 0xff4444,
  VALUABLE: 0xffaa00
}

export const ATTRIBUTE_LABELS = {
  NORMAL: '普通库位',
  COLD: '冷藏库位',
  DANGEROUS: '危险品库位',
  VALUABLE: '高价值库位'
}

const CELL_SIZE = 0.8
const CELL_GAP = 0.2
const ZONE_SIZE = 12
const ZONE_GAP = 2

const ZONES = [
  { name: 'A', color: 0xff4444, offset: { x: -13, z: 0 } },
  { name: 'B', color: 0x44ff44, offset: { x: 0, z: 0 } },
  { name: 'C', color: 0x4444ff, offset: { x: 13, z: 0 } }
]

const CAMERA_VIEWS = {
  global: { pos: [25, 22, 25], target: [0, 0, 0] },
  zone: {
    A: { pos: [2, 12, 18], target: [-13, 2, 0] },
    B: { pos: [2, 12, 18], target: [0, 2, 0] },
    C: { pos: [2, 12, 18], target: [13, 2, 0] }
  },
  close: {
    A: { pos: [-8, 6, 5], target: [-13, 2, 0] },
    B: { pos: [5, 6, 5], target: [0, 2, 0] },
    C: { pos: [18, 6, 5], target: [13, 2, 0] }
  }
}

const getView = (viewType, zone = null) => {
  if (viewType === 'global') {
    return CAMERA_VIEWS.global
  }
  const zoneView = CAMERA_VIEWS[viewType]
  if (zoneView && zone && zoneView[zone]) {
    return zoneView[zone]
  }
  return zoneView ? zoneView.B || CAMERA_VIEWS.global : CAMERA_VIEWS.global
}

export function useScene() {
  let scene, camera, renderer, controls, raycaster, mouse
  let animationId = null
  let hoveredMesh = null
  let shelves = []
  let highlightedMeshes = []
  let highlightTimer = null  // 高亮自动清除定时器
  let geometryCache = null
  let materialCache = {}

  const getGeometry = () => {
    if (!geometryCache) {
      geometryCache = new THREE.BoxGeometry(CELL_SIZE, CELL_SIZE, CELL_SIZE)
    }
    return geometryCache
  }

  const getMaterial = (color) => {
    if (!materialCache[color]) {
      materialCache[color] = new THREE.MeshStandardMaterial({
        color,
        roughness: 0.4,
        metalness: 0.3
      })
    }
    return materialCache[color]
  }

  const getStatusColor = (status, quantity, warningThreshold = 10, frozen = false) => {
    if (frozen) return STATUS_COLORS.FROZEN
    if (status === 0) return STATUS_COLORS.EMPTY
    if (status === 3) return STATUS_COLORS.ERROR
    if (status === 1 && quantity <= warningThreshold) return STATUS_COLORS.WARNING
    if (status === 1) return STATUS_COLORS.NORMAL
    return STATUS_COLORS.EMPTY
  }

  const initScene = (container, cameraView = 'global') => {
    scene = new THREE.Scene()
    scene.background = new THREE.Color(0x0d0d12)
    scene.fog = new THREE.Fog(0x0d0d12, 25, 45)

    const view = CAMERA_VIEWS[cameraView] || CAMERA_VIEWS.global
    camera = new THREE.PerspectiveCamera(60, container.clientWidth / container.clientHeight, 0.1, 1000)
    camera.position.set(...view.pos)
    camera.lookAt(...view.target)

    renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' })
    renderer.setSize(container.clientWidth, container.clientHeight)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.shadowMap.enabled = true
    renderer.shadowMap.type = THREE.PCFSoftShadowMap

    controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.08
    controls.minDistance = 3
    controls.maxDistance = 30
    controls.maxPolarAngle = Math.PI / 2.1
    controls.rotateSpeed = 0.6
    controls.zoomSpeed = 1.2
    controls.panSpeed = 0.8

    raycaster = new THREE.Raycaster()
    mouse = new THREE.Vector2()

    createGround()
    createShelves()
    addLights()

    return renderer.domElement
  }

  const createGround = () => {
    const groundGeo = new THREE.PlaneGeometry(40, 40)
    const groundMat = new THREE.MeshStandardMaterial({ color: 0x1a1a2e, roughness: 0.9 })
    const ground = new THREE.Mesh(groundGeo, groundMat)
    ground.rotation.x = -Math.PI / 2
    ground.receiveShadow = true
    scene.add(ground)

    const grid = new THREE.GridHelper(40, 40, 0x2a2a4a, 0x1a1a2e)
    grid.position.y = 0.01
    scene.add(grid)
  }

  const createShelves = () => {
    const geo = getGeometry()

    ZONES.forEach((zone) => {
      // 区域标识平面
      const planeGeo = new THREE.PlaneGeometry(ZONE_SIZE - ZONE_GAP, ZONE_SIZE - ZONE_GAP)
      const planeMat = new THREE.MeshBasicMaterial({ color: zone.color, transparent: true, opacity: 0.12 })
      const plane = new THREE.Mesh(planeGeo, planeMat)
      plane.rotation.x = -Math.PI / 2
      plane.position.set(zone.offset.x, 0.02, zone.offset.z)
      scene.add(plane)

      // 区域标签
      const labelCanvas = document.createElement('canvas')
      labelCanvas.width = 128
      labelCanvas.height = 64
      const ctx = labelCanvas.getContext('2d')
      ctx.fillStyle = '#' + zone.color.toString(16).padStart(6, '0')
      ctx.font = 'bold 48px Arial'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(zone.name + '区', 64, 32)
      const labelTexture = new THREE.CanvasTexture(labelCanvas)
      const labelMat = new THREE.SpriteMaterial({ map: labelTexture, transparent: true, opacity: 0.7 })
      const label = new THREE.Sprite(labelMat)
      label.position.set(zone.offset.x, 0.5, zone.offset.z - (ZONE_SIZE - ZONE_GAP) / 2 - 1)
      label.scale.set(3, 1.5, 1)
      scene.add(label)

      const zoneShelves = { zone: zone.name, cells: [] }
      const startX = zone.offset.x - (ZONE_SIZE - ZONE_GAP) / 2 + CELL_SIZE / 2 + CELL_GAP
      const startZ = zone.offset.z - (ZONE_SIZE - ZONE_GAP) / 2 + CELL_SIZE / 2 + CELL_GAP

      for (let row = 0; row < 4; row++) {
        for (let col = 0; col < 4; col++) {
          for (let level = 0; level < 3; level++) {
            const code = zone.name + '-' + String(row + 1).padStart(2, '0') + '-' +
              String(col + 1).padStart(2, '0') + '-' + String(level + 1).padStart(2, '0')

            const mat = getMaterial(STATUS_COLORS.EMPTY).clone()
            const mesh = new THREE.Mesh(geo, mat)
            mesh.castShadow = true
            mesh.receiveShadow = true

            const x = startX + col * (CELL_SIZE + CELL_GAP)
            const y = CELL_SIZE / 2 + level * (CELL_SIZE + CELL_GAP)
            const z = startZ + row * (CELL_SIZE + CELL_GAP)
            mesh.position.set(x, y, z)

            mesh.userData = { code, zone: zone.name, row, col, level, status: 0, locationId: null, goodsName: '', batchNo: '', quantity: 0 }

            scene.add(mesh)
            zoneShelves.cells.push({ mesh, code, status: 0, locationId: null })
          }
        }
      }
      shelves.push(zoneShelves)
    })
  }

  const addLights = () => {
    const ambient = new THREE.AmbientLight(0xffffff, 0.5)
    scene.add(ambient)

    const dirLight = new THREE.DirectionalLight(0xffffff, 0.9)
    dirLight.position.set(15, 20, 15)
    dirLight.castShadow = true
    dirLight.shadow.mapSize.width = 1024
    dirLight.shadow.mapSize.height = 1024
    dirLight.shadow.camera.near = 0.5
    dirLight.shadow.camera.far = 50
    dirLight.shadow.camera.left = -20
    dirLight.shadow.camera.right = 20
    dirLight.shadow.camera.top = 20
    dirLight.shadow.camera.bottom = -20
    scene.add(dirLight)

    const pointLight = new THREE.PointLight(0x6699ff, 0.3)
    pointLight.position.set(-10, 15, -10)
    scene.add(pointLight)
  }

  const updateLocations = (locations, warningThreshold = 10) => {
    shelves.forEach(zoneShelves => {
      zoneShelves.cells.forEach(cell => {
        const location = locations.find(l => l.locationCode === cell.code)
        if (location) {
          cell.locationId = location.id
          cell.status = location.status
          const qty = location.quantity || 0
          const frozen = location.frozen || false
          cell.mesh.userData = {
            code: cell.code,
            zone: cell.mesh.userData.zone,
            row: cell.mesh.userData.row,
            col: cell.mesh.userData.col,
            level: cell.mesh.userData.level,
            status: location.status,
            locationId: location.id,
            goodsName: location.goodsName || '',
            batchNo: location.batchNo || '',
            quantity: qty,
            goodsInstanceId: location.currentGoodsInstanceId,
            frozen: frozen,
            frozenReason: location.frozenReason || '',
            supplier: location.supplier || '',
            expiryDate: location.expiryDate || '',
            inTime: location.inTime || '',
            attribute: location.attribute || 'NORMAL',
            description: location.description || '',
            goodsCategory: location.goodsCategory || '',
            storageRule: location.storageRule || ''
          }
          const color = getStatusColor(location.status, qty, warningThreshold, frozen)
          cell.mesh.material.color.setHex(color)
        }
      })
    })
  }

  const updateLocationStatus = (locationId, newStatus, warningThreshold = 10) => {
    shelves.forEach(zoneShelves => {
      zoneShelves.cells.forEach(cell => {
        if (String(cell.locationId) === String(locationId)) {
          cell.status = newStatus
          cell.mesh.userData.status = newStatus
          const qty = cell.mesh.userData.quantity || 0
          const frozen = cell.mesh.userData.frozen || false
          const color = getStatusColor(newStatus, qty, warningThreshold, frozen)
          cell.mesh.material.color.setHex(color)
        }
      })
    })
  }

  const highlightCell = (locationCode) => {
    clearHighlight()
    shelves.forEach(zoneShelves => {
      zoneShelves.cells.forEach(cell => {
        if (cell.code === locationCode) {
          cell.mesh.material.emissive.setHex(0xffaa00)
          cell.mesh.material.emissiveIntensity = 0.8
          highlightedMeshes.push(cell.mesh)
        }
      })
    })
    // 清除旧定时器，设置新的3秒自动清除高亮
    if (highlightTimer) clearTimeout(highlightTimer)
    highlightTimer = setTimeout(clearHighlight, 3000)
  }

  const clearHighlight = () => {
    if (highlightTimer) {
      clearTimeout(highlightTimer)
      highlightTimer = null
    }
    highlightedMeshes.forEach(mesh => {
      mesh.material.emissive.setHex(0x000000)
      mesh.material.emissiveIntensity = 0
    })
    highlightedMeshes = []
  }

  const handleMouseMove = (event, container, onHover) => {
    if (!container) return
    const rect = container.getBoundingClientRect()
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

    raycaster.setFromCamera(mouse, camera)
    const allMeshes = shelves.flatMap(s => s.cells.map(c => c.mesh))
    const intersects = raycaster.intersectObjects(allMeshes)

    if (hoveredMesh && !highlightedMeshes.includes(hoveredMesh)) {
      hoveredMesh.material.emissive.setHex(0x000000)
      hoveredMesh.material.emissiveIntensity = 0
    }
    hoveredMesh = null

    if (intersects.length > 0) {
      const mesh = intersects[0].object
      if (!highlightedMeshes.includes(mesh)) {
        mesh.material.emissive.setHex(0x4488ff)
        mesh.material.emissiveIntensity = 0.4
      }
      hoveredMesh = mesh
      if (onHover) onHover(mesh.userData, event)
    } else {
      if (onHover) onHover(null, event)
    }
  }

  const handleClick = (event, container, onClick) => {
    if (!container) return
    const rect = container.getBoundingClientRect()
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

    raycaster.setFromCamera(mouse, camera)
    const allMeshes = shelves.flatMap(s => s.cells.map(c => c.mesh))
    const intersects = raycaster.intersectObjects(allMeshes)

    if (intersects.length > 0 && onClick) {
      onClick(intersects[0].object.userData)
    }
  }

  const resetCamera = (viewType = 'global', zone = null) => {
    const view = getView(viewType, zone)
    camera.position.set(...view.pos)
    controls.target.set(...view.target)
    controls.update()
  }

  const handleResize = (container) => {
    if (!container) return
    camera.aspect = container.clientWidth / container.clientHeight
    camera.updateProjectionMatrix()
    renderer.setSize(container.clientWidth, container.clientHeight)
  }

  const animate = () => {
    animationId = requestAnimationFrame(animate)
    controls.update()
    renderer.render(scene, camera)
  }

  const startAnimate = () => { animate() }

  const dispose = () => {
    // 取消动画循环
    if (animationId) {
      cancelAnimationFrame(animationId)
      animationId = null
    }

    // 清理高亮定时器
    if (highlightTimer) {
      clearTimeout(highlightTimer)
      highlightTimer = null
    }
    clearHighlight()

    // 清理控制器
    if (controls) {
      controls.dispose()
      controls = null
    }

    // 遍历场景释放所有 Three.js 资源
    if (scene) {
      scene.traverse((obj) => {
        if (obj.geometry) {
          obj.geometry.dispose()
        }
        if (obj.material) {
          if (Array.isArray(obj.material)) {
            obj.material.forEach(m => disposeMaterial(m))
          } else {
            disposeMaterial(obj.material)
          }
        }
      })
      // 清空场景子元素
      while (scene.children.length > 0) {
        scene.remove(scene.children[0])
      }
      scene = null
    }

    // 清理渲染器
    if (renderer) {
      renderer.dispose()
      // 清理 WebGL 上下文
      const gl = renderer.domElement?.getContext?.('webgl2') || renderer.domElement?.getContext?.('webgl')
      if (gl?.getExtension('WEBGL_lose_context')) {
        gl.getExtension('WEBGL_lose_context').loseContext()
      }
      renderer.forceContextLoss?.()
      if (renderer.domElement?.parentNode) {
        renderer.domElement.parentNode.removeChild(renderer.domElement)
      }
      renderer = null
    }

    // 清理相机引用
    camera = null
    raycaster = null
    mouse = null
    hoveredMesh = null

    // 清理几何体缓存
    if (geometryCache) {
      geometryCache.dispose()
      geometryCache = null
    }

    // 清理材质缓存
    Object.values(materialCache).forEach(m => disposeMaterial(m))
    materialCache = {}

    shelves = []
    highlightedMeshes = []
  }

  // 安全释放材质（含纹理清理）
  const disposeMaterial = (mat) => {
    // 遍历材质的纹理属性并释放
    for (const key of Object.keys(mat)) {
      const value = mat[key]
      if (value?.isTexture) {
        value.dispose()
      }
    }
    mat.dispose()
  }

  const getShelves = () => shelves

  return {
    initScene,
    updateLocations,
    updateLocationStatus,
    highlightCell,
    clearHighlight,
    handleMouseMove,
    handleClick,
    resetCamera,
    handleResize,
    startAnimate,
    dispose,
    getShelves
  }
}
