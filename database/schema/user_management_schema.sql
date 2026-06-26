-- =====================================================
-- USER MANAGEMENT DATABASE SCHEMA
-- =====================================================
-- Database: meditrack_user_management
-- Purpose: Store all system users, roles, and access control

-- Create database
CREATE DATABASE IF NOT EXISTS meditrack_user_management 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE meditrack_user_management;

-- =====================================================
-- ROLES TABLE
-- =====================================================
CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    permissions JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role_name (role_name)
);

-- =====================================================
-- DEPARTMENTS TABLE
-- =====================================================
CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_department_name (department_name),
    INDEX idx_active (is_active)
);

-- =====================================================
-- USERS TABLE
-- =====================================================
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY, -- UUID or custom ID like 'admin-1', 'doc-1', etc.
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role_id INT NOT NULL,
    department_id INT,
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (department_id) REFERENCES departments(id),
    INDEX idx_email (email),
    INDEX idx_role (role_id),
    INDEX idx_department (department_id),
    INDEX idx_active (is_active),
    INDEX idx_name (first_name, last_name)
);

-- =====================================================
-- ADMINS TABLE (extends users for admin-specific data)
-- =====================================================
CREATE TABLE admins (
    user_id VARCHAR(50) PRIMARY KEY,
    admin_level ENUM('SUPER_ADMIN', 'ADMIN', 'ASSISTANT_ADMIN') DEFAULT 'ADMIN',
    access_permissions JSON, -- Additional permissions beyond role
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_admin_level (admin_level)
);

-- =====================================================
-- DOCTORS TABLE (extends users for doctor-specific data)
-- =====================================================
CREATE TABLE doctors (
    user_id VARCHAR(50) PRIMARY KEY,
    license_number VARCHAR(100) UNIQUE,
    specialization VARCHAR(100),
    years_of_experience INT DEFAULT 0,
    education TEXT,
    certification JSON, -- Store certifications as JSON array
    consultation_fee DECIMAL(10,2),
    is_available BOOLEAN DEFAULT TRUE,
    max_patients INT DEFAULT 50,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_specialization (specialization),
    INDEX idx_available (is_available),
    INDEX idx_experience (years_of_experience)
);

-- =====================================================
-- NURSES TABLE (extends users for nurse-specific data)
-- =====================================================
CREATE TABLE nurses (
    user_id VARCHAR(50) PRIMARY KEY,
    license_number VARCHAR(100) UNIQUE,
    certification JSON, -- Store certifications as JSON array
    shift_type ENUM('DAY', 'NIGHT', 'ROTATING') DEFAULT 'DAY',
    is_available BOOLEAN DEFAULT TRUE,
    max_patients INT DEFAULT 20,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_shift (shift_type),
    INDEX idx_available (is_available)
);

-- =====================================================
-- AUDIT LOGS TABLE
-- =====================================================
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50), -- 'user', 'patient', 'department', etc.
    entity_id VARCHAR(50),
    old_values JSON,
    new_values JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_action (user_id, action),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created (created_at)
);

-- =====================================================
-- USER SESSIONS TABLE (for security and monitoring)
-- =====================================================
CREATE TABLE user_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    session_token VARCHAR(1000) NOT NULL UNIQUE,
    ip_address VARCHAR(45),
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_session (user_id),
    INDEX idx_token (session_token),
    INDEX idx_expires (expires_at)
);

-- =====================================================
-- TRIGGERS FOR AUDIT LOGGING
-- =====================================================
DELIMITER //

CREATE TRIGGER after_user_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action, entity_type, entity_id, new_values)
    VALUES (NEW.id, 'CREATE', 'user', NEW.id, JSON_OBJECT(
        'email', NEW.email,
        'first_name', NEW.first_name,
        'last_name', NEW.last_name,
        'role_id', NEW.role_id,
        'department_id', NEW.department_id
    ));
END//

CREATE TRIGGER after_user_update
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values)
    VALUES (NEW.id, 'UPDATE', 'user', NEW.id, 
        JSON_OBJECT(
            'email', OLD.email,
            'first_name', OLD.first_name,
            'last_name', OLD.last_name,
            'is_active', OLD.is_active
        ),
        JSON_OBJECT(
            'email', NEW.email,
            'first_name', NEW.first_name,
            'last_name', NEW.last_name,
            'is_active', NEW.is_active
        )
    );
END//

DELIMITER ;
