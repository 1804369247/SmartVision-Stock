package com.example.smartvisionstock.config;

import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.GoodsRepository;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final double CELL_SIZE = 0.8;
    private static final double CELL_GAP = 0.2;
    private static final double ZONE_SIZE = 12;
    private static final double ZONE_GAP = 2;

    private final String[] zones = {"A", "B", "C"};
    private final int[] zoneOffsets = {-13, 0, 13};

    @Override
    public void run(String... args) throws Exception {
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
        } catch (Exception e) {
            Thread.sleep(2000);
        }

        initGoods();
        initLocations();
        initGoodsInstances();
    }

    private void initGoods() {
        try {
            if (goodsRepository.count() > 0) return;
        } catch (Exception e) {
            return;
        }

        Goods goods1 = new Goods();
        goods1.setCode("GD-001");
        goods1.setName("电机转子");
        goods1.setSpec("型号MR-200");
        goods1.setUnit("个");
        goods1.setWarningQuantity(10);
        goods1.setCategory("电机设备");
        goods1.setDefaultSupplier("华东电机厂");
        goods1.setStorageRule("防潮");
        goods1.setDefaultShelfLife(365);
        goodsRepository.save(goods1);

        Goods goods2 = new Goods();
        goods2.setCode("GD-002");
        goods2.setName("轴承");
        goods2.setSpec("型号6205");
        goods2.setUnit("个");
        goods2.setWarningQuantity(20);
        goods2.setCategory("机械配件");
        goods2.setDefaultSupplier("精密轴承公司");
        goods2.setStorageRule("常温");
        goods2.setDefaultShelfLife(730);
        goodsRepository.save(goods2);

        Goods goods3 = new Goods();
        goods3.setCode("GD-003");
        goods3.setName("PLC控制器");
        goods3.setSpec("型号S7-1200");
        goods3.setUnit("台");
        goods3.setWarningQuantity(5);
        goods3.setCategory("电子设备");
        goods3.setDefaultSupplier("西门子代理");
        goods3.setStorageRule("防静电");
        goods3.setDefaultShelfLife(1095);
        goodsRepository.save(goods3);

        System.out.println("Goods initialized");
    }

    private void initLocations() {
        try {
            if (storageLocationRepository.count() > 0) return;
        } catch (Exception e) {
            return;
        }

        for (int zoneIdx = 0; zoneIdx < zones.length; zoneIdx++) {
            String zoneName = zones[zoneIdx];
            double zoneOffsetX = zoneOffsets[zoneIdx];

            double startX = zoneOffsetX - (ZONE_SIZE - ZONE_GAP) / 2 + CELL_SIZE / 2 + CELL_GAP;
            double startZ = 0 - (ZONE_SIZE - ZONE_GAP) / 2 + CELL_SIZE / 2 + CELL_GAP;

            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    for (int level = 0; level < 3; level++) {
                        String locationCode = zoneName + "-" + String.format("%02d", row + 1) + "-" +
                                String.format("%02d", col + 1) + "-" + String.format("%02d", level + 1);

                        double x = startX + col * (CELL_SIZE + CELL_GAP);
                        double y = CELL_SIZE / 2 + level * (CELL_SIZE + CELL_GAP);
                        double z = startZ + row * (CELL_SIZE + CELL_GAP);

                        StorageLocation location = new StorageLocation();
                        location.setLocationCode(locationCode);
                        location.setArea(zoneName);
                        location.setStatus(0);
                        location.setXCoord(x);
                        location.setYCoord(y);
                        location.setZCoord(z);
                        if (zoneName.equals("A")) {
                            location.setAttribute("DANGEROUS");
                            location.setDescription("危险品存放区");
                        } else if (zoneName.equals("B")) {
                            location.setAttribute("COLD");
                            location.setDescription("冷藏库区");
                        } else {
                            location.setAttribute("NORMAL");
                            location.setDescription("普通存储区");
                        }
                        storageLocationRepository.save(location);
                    }
                }
            }
        }

        System.out.println("Locations initialized: " + storageLocationRepository.count());
    }

    private void initGoodsInstances() {
        try {
            if (goodsInstanceRepository.count() > 0) return;
        } catch (Exception e) {
            return;
        }

        List<Goods> goodsList = goodsRepository.findAll();
        if (goodsList.isEmpty()) return;

        List<StorageLocation> locations = storageLocationRepository.findAll();
        Random random = new Random();

        for (StorageLocation location : locations) {
            if (random.nextDouble() < 0.2) {
                Goods goods = goodsList.get(random.nextInt(goodsList.size()));
                int quantity = random.nextInt(10) + 1;

                GoodsInstance instance = new GoodsInstance();
                instance.setGoodsId(goods.getId());
                instance.setBatchNo("BATCH-" + System.currentTimeMillis() + "-" + random.nextInt(1000));
                instance.setQuantity(quantity);
                instance.setLocationId(location.getId());
                instance.setInTime(LocalDateTime.now().minusDays(random.nextInt(30)));
                instance.setOperator("admin");
                instance.setSupplier(goods.getDefaultSupplier());
                instance.setExpiryDate(LocalDateTime.now().plusDays(goods.getDefaultShelfLife()));
                instance.setFrozen(false);
                instance.setFrozenReason(null);

                GoodsInstance savedInstance = goodsInstanceRepository.save(instance);

                location.setStatus(1);
                location.setCurrentGoodsInstanceId(savedInstance.getId());
                storageLocationRepository.save(location);
            }
        }

        System.out.println("Goods instances initialized: " + goodsInstanceRepository.count());
    }
}
