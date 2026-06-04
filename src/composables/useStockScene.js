import { ref } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';

export function useStockScene() {
  const scene = ref(null);
  const camera = ref(null);
  const renderer = ref(null);
  const controls = ref(null);
  const raycaster = ref(new THREE.Raycaster());
  const mouse = ref(new THREE.Vector2());
  const shelves = ref([]);

  const CELL_SIZE = 0.8;
  const CELL_GAP = 0.2;
  const ZONE_SIZE = 12;
  const ZONE_GAP = 2;

  const zones = [
    { name: 'A', color: 0xff4444, offset: { x: -13, z: 0 } },
    { name: 'B', color: 0x44ff44, offset: { x: 0, z: 0 } },
    { name: 'C', color: 0x4444ff, offset: { x: 13, z: 0 } }
  ];

  const initScene = () => {
    scene.value = new THREE.Scene();
    scene.value.background = new THREE.Color(0x1a1a1a);
  };

  const initCamera = () => {
    camera.value = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 1000);
    camera.value.position.set(8, 6, 8);
    camera.value.lookAt(0, 0, 0);
  };

  const initRenderer = () => {
    renderer.value = new THREE.WebGLRenderer({ antialias: true });
    renderer.value.setSize(window.innerWidth, window.innerHeight);
    renderer.value.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.value.shadowMap.enabled = true;
  };

  const initControls = () => {
    controls.value = new OrbitControls(camera.value, renderer.value.domElement);
    controls.value.enableDamping = true;
    controls.value.dampingFactor = 0.05;
    controls.value.minDistance = 3;
    controls.value.maxDistance = 30;
    controls.value.maxPolarAngle = Math.PI / 2;
  };

  const createGround = () => {
    const groundGeometry = new THREE.PlaneGeometry(30, 30);
    const groundMaterial = new THREE.MeshStandardMaterial({ color: 0x333333, roughness: 0.8 });
    const ground = new THREE.Mesh(groundGeometry, groundMaterial);
    ground.rotation.x = -Math.PI / 2;
    ground.receiveShadow = true;
    scene.value.add(ground);

    const gridHelper = new THREE.GridHelper(30, 30, 0x444444, 0x333333);
    gridHelper.position.y = 0.01;
    scene.value.add(gridHelper);
  };

  const createZonePlane = (zone) => {
    const planeGeometry = new THREE.PlaneGeometry(ZONE_SIZE - ZONE_GAP, ZONE_SIZE - ZONE_GAP);
    const planeMaterial = new THREE.MeshBasicMaterial({ color: zone.color, transparent: true, opacity: 0.2 });
    const plane = new THREE.Mesh(planeGeometry, planeMaterial);
    plane.rotation.x = -Math.PI / 2;
    plane.position.set(zone.offset.x, 0.02, zone.offset.z);
    scene.value.add(plane);
  };

  const createCell = (zoneName, row, col, level, status = 0, locationId = null, goodsName = '', batchNo = '', quantity = 0) => {
    const geometry = new THREE.BoxGeometry(CELL_SIZE, CELL_SIZE, CELL_SIZE);
    const color = status === 1 ? 0x44ff44 : 0x666666;
    const material = new THREE.MeshStandardMaterial({ color: color, roughness: 0.3, metalness: 0.2 });
    const mesh = new THREE.Mesh(geometry, material);
    mesh.castShadow = true;
    mesh.receiveShadow = true;

    const code = zoneName + '-' + String(row + 1).padStart(2, '0') + '-' + String(col + 1).padStart(2, '0') + '-' + String(level + 1).padStart(2, '0');
    mesh.userData = { 
      code, 
      zone: zoneName, 
      row, 
      col, 
      level, 
      status,
      locationId,
      goodsName,
      batchNo,
      quantity
    };

    return { mesh, code, status, locationId };
  };

  const createShelves = () => {
    zones.forEach((zone) => {
      createZonePlane(zone);
      const zoneShelves = { zone: zone.name, cells: [] };

      const startX = zone.offset.x - (ZONE_SIZE - ZONE_GAP) / 2 + CELL_SIZE / 2 + CELL_GAP;
      const startZ = zone.offset.z - (ZONE_SIZE - ZONE_GAP) / 2 + CELL_SIZE / 2 + CELL_GAP;

      for (let row = 0; row < 4; row++) {
        for (let col = 0; col < 4; col++) {
          for (let level = 0; level < 3; level++) {
            const cell = createCell(zone.name, row, col, level);
            const x = startX + col * (CELL_SIZE + CELL_GAP);
            const y = CELL_SIZE / 2 + level * (CELL_SIZE + CELL_GAP);
            const z = startZ + row * (CELL_SIZE + CELL_GAP);
            cell.mesh.position.set(x, y, z);
            scene.value.add(cell.mesh);
            zoneShelves.cells.push(cell);
          }
        }
      }
      shelves.value.push(zoneShelves);
    });
  };

  const addLights = () => {
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    scene.value.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
    directionalLight.position.set(10, 15, 10);
    directionalLight.castShadow = true;
    scene.value.add(directionalLight);

    const pointLight = new THREE.PointLight(0xffffff, 0.5);
    pointLight.position.set(-10, 10, -10);
    scene.value.add(pointLight);
  };

  const loadLocations = async () => {
    try {
      const response = await fetch('/api/locations');
      const locations = await response.json();

      shelves.value.forEach(zoneShelves => {
        zoneShelves.cells.forEach(cell => {
          const location = locations.find(l => l.locationCode === cell.code);
          if (location) {
            cell.locationId = location.id;
            cell.status = location.status;
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
              quantity: location.quantity || 0
            };

            const color = location.status === 1 ? 0x44ff44 : 0x666666;
            cell.mesh.material.color.setHex(color);
          }
        });
      });

      return locations;
    } catch (error) {
      console.error('Failed to load locations:', error);
      return [];
    }
  };

  const updateLocationStatus = (locationId, newStatus) => {
    shelves.value.forEach(zoneShelves => {
      zoneShelves.cells.forEach(cell => {
        if (cell.locationId === locationId) {
          cell.status = newStatus;
          cell.mesh.userData.status = newStatus;
          const color = newStatus === 1 ? 0x44ff44 : 0x666666;
          cell.mesh.material.color.setHex(color);
        }
      });
    });
  };

  const dispose = () => {
    if (controls.value) {
      controls.value.dispose();
    }
    if (renderer.value) {
      renderer.value.dispose();
    }
  };

  const init = () => {
    initScene();
    initCamera();
    initRenderer();
    initControls();
    createGround();
    createShelves();
    addLights();
  };

  init();

  return { 
    scene, 
    camera, 
    renderer, 
    controls, 
    raycaster, 
    mouse, 
    shelves, 
    dispose,
    loadLocations,
    updateLocationStatus
  };
}
