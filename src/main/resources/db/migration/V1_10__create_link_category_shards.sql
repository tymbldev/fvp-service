-- Create shard tables for link_category

-- Shard 1
CREATE TABLE IF NOT EXISTS link_category_shard1 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);

-- Shard 2
CREATE TABLE IF NOT EXISTS link_category_shard2 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);

-- Shard 3
CREATE TABLE IF NOT EXISTS link_category_shard3 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);

-- Shard 4
CREATE TABLE IF NOT EXISTS link_category_shard4 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);

-- Shard 5
CREATE TABLE IF NOT EXISTS link_category_shard5 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);

-- Shard 6
CREATE TABLE IF NOT EXISTS link_category_shard6 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);

-- Shard 7
CREATE TABLE IF NOT EXISTS link_category_shard7 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);

-- Shard 8
CREATE TABLE IF NOT EXISTS link_category_shard8 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);

-- Shard 9
CREATE TABLE IF NOT EXISTS link_category_shard9 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
);

-- Shard 10
CREATE TABLE IF NOT EXISTS link_category_shard10 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order INT,
    INDEX idx_tenant_category (tenant_id, category),
    INDEX idx_tenant_link (tenant_id, link_id),
    INDEX idx_link_id (link_id),
    FOREIGN KEY (link_id) REFERENCES link(id) ON DELETE CASCADE
); 