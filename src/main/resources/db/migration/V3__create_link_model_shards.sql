-- Create LinkModel shard tables

-- Shard 1
CREATE TABLE link_model_shard1 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
);

-- Shard 2
CREATE TABLE link_model_shard2 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
);

-- Shard 3
CREATE TABLE link_model_shard3 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
);

-- Shard 4
CREATE TABLE link_model_shard4 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
);

-- Shard 5
CREATE TABLE link_model_shard5 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
);

-- Shard 6
CREATE TABLE link_model_shard6 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
);

-- Shard 7
CREATE TABLE link_model_shard7 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
);

-- Shard 8
CREATE TABLE link_model_shard8 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
);

-- Shard 9
CREATE TABLE link_model_shard9 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
);

-- Shard 10
CREATE TABLE link_model_shard10 (
    id INT AUTO_INCREMENT PRIMARY KEY,
    link_id INT NOT NULL,
    tenant_id INT,
    model VARCHAR(255) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    random_order DOUBLE DEFAULT RAND(),
    INDEX idx_link_id (link_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_model (model),
    INDEX idx_tenant_model (tenant_id, model)
); 