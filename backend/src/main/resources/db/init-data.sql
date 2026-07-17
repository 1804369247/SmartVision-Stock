-- ================================================
-- SmartVision Stock 初始化数据脚本（参考用）
-- 
-- 实际初始化由 DataInitializer.java 在应用启动时自动完成。
-- 此脚本仅供手动部署或 Flyway 集成时参考。
-- 
-- 密码使用 BCrypt 加密，运行时可自动生成。
-- 默认账号（DataInitializer 创建）：
--   admin / admin123    (角色: ADMIN)
--   operator / operator123  (角色: OPERATOR)
-- ================================================

-- 默认仓库
INSERT IGNORE INTO warehouse (id, name, code, address, manager, phone, status)
SELECT 1, '主仓库', 'WH2026010100001', '中国深圳南山区科技园', '张经理', '13800138000', 1
WHERE NOT EXISTS (SELECT 1 FROM warehouse WHERE code = 'WH2026010100001');

-- 默认供应商
INSERT IGNORE INTO supplier (id, name, supplier_code, contact, phone, address, enabled)
SELECT 1, '默认供应商', 'SP2026010100001', '李经理', '13900139000', '中国深圳龙华区', 1
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE supplier_code = 'SP2026010100001');
