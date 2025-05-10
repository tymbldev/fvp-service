-- Link Category Shard Indexes Creation Script
-- This script creates necessary indexes for all 50 shards of the link_category table
-- Created based on query patterns in LinkCategoryDynamicQueryBuilder

-- Function to create indexes for a single shard
DELIMITER //
CREATE PROCEDURE create_link_category_indexes(IN shard_number INT)
BEGIN
    SET @table_name = CONCAT('link_category_shard_', shard_number);
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name, ' ADD INDEX idx_link_category_category1 (category)');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name, ' ADD INDEX idx_link_category_category_tenant (category, tenant_id)');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name, ' ADD INDEX idx_link_category_tenant_category (tenant_id, category)');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name, ' ADD INDEX idx_link_category_tenant_category_created (tenant_id, category, created_on)');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name, ' ADD INDEX idx_link_category_tenant_category_random_order (tenant_id, category, random_order)');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name, ' ADD INDEX idx_link_category_tenant_link_id (tenant_id, link_id)');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name, ' ADD INDEX idx_link_category_tenant_category_id (tenant_id, category, id)');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SET @sql = CONCAT('ALTER TABLE ', @table_name, ' ADD INDEX idx_link_category_tenant_category_link_id (tenant_id, category, link_id)');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END //
DELIMITER ;

-- Create indexes for all 50 shards
CALL create_link_category_indexes(1);
CALL create_link_category_indexes(2);
CALL create_link_category_indexes(3);
CALL create_link_category_indexes(4);
CALL create_link_category_indexes(5);
CALL create_link_category_indexes(6);
CALL create_link_category_indexes(7);
CALL create_link_category_indexes(8);
CALL create_link_category_indexes(9);
CALL create_link_category_indexes(10);
CALL create_link_category_indexes(11);
CALL create_link_category_indexes(12);
CALL create_link_category_indexes(13);
CALL create_link_category_indexes(14);
CALL create_link_category_indexes(15);
CALL create_link_category_indexes(16);
CALL create_link_category_indexes(17);
CALL create_link_category_indexes(18);
CALL create_link_category_indexes(19);
CALL create_link_category_indexes(20);
CALL create_link_category_indexes(21);
CALL create_link_category_indexes(22);
CALL create_link_category_indexes(23);
CALL create_link_category_indexes(24);
CALL create_link_category_indexes(25);
CALL create_link_category_indexes(26);
CALL create_link_category_indexes(27);
CALL create_link_category_indexes(28);
CALL create_link_category_indexes(29);
CALL create_link_category_indexes(30);
CALL create_link_category_indexes(31);
CALL create_link_category_indexes(32);
CALL create_link_category_indexes(33);
CALL create_link_category_indexes(34);
CALL create_link_category_indexes(35);
CALL create_link_category_indexes(36);
CALL create_link_category_indexes(37);
CALL create_link_category_indexes(38);
CALL create_link_category_indexes(39);
CALL create_link_category_indexes(40);
CALL create_link_category_indexes(41);
CALL create_link_category_indexes(42);
CALL create_link_category_indexes(43);
CALL create_link_category_indexes(44);
CALL create_link_category_indexes(45);
CALL create_link_category_indexes(46);
CALL create_link_category_indexes(47);
CALL create_link_category_indexes(48);
CALL create_link_category_indexes(49);
CALL create_link_category_indexes(50);

-- Drop the procedure after use
DROP PROCEDURE create_link_category_indexes; 