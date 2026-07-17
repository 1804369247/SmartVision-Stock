package com.example.smartvisionstock.config;

import com.example.smartvisionstock.entity.Supplier;
import com.example.smartvisionstock.entity.User;
import com.example.smartvisionstock.entity.Warehouse;
import com.example.smartvisionstock.repository.SupplierRepository;
import com.example.smartvisionstock.repository.UserRepository;
import com.example.smartvisionstock.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 生成随机密码（8位，含大小写字母和数字） */
    private static String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public void run(String... args) {
        // 初始化默认管理员和操作员账号
        if (userRepository.count() == 0) {
            String adminPassword = "admin123";
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            admin.setRealName("系统管理员");
            admin.setEnabled(true);
            admin.setCreateTime(LocalDateTime.now());
            userRepository.save(admin);
            log.info("默认管理员账号已创建: admin / admin123");

            String operatorPassword = "operator123";
            User operator = new User();
            operator.setUsername("operator");
            operator.setPassword(passwordEncoder.encode(operatorPassword));
            operator.setRole("OPERATOR");
            operator.setRealName("操作员");
            operator.setEnabled(true);
            operator.setCreateTime(LocalDateTime.now());
            userRepository.save(operator);
            log.info("默认操作员账号已创建: operator / operator123");
        }

        // 初始化默认仓库
        if (warehouseRepository.count() == 0) {
            Warehouse wh = new Warehouse();
            wh.setName("主仓库");
            wh.setCode("WH2026010100001");
            wh.setAddress("中国深圳南山区科技园");
            wh.setManager("张经理");
            wh.setPhone("13800138000");
            wh.setStatus(1);
            warehouseRepository.save(wh);
            log.info("默认仓库已创建: 主仓库");
        }

        // 初始化默认供应商
        if (supplierRepository.count() == 0) {
            Supplier sp = new Supplier();
            sp.setName("默认供应商");
            sp.setSupplierCode("SP2026010100001");
            sp.setContact("李经理");
            sp.setPhone("13900139000");
            sp.setAddress("中国深圳龙华区");
            sp.setEnabled(true);
            supplierRepository.save(sp);
            log.info("默认供应商已创建: 默认供应商");
        }
    }
}
