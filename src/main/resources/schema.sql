-- Drop tables if they exist
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS link_categories;
DROP TABLE IF EXISTS links;

-- Create users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create roles table
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create user_roles table (many-to-many relationship)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create links table
CREATE TABLE links (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    category JSON,
    duration INT NOT NULL,
    thumbnail VARCHAR(500) NOT NULL,
    thumb_path VARCHAR(100) NOT NULL,
    sheet_name VARCHAR(100) NOT NULL,
    link VARCHAR(500) NOT NULL UNIQUE,
    source VARCHAR(100) NOT NULL,
    star JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    trailer VARCHAR(500)
);

-- Create link_categories table
CREATE TABLE link_categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    link_id INT NOT NULL,
    category VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (link_id) REFERENCES links(id) ON DELETE CASCADE
);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'Administrator role with full access'),
('ROLE_USER', 'Standard user role with limited access');

-- Create indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_links_tenant_id ON links(tenant_id);
CREATE INDEX idx_link_categories_link_id ON link_categories(link_id); 