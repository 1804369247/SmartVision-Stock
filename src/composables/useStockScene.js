import { shallowRef, watch } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { get } from '../api/index-helpers';

export function useStockScene(viewMode, viewZone) {
  const scene = shallowRef(null);
  const camera = shallowRef(null);
  const renderer = shallowRef(null);
  const controls = shallowRef(null);
  const raycaster = shallowRef(new THREE.Raycaster());
  const mouse = shallowRef(new THREE.Vector2());
  const shelves = shallowRef([]);

  // ========== 尺寸配置 ==========
  const CELL_W = 1.0;
  const CELL_D = 1.0;
  const CELL_H = 0.65;
  const CELL_GAP = 0.12;
  const SHELF_POST = 0.05;

  // 分区配置（baseColor 仅用于底板/标签装饰，避免与状态色冲突）
  const zones = [
    { name: 'A', label: 'A 区 - 普通货物', baseColor: 0x1890ff, offset: { x: -13, z: 0 }, rows: 3, cols: 4, levels: 4 },
    { name: 'B', label: 'B 区 - 特殊存储', baseColor: 0xfa8c16, offset: { x: 0, z: 0 }, rows: 3, cols: 4, levels: 4 },
    { name: 'C', label: 'C 区 - 备用区域', baseColor: 0xeb2f96, offset: { x: 13, z: 0 }, rows: 3, cols: 4, levels: 4 },
  ];

  // 状态颜色映射
  const STATUS_COLORS = {
    empty:    { color: 0x8c9ab0, emissive: 0x000000 },
    occupied: { color: 0x36c369, emissive: 0x1a8040 },
    warning:  { color: 0xfaad14, emissive: 0xd48806 },
    danger:   { color: 0xff4d4f, emissive: 0xcf1322 },
  };

  // ========== 爆炸展开状态 ==========
  let isExploded = false;
  let explodedZoneName = null;
  // 存储每个货架组的原始位置和库位的原始位置（用于动画恢复）
  let groupOriginalPositions = new Map();  // group -> { x, y, z }
  let cellOriginalPositions = new Map();   // mesh -> { x, y, z }
  let cellLabels = [];                      // 库位标签精灵数组
  let cellLabelGroup = null;               // 存放所有标签的组

  // ==================== 初始化 ====================

  const initScene = () => {
    scene.value = new THREE.Scene();
    scene.value.background = new THREE.Color(0xd8dde8);
    scene.value.fog = new THREE.Fog(0xd8dde8, 45, 70);
  };

  const initCamera = () => {
    camera.value = new THREE.PerspectiveCamera(
      50,
      window.innerWidth / window.innerHeight,
      0.1,
      500
    );
    camera.value.position.set(16, 11, 16);
    camera.value.lookAt(0, 1.5, 0);
  };

  const initRenderer = () => {
    renderer.value = new THREE.WebGLRenderer({
      antialias: true,
      alpha: false,
      powerPreference: 'high-performance',
    });
    renderer.value.setSize(window.innerWidth, window.innerHeight);
    renderer.value.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.value.shadowMap.enabled = true;
    renderer.value.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.value.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.value.toneMappingExposure = 1.0;
    renderer.value.outputColorSpace = THREE.SRGBColorSpace;
  };

  const initControls = () => {
    controls.value = new OrbitControls(camera.value, renderer.value.domElement);
    controls.value.enableDamping = true;
    controls.value.dampingFactor = 0.08;
    controls.value.minDistance = 3;
    controls.value.maxDistance = 50;
    controls.value.maxPolarAngle = Math.PI / 2.05;
    controls.value.minPolarAngle = Math.PI * 0.08;
    controls.value.target.set(0, 1.5, 0);
    controls.value.update();
  };

  // ==================== 光照系统 ====================

  const addLights = () => {
    const ambient = new THREE.AmbientLight(0xffffff, 0.7);
    scene.value.add(ambient);

    const mainLight = new THREE.DirectionalLight(0xfffaf0, 1.2);
    mainLight.position.set(8, 18, 8);
    mainLight.castShadow = true;
    mainLight.shadow.mapSize.width = 2048;
    mainLight.shadow.mapSize.height = 2048;
    mainLight.shadow.camera.near = 1;
    mainLight.shadow.camera.far = 50;
    mainLight.shadow.camera.left = -25;
    mainLight.shadow.camera.right = 25;
    mainLight.shadow.camera.top = 18;
    mainLight.shadow.camera.bottom = -8;
    mainLight.shadow.bias = -0.0003;
    scene.value.add(mainLight);

    const fillLight = new THREE.DirectionalLight(0xe8f0ff, 0.5);
    fillLight.position.set(-12, 10, -6);
    scene.value.add(fillLight);

    const backLight = new THREE.DirectionalLight(0xfff5e6, 0.3);
    backLight.position.set(10, 8, -10);
    scene.value.add(backLight);

    const hemiLight = new THREE.HemisphereLight(0xffffff, 0xcccccc, 0.45);
    scene.value.add(hemiLight);

    zones.forEach(zone => {
      const point = new THREE.PointLight(zone.baseColor, 0.25, 12);
      point.position.set(zone.offset.x, 5, zone.offset.z);
      scene.value.add(point);
    });
  };

  // ==================== 场景元素 ====================

  /** 地面 + 网格 */
  const createGround = () => {
    const groundGeo = new THREE.PlaneGeometry(60, 40);
    const groundMat = new THREE.MeshStandardMaterial({
      color: 0xc8cdd8,
      roughness: 0.85,
      metalness: 0.02,
    });
    const ground = new THREE.Mesh(groundGeo, groundMat);
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.01;
    ground.receiveShadow = true;
    scene.value.add(ground);

    const gridHelper = new THREE.GridHelper(60, 60, 0x8899aa, 0xb0bcc8);
    gridHelper.position.y = 0.002;
    scene.value.add(gridHelper);

    zones.forEach(zone => {
      const hw = (zone.cols * (CELL_W + CELL_GAP)) / 2 + 0.8;
      const hd = (zone.rows * (CELL_D + CELL_GAP)) / 2 + 0.8;
      const corners = [
        [zone.offset.x - hw, zone.offset.z - hd],
        [zone.offset.x + hw, zone.offset.z - hd],
        [zone.offset.x + hw, zone.offset.z + hd],
        [zone.offset.x - hw, zone.offset.z + hd],
        [zone.offset.x - hw, zone.offset.z - hd],
      ];
      const lineGeo = new THREE.BufferGeometry();
      lineGeo.setFromPoints(corners.map(c => new THREE.Vector3(c[0], 0.02, c[1])));
      const lineMat = new THREE.LineBasicMaterial({ color: zone.baseColor, transparent: true, opacity: 0.5 });
      scene.value.add(new THREE.Line(lineGeo, lineMat));
    });
  };

  /** 区域地面色块 */
  const createZoneFloor = (zone) => {
    const w = zone.cols * (CELL_W + CELL_GAP) + CELL_GAP * 2;
    const d = zone.rows * (CELL_D + CELL_GAP) + CELL_GAP * 2;
    const floorGeo = new THREE.PlaneGeometry(w, d);
    const floorMat = new THREE.MeshStandardMaterial({
      color: zone.baseColor,
      transparent: true,
      opacity: 0.10,
      roughness: 0.9,
    });
    const floorMesh = new THREE.Mesh(floorGeo, floorMat);
    floorMesh.rotation.x = -Math.PI / 2;
    floorMesh.position.set(zone.offset.x, 0.003, zone.offset.z);
    floorMesh.receiveShadow = true;
    scene.value.add(floorMesh);
  };

  /**
   * 创建单个货架组 — 返回 group，内部包含立柱、层板、库位盒子
   */
  const createShelfUnit = (zone, rowIdx, colIdx) => {
    const group = new THREE.Group();
    const xBase = zone.offset.x;
    const zBase = zone.offset.z;

    const shelfW = CELL_W + CELL_GAP;
    const shelfD = CELL_D + CELL_GAP;
    const totalW = zone.cols * shelfW;
    const totalD = zone.rows * shelfD;
    const startX = xBase - totalW / 2 + shelfW / 2;
    const startZ = zBase - totalD / 2 + shelfD / 2;

    const ux = startX + colIdx * shelfW;
    const uz = startZ + rowIdx * shelfD;

    // 立柱
    const postH = zone.levels * (CELL_H + CELL_GAP) + 0.25;
    const postGeo = new THREE.BoxGeometry(SHELF_POST, postH, SHELF_POST);
    const postMat = new THREE.MeshStandardMaterial({
      color: 0x7a8899,
      roughness: 0.35,
      metalness: 0.5,
    });

    const halfW = CELL_W / 2 - SHELF_POST / 2 - 0.01;
    const halfD = CELL_D / 2 - SHELF_POST / 2 - 0.01;
    const postPositions = [
      [ux - halfW, postH / 2, uz - halfD],
      [ux + halfW, postH / 2, uz - halfD],
      [ux - halfW, postH / 2, uz + halfD],
      [ux + halfW, postH / 2, uz + halfD],
    ];

    postPositions.forEach(pos => {
      const post = new THREE.Mesh(postGeo, postMat);
      post.position.set(pos[0], pos[1], pos[2]);
      post.castShadow = true;
      group.add(post);
    });

    // 层板 + 库位盒子
    for (let lv = 0; lv < zone.levels; lv++) {
      const y = CELL_H / 2 + lv * (CELL_H + CELL_GAP);
      const slabGeo = new THREE.BoxGeometry(CELL_W + 0.03, 0.025, CELL_D + 0.03);
      const slabMat = new THREE.MeshStandardMaterial({
        color: 0xa0aab8,
        roughness: 0.65,
        metalness: 0.1,
      });
      const slab = new THREE.Mesh(slabGeo, slabMat);
      slab.position.set(ux, y - CELL_H / 2 - 0.015, uz);
      slab.receiveShadow = true;
      slab.castShadow = true;
      group.add(slab);

      const cell = createCell(zone.name, rowIdx, colIdx, lv);
      cell.mesh.position.set(ux, y, uz);
      group.add(cell.mesh);
    }

    // 顶部横梁框架
    const topY = postH + 0.12;
    const beamGeoW = new THREE.BoxGeometry(CELL_W + 0.03, SHELF_POST, SHELF_POST);
    const beamGeoD = new THREE.BoxGeometry(SHELF_POST, SHELF_POST, CELL_D + 0.03);

    [halfD, -halfD].forEach(dz => {
      const beam = new THREE.Mesh(beamGeoW, postMat);
      beam.position.set(ux, topY, uz + dz);
      beam.castShadow = true;
      group.add(beam);
    });
    [-halfW, halfW].forEach(dx => {
      const beam = new THREE.Mesh(beamGeoD, postMat);
      beam.position.set(ux + dx, topY, uz);
      beam.castShadow = true;
      group.add(beam);
    });

    // 记录原始位置
    groupOriginalPositions.set(group, { x: 0, y: 0, z: 0 });

    return group;
  };

  /** 创建库位盒子 */
  const createCell = (zoneName, row, col, level, status = 0, locationId = null, goodsName = '', quantity = 0) => {
    const geo = new THREE.BoxGeometry(CELL_W * 0.82, CELL_H * 0.84, CELL_D * 0.82);
    const statusKey = getStatusKey(status, quantity);
    const palette = STATUS_COLORS[statusKey];

    const mat = new THREE.MeshStandardMaterial({
      color: palette.color,
      roughness: 0.4,
      metalness: 0.1,
      emissive: palette.emissive,
      emissiveIntensity: status !== 0 ? 0.35 : 0,
    });

    const mesh = new THREE.Mesh(geo, mat);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    mesh.position.y += 0.018;

    const code = `${zoneName}-${String(row + 1).padStart(2, '0')}-${String(col + 1).padStart(2, '0')}-${String(level + 1).padStart(2, '0')}`;
    mesh.userData = {
      code,
      zone: zoneName,
      row, col, level,
      status,
      locationId,
      goodsName,
      quantity,
      isCell: true,
    };

    // 记录原始世界位置（将在 add 到场景后更新）
    cellOriginalPositions.set(mesh, { x: 0, y: 0, z: 0 });

    return { mesh, code, status, locationId };
  };

  /** 状态判定 */
  const getStatusKey = (status, qty) => {
    if (status === 0) return 'empty';
    if (qty > 0 && qty <= 5) return 'warning';
    if (qty > 100) return 'danger';
    return 'occupied';
  };

  /** 创建所有货架 */
  const createShelves = () => {
    zones.forEach(zone => {
      createZoneFloor(zone);
      const zoneData = { zone: zone.name, label: zone.label, cells: [], groups: [], baseColor: zone.baseColor, offset: zone.offset };

      for (let r = 0; r < zone.rows; r++) {
        for (let c = 0; c < zone.cols; c++) {
          const shelfGroup = createShelfUnit(zone, r, c);
          scene.value.add(shelfGroup);
          zoneData.groups.push(shelfGroup);

          shelfGroup.children.forEach(child => {
            if (child.userData?.isCell) {
              // 更新库位的原始世界位置
              const wp = new THREE.Vector3();
              child.getWorldPosition(wp);
              cellOriginalPositions.set(child, { x: wp.x, y: wp.y, z: wp.z });

              zoneData.cells.push({
                mesh: child,
                code: child.userData.code,
                status: child.userData.status,
                locationId: null,
              });
            }
          });
        }
      }

      shelves.value.push(zoneData);
    });
  };

  /** 区域标签 */
  const createZoneLabels = () => {
    zones.forEach(zone => {
      const canvas = document.createElement('canvas');
      canvas.width = 256;
      canvas.height = 76;
      const ctx = canvas.getContext('2d');

      ctx.fillStyle = 'rgba(255,255,255,0.92)';
      roundRect(ctx, 4, 4, 248, 68, 8);
      ctx.fill();

      const grd = ctx.createLinearGradient(0, 0, 4, 0);
      grd.addColorStop(0, '#' + zone.baseColor.toString(16).padStart(6, '0'));
      grd.addColorStop(1, '#' + zone.baseColor.toString(16).padStart(6, '0'));
      ctx.fillStyle = grd;
      roundRect(ctx, 4, 4, 6, 68, 8);
      ctx.fill();

      ctx.fillStyle = '#1a1a2e';
      ctx.font = 'bold 22px "Microsoft YaHei", sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(zone.label, 130, 30);

      ctx.fillStyle = '#666';
      ctx.font = '12px "Microsoft YaHei", sans-serif';
      ctx.fillText(`${zone.rows * zone.cols * zone.levels} 个库位`, 130, 54);

      const texture = new THREE.CanvasTexture(canvas);
      texture.needsUpdate = true;
      const spriteMat = new THREE.SpriteMaterial({
        map: texture,
        transparent: true,
        depthTest: false,
      });
      const sprite = new THREE.Sprite(spriteMat);
      sprite.scale.set(3.8, 1.15, 1);
      sprite.position.set(
        zone.offset.x,
        zone.levels * (CELL_H + CELL_GAP) + 1.8,
        zone.offset.z + (zone.rows * (CELL_D + CELL_GAP)) / 2 + 1.5
      );
      sprite.userData.isLabel = true;
      scene.value.add(sprite);
    });
  };

  function roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, h - r);
    ctx.quadraticCurveTo(x + w, h, x + w - r, h);
    ctx.lineTo(x + r, h);
    ctx.quadraticCurveTo(x, h, x, h - r);
    ctx.lineTo(x, r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
  }

  // ==================== ★★★ 爆炸展开视图核心逻辑 ★★★ ====================

  /**
   * 为指定区域的所有库位生成标签精灵
   * 只在爆炸视图中显示
   */
  const createCellLabelsForZone = (zoneName) => {
    if (!scene.value) return  // 场景已销毁，跳过
    clearCellLabels();

    cellLabelGroup = new THREE.Group();
    cellLabelGroup.name = 'cellLabelGroup';

    const zd = shelves.value.find(z => z.zone === zoneName);
    if (!zd) return;

    zd.cells.forEach(cellData => {
      const mesh = cellData.mesh;
      if (!mesh) return;

      const code = mesh.userData.code || '';
      const qty = mesh.userData.quantity || 0;
      const goodsName = mesh.userData.goodsName || '';

      // 创建 Canvas 标签
      const canvas = document.createElement('canvas');
      canvas.width = 256;
      canvas.height = 96;
      const ctx = canvas.getContext('2d');

      // 背景
      ctx.fillStyle = 'rgba(255,255,255,0.95)';
      roundRect(ctx, 0, 0, 256, 96, 6);
      ctx.fill();
      ctx.strokeStyle = '#1890ff';
      ctx.lineWidth = 2;
      roundRect(ctx, 0, 0, 256, 96, 6);
      ctx.stroke();

      // 编码（大字）
      ctx.fillStyle = '#1a1a2e';
      ctx.font = 'bold 20px "Consolas", "Microsoft YaHei", monospace';
      ctx.textAlign = 'left';
      ctx.textBaseline = 'top';
      ctx.fillText(code, 10, 8);

      // 数量徽章
      if (qty > 0) {
        const key = getStatusKey(mesh.userData.status || 0, qty);
        const badgeColor = STATUS_COLORS[key].color;
        ctx.fillStyle = '#' + badgeColor.toString(16).padStart(6, '0');
        roundRect(ctx, 170, 6, 80, 24, 12);
        ctx.fill();
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 13px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(`数量: ${qty}`, 210, 14);
      } else {
        ctx.fillStyle = '#999';
        ctx.font = '12px sans-serif';
        ctx.textAlign = 'right';
        ctx.fillText('空', 246, 18);
      }

      // 货物名
      if (goodsName) {
        ctx.fillStyle = '#52c41a';
        ctx.font = '13px "Microsoft YaHei", sans-serif';
        ctx.textAlign = 'left';
        const displayGoods = goodsName.length > 14 ? goodsName.substring(0, 14) + '…' : goodsName;
        ctx.fillText(displayGoods, 10, 38);
      }

      // 层级提示
      ctx.fillStyle = '#888';
      ctx.font = '11px "Microsoft YaHei", sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText(`L${mesh.userData.level + 1}`, 10, 72);

      // 状态文字
      const stKey = getStatusKey(mesh.userData.status || 0, mesh.userData.quantity || 0);
      const statusTextMap = { empty: '空闲', occupied: '在库', warning: '低库存', danger: '异常' };
      ctx.fillStyle = '#666';
      ctx.textAlign = 'right';
      ctx.fillText(statusTextMap[stKey] || '-', 246, 72);

      const texture = new THREE.CanvasTexture(canvas);
      texture.needsUpdate = true;
      const spriteMat = new THREE.SpriteMaterial({
        map: texture,
        transparent: true,
        depthTest: false,
      });
      const sprite = new THREE.Sprite(spriteMat);
      sprite.scale.set(1.6, 0.6, 1);

      // 标签位置：在库位盒子上方
      sprite.userData = { isCellLabel: true, targetMesh: mesh };
      cellLabels.push(sprite);
      cellLabelGroup.add(sprite);
    });

    scene.value.add(cellLabelGroup);
  };

  /** 清除所有库位标签 */
  const clearCellLabels = () => {
    if (!scene.value) return  // 场景已销毁
    if (cellLabelGroup) {
      scene.value.remove(cellLabelGroup);
      cellLabelGroup.traverse(obj => {
        if (obj.isSprite && obj.material?.map) obj.material.map.dispose?.();
        if (obj.material) obj.material.dispose?.();
      });
      cellLabelGroup = null;
    }
    cellLabels = [];
  };

  /** 更新标签位置（每帧调用） */
  const updateCellLabelPositions = () => {
    if (!cellLabelGroup || !isExploded) return;
    cellLabels.forEach(sprite => {
      if (sprite.userData.targetMesh) {
        const target = sprite.userData.targetMesh;
        const wp = new THREE.Vector3();
        target.getWorldPosition(wp);
        sprite.position.set(wp.x, wp.y + CELL_H * 0.6, wp.z);
      }
    });
  };

  /**
   * ★ 核心：爆炸展开一个区域
   * @param {string} zoneName - 目标区域名
   * @param {number} explodeFactor - 展开系数（1=紧凑, 2.5=轻度展开, 4=完全展开）
   * @param {number} duration - 动画帧数（约60fps）
   */
  const explodeZone = (zoneName, explodeFactor = 4.0, duration = 50) => {
    const zd = shelves.value.find(z => z.zone === zoneName);
    if (!zd) return;

    const zone = zones.find(z => z.name === zoneName);
    if (!zone) return;

    isExploded = true;
    explodedZoneName = zoneName;

    const shelfW = CELL_W + CELL_GAP;
    const shelfD = CELL_D + CELL_GAP;

    // 收集需要移动的组和目标位置
    const animations = [];

    zd.groups.forEach((group, idx) => {
      const rowIdx = Math.floor(idx / zone.cols);
      const colIdx = idx % zone.cols;

      // 原始位置（group 的局部坐标原点对应的世界坐标）
      const origPos = groupOriginalPositions.get(group);
      if (!origPos) return;

      // 紧凑排列时的基准位置
      const totalW = zone.cols * shelfW;
      const totalD = zone.rows * shelfD;
      const startX = zone.offset.x - totalW / 2 + shelfW / 2;
      const startZ = zone.offset.z - totalD / 2 + shelfD / 2;
      const compactX = startX + colIdx * shelfW;
      const compactZ = startZ + rowIdx * shelfD;

      // 展开后的目标位置 —— 以区域中心为基准向四周扩散
      const centerX = zone.offset.x;
      const centerZ = zone.offset.z;

      // 计算从中心出发的方向和距离
      const dirX = compactX - centerX;
      const dirZ = compactZ - centerZ;
      const dist = Math.sqrt(dirX * dirX + dirZ * dirZ) || 1;

      // 目标：沿方向拉开距离
      const targetX = centerX + dirX * explodeFactor;
      const targetZ = centerZ + dirZ * explodeFactor;

      animations.push({
        group,
        fromX: group.position.x || 0,
        fromY: group.position.y || 0,
        fromZ: group.position.z || 0,
        toX: targetX - compactX, // 因为group内部坐标已经包含了compact偏移，这里设为相对位移
        toY: 0,
        toZ: targetZ - compactZ,
      });
    });

    // 执行动画
    let progress = 0;
    const animateStep = () => {
      progress++;
      const t = easeOutCubic(Math.min(progress / duration, 1));

      animations.forEach(anim => {
        anim.group.position.x = anim.fromX + (anim.toX - anim.fromX) * t;
        anim.group.position.y = anim.fromY + (anim.toY - anim.fromY) * t;
        anim.group.position.z = anim.fromZ + (anim.toZ - anim.fromZ) * t;
      });

      if (progress < duration) {
        trackedRequestAnimationFrame(animateStep);
      } else {
        // 动画结束，创建库位标签
        createCellLabelsForZone(zoneName);
      }
    };
    animateStep();
  };

  /**
   * 恢复紧凑排列
   */
  const implodeZone = (duration = 40) => {
    if (!isExploded) return;

    const zd = shelves.value.find(z => z.zone === explodedZoneName);
    if (!zd) return;

    // 先清除标签
    clearCellLabels();

    const animations = [];

    zd.groups.forEach(group => {
      animations.push({
        group,
        fromX: group.position.x,
        fromY: group.position.y,
        fromZ: group.position.z,
        toX: 0,
        toY: 0,
        toZ: 0,
      });
    });

    let progress = 0;
    const animateStep = () => {
      progress++;
      const t = easeOutCubic(Math.min(progress / duration, 1));

      animations.forEach(anim => {
        anim.group.position.x = anim.fromX + (anim.toX - anim.fromX) * t;
        anim.group.position.y = anim.fromY + (anim.toY - anim.fromY) * t;
        anim.group.position.z = anim.fromZ + (anim.toZ - anim.fromZ) * t;
      });

      if (progress < duration) {
        trackedRequestAnimationFrame(animateStep);
      } else {
        isExploded = false;
        explodedZoneName = null;
      }
    };
    animateStep();
  };

  // ==================== 视图切换功能 ====================

  let currentViewMode = 'global';
  let isTransitioning = false; // 动画锁，防止并发

  /**
   * implodeZone 返回 Promise，动画完成后 resolve
   */
  const implodeZoneAsync = (duration = 40) => {
    return new Promise((resolve) => {
      if (!isExploded) { resolve(); return; }

      const zd = shelves.value.find(z => z.zone === explodedZoneName);
      if (!zd) { resolve(); return; }

      clearCellLabels();

      const animations = [];
      zd.groups.forEach(group => {
        animations.push({
          group,
          fromX: group.position.x,
          fromY: group.position.y,
          fromZ: group.position.z,
          toX: 0, toY: 0, toZ: 0,
        });
      });

      let progress = 0;
      const animateStep = () => {
        progress++;
        const t = easeOutCubic(Math.min(progress / duration, 1));
        animations.forEach(anim => {
          anim.group.position.x = anim.fromX + (anim.toX - anim.fromX) * t;
          anim.group.position.y = anim.fromY + (anim.toY - anim.fromY) * t;
          anim.group.position.z = anim.fromZ + (anim.toZ - anim.fromZ) * t;
        });
        if (progress < duration) {
          trackedRequestAnimationFrame(animateStep);
        } else {
          isExploded = false;
          explodedZoneName = null;
          resolve(); // ✅ 动画完成
        }
      };
      animateStep();
    });
  };

  /**
   * explodeZone 也返回 Promise
   */
  const explodeZoneAsync = (zoneName, explodeFactor = 4.0, duration = 50) => {
    return new Promise((resolve) => {
      const zd = shelves.value.find(z => z.zone === zoneName);
      if (!zd) { resolve(); return; }
      const zone = zones.find(z => z.name === zoneName);
      if (!zone) { resolve(); return; }

      isExploded = true;
      explodedZoneName = zoneName;

      const shelfW = CELL_W + CELL_GAP;
      const shelfD = CELL_D + CELL_GAP;
      const animations = [];

      zd.groups.forEach((group, idx) => {
        const rowIdx = Math.floor(idx / zone.cols);
        const colIdx = idx % zone.cols;
        const origPos = groupOriginalPositions.get(group);
        if (!origPos) return;

        const totalW = zone.cols * shelfW;
        const totalD = zone.rows * shelfD;
        const startX = zone.offset.x - totalW / 2 + shelfW / 2;
        const startZ = zone.offset.z - totalD / 2 + shelfD / 2;
        const compactX = startX + colIdx * shelfW;
        const compactZ = startZ + rowIdx * shelfD;
        const centerX = zone.offset.x;
        const centerZ = zone.offset.z;
        const dirX = compactX - centerX;
        const dirZ = compactZ - centerZ;

        const targetX = centerX + dirX * explodeFactor;
        const targetZ = centerZ + dirZ * explodeFactor;

        animations.push({
          group,
          fromX: group.position.x || 0,
          fromY: group.position.y || 0,
          fromZ: group.position.z || 0,
          toX: targetX - compactX,
          toY: 0,
          toZ: targetZ - compactZ,
        });
      });

      let progress = 0;
      const animateStep = () => {
        progress++;
        const t = easeOutCubic(Math.min(progress / duration, 1));
        animations.forEach(anim => {
          anim.group.position.x = anim.fromX + (anim.toX - anim.fromX) * t;
          anim.group.position.y = anim.fromY + (anim.toY - anim.fromY) * t;
          anim.group.position.z = anim.fromZ + (anim.toZ - anim.fromZ) * t;
        });
        if (progress < duration) {
          trackedRequestAnimationFrame(animateStep);
        } else {
          createCellLabelsForZone(zoneName);
          resolve(); // ✅ 动画完成
        }
      };
      animateStep();
    });
  };

  /**
   * ★ applyViewMode — 重写版：串行化所有动画，杜绝并发冲突
   *
   * 流程：
   *   1. 如果正在过渡中，先忽略（防抖）
   *   2. 如果当前是爆炸状态且目标不同 → 先收回，再执行新操作
   *   3. global: 只收回 + 恢复视角
   *   4. zone: 收回后轻度展开(factor=2.0)
   *   5. close: 收回后完全展开(factor=4.0)
   */
  const applyViewMode = async (mode, targetZone) => {
    if (!camera.value || !controls.value) return;

    // 防抖：正在动画中则忽略新请求
    if (isTransitioning) return;
    isTransitioning = true;

    currentViewMode = mode;

    try {
      switch (mode) {
        case 'global': {
          // 全局模式：只收回爆炸 + 恢复视角
          if (isExploded) {
            await implodeZoneAsync(35);
          }
          restoreAllZoneOpacity();
          const targetPos = new THREE.Vector3(16, 11, 16);
          const lookAt = new THREE.Vector3(0, 1.5, 0);
          animateCameraTo(targetPos, lookAt);
          break;
        }

        case 'zone': {
          const z = zones.find(z => z.name === targetZone);
          if (!z) break;

          // 第1步：如果之前有爆炸状态，先收回
          if (isExploded) {
            await implodeZoneAsync(30);
          }

          // 第2步：轻度展开
          await explodeZoneAsync(targetZone, 2.0, 40);

          // 第3步：相机 + 区域隔离
          const targetPos = new THREE.Vector3(
            z.offset.x + 8, 12, z.offset.z + 10
          );
          const lookAt = new THREE.Vector3(z.offset.x, 1.5, z.offset.z);
          animateCameraTo(targetPos, lookAt);
          setZoneVisibility(targetZone, false, 0.12);
          break;
        }

        case 'close': {
          const z = zones.find(z => z.name === targetZone);
          if (!z) break;

          // 第1步：收回之前的爆炸
          if (isExploded) {
            await implodeZoneAsync(30);
          }

          // 第2步：完全爆炸展开
          await explodeZoneAsync(targetZone, 4.0, 55);

          // 第3步：相机 + 强隔离
          const targetPos = new THREE.Vector3(
            z.offset.x + 3, 10, z.offset.z + 12
          );
          const lookAt = new THREE.Vector3(z.offset.x, 1.5, z.offset.z);
          animateCameraTo(targetPos, lookAt);
          setZoneVisibility(targetZone, true, 0.05);
          break;
        }
      }
    } finally {
      // 稍微延迟解锁，防止快速连续点击
      setTimeout(() => { isTransitioning = false; }, 100);
    }
  };

  /** 相机平滑动画 */
  const animateCameraTo = (targetPosition, targetLookAt) => {
    const startPos = camera.value.position.clone();
    const startTarget = controls.value.target.clone();
    let progress = 0;
    const duration = 45;

    const step = () => {
      progress++;
      const t = easeOutCubic(Math.min(progress / duration, 1));
      camera.value.position.lerpVectors(startPos, targetPosition, t);
      controls.value.target.lerpVectors(startTarget, targetLookAt, t);
      controls.value.update();
      if (progress < duration) {
        trackedRequestAnimationFrame(step);
      }
    };
    step();
  };

  /**
   * 设置区域可见性（高亮/暗化）
   * @param {string|null} activeZoneName - 高亮的目标区，null=全部恢复
   * @param {boolean} isolate - 是否隔离模式
   * @param {number} [fadeOpacity] - 非目标区的透明度
   */
  const setZoneVisibility = (activeZoneName, isolate, fadeOpacity) => {
    shelves.value.forEach(zd => {
      const isActive = zd.zone === activeZoneName;
      zd.groups.forEach(group => {
        group.traverse(child => {
          if (child.isMesh && child.material) {
            if (isolate && activeZoneName !== null) {
              child.material.transparent = true;
              const targetOpacity = isActive ? 1.0 : (fadeOpacity ?? (isExploded ? 0.05 : 0.12));
              child.material.opacity = targetOpacity;
            } else {
              child.material.opacity = 1.0;
              child.material.transparent = child.material.emissiveIntensity > 0;
            }
            child.material.needsUpdate = true;
          }
        });
      });
    });
  };

  /** 恢复所有区域完全不透明 */
  const restoreAllZoneOpacity = () => {
    setZoneVisibility(null, false);
  };

  function easeOutCubic(t) {
    return 1 - Math.pow(1 - t, 3);
  }

  // ==================== 数据加载 ====================

  const loadLocations = async () => {
    try {
      const res = await get('/locations', { size: 1000 });
      // 兼容三种返回：LIST / OBJ.data / PAGE.content
      const data = res?.data || res || []
      const locations = Array.isArray(data) ? data : (data.content || [])

      shelves.value.forEach(zoneData => {
        zoneData.cells.forEach(cell => {
          const match = locations.find(l => l.locationCode === cell.code);
          if (match) {
            cell.locationId = match.id;
            cell.status = match.status || 0;
            const qty = match.quantity || 0;
            const goodsName = match.goodsName || '';

            Object.assign(cell.mesh.userData, {
              status: cell.status,
              locationId: match.id,
              goodsName,
              batchNo: match.batchNo || '',
              quantity: qty,
              attribute: match.attribute || '',
            });

            updateCellAppearance(cell.mesh, cell.status, qty);

            // 如果当前是爆炸模式且标签存在，重建标签
            if (isExploded) {
              setTimeout(() => createCellLabelsForZone(explodedZoneName), 100);
            }
          }
        });
      });
      return locations;
    } catch (error) {
      console.warn('加载库位数据失败，使用默认显示:', error.message);
      return [];
    }
  };

  /** 更新单个库位外观 */
  const updateCellAppearance = (mesh, status, quantity = 0) => {
    const key = getStatusKey(status, quantity);
    const pal = STATUS_COLORS[key];
    mesh.material.color.setHex(pal.color);
    mesh.material.emissive.setHex(pal.emissive);
    mesh.material.emissiveIntensity = status !== 0 ? 0.35 : 0;
    mesh.material.opacity = 1.0;
    mesh.material.transparent = pal.emissiveIntensity > 0;
    mesh.material.needsUpdate = true;
  };

  /** 通过 ID 更新状态（WebSocket 回调用） */
  const updateLocationStatus = (locationId, newStatus) => {
    shelves.value.forEach(zd => {
      zd.cells.forEach(cell => {
        if (String(cell.locationId) === String(locationId)) {
          cell.status = newStatus;
          cell.mesh.userData.status = newStatus;
          const qty = cell.mesh.userData.quantity || 0;
          updateCellAppearance(cell.mesh, newStatus, qty);
        }
      });
    });
  };

  /** 高亮某个库位 */
  const highlightCell = (mesh, isHover) => {
    if (!mesh || !mesh.material) return;
    if (isHover) {
      mesh.material.emissiveIntensity = 0.7;
      mesh.scale.setScalar(1.06);
    } else {
      const st = mesh.userData.status || 0;
      const qty = mesh.userData.quantity || 0;
      const key = getStatusKey(st, qty);
      mesh.material.emissiveIntensity = st !== 0 ? 0.35 : 0;
      mesh.scale.setScalar(1.0);
    }
  };

  // ==================== 清理 ====================

  // 跟踪所有正在运行的动画帧，dispose 时取消
  let activeAnimations = new Set()

  const trackedRequestAnimationFrame = (callback) => {
    const id = requestAnimationFrame(() => {
      activeAnimations.delete(id)
      callback()
    })
    activeAnimations.add(id)
    return id
  }

  const dispose = () => {
    // 取消所有正在运行的动画帧
    activeAnimations.forEach(id => cancelAnimationFrame(id))
    activeAnimations.clear()

    // 清理爆炸/恢复动画状态
    isExploded = false
    explodedZoneName = null

    if (controls.value) {
      controls.value.dispose()
      controls.value = null
    }
    if (renderer.value) {
      renderer.value.dispose()
      renderer.value.forceContextLoss?.()
      if (renderer.value.domElement?.parentNode) {
        renderer.value.domElement.parentNode.removeChild(renderer.value.domElement)
      }
      renderer.value = null
    }
    if (scene.value) {
      clearCellLabels()
      scene.value.traverse(obj => {
        if (obj.geometry) obj.geometry.dispose?.()
        if (obj.material) {
          if (Array.isArray(obj.material)) {
            obj.material.forEach(m => disposeMaterialDeep(m))
          } else {
            disposeMaterialDeep(obj.material)
          }
        }
      })
      while (scene.value.children.length > 0) {
        scene.value.remove(scene.value.children[0])
      }
      scene.value = null
    }

    camera.value = null
    raycaster.value = null
    mouse.value = null
    shelves.value = []

    groupOriginalPositions.clear()
    cellOriginalPositions.clear()
    cellLabels = []
    cellLabelGroup = null
  }

  // 深度释放材质（含纹理）
  const disposeMaterialDeep = (mat) => {
    for (const key of Object.keys(mat)) {
      const value = mat[key]
      if (value?.isTexture) value.dispose()
    }
    mat.dispose?.()
  }

  // ==================== 启动 ====================

  const init = () => {
    initScene();
    initCamera();
    initRenderer();
    initControls();
    createGround();
    createShelves();
    createZoneLabels();
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
    updateLocationStatus,
    highlightCell,
    STATUS_COLORS,
    applyViewMode,
    // 状态查询
    isExploded: () => isExploded,
    explodedZoneName: () => explodedZoneName,
    updateCellLabelPositions,
  };
}
