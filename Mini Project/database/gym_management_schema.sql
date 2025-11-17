-- ===================================
-- GYM MANAGEMENT DATABASE SETUP (FIXED)
-- ===================================

CREATE DATABASE IF NOT EXISTS gym_management
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE gym_management;
SET FOREIGN_KEY_CHECKS = 1;

-- Drop tables in reverse order of dependencies (optional - use if you want fresh start)
-- DROP TABLE IF EXISTS workout_sessions;
-- DROP TABLE IF EXISTS progress_tracking;
-- DROP TABLE IF EXISTS payments;
-- DROP TABLE IF EXISTS class_bookings;
-- DROP TABLE IF EXISTS classes;
-- DROP TABLE IF EXISTS trainers;
-- DROP TABLE IF EXISTS members;
-- DROP TABLE IF EXISTS membership_plans;
-- DROP TABLE IF EXISTS equipment;
-- DROP TABLE IF EXISTS users;

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    role ENUM('ADMIN', 'TRAINER', 'MEMBER') NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role)
);

CREATE TABLE IF NOT EXISTS membership_plans (
    id INT PRIMARY KEY AUTO_INCREMENT,
    plan_name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    duration_in_months INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plan_name (plan_name),
    INDEX idx_is_active (is_active)
);

CREATE TABLE IF NOT EXISTS members (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    emergency_contact VARCHAR(100),
    medical_conditions TEXT,
    membership_plan_id INT NOT NULL,
    membership_start_date DATE NOT NULL,
    membership_end_date DATE NOT NULL,
    membership_status ENUM('ACTIVE', 'EXPIRED', 'SUSPENDED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (membership_plan_id) REFERENCES membership_plans(id),
    INDEX idx_user_id (user_id),
    INDEX idx_membership_status (membership_status),
    INDEX idx_membership_end_date (membership_end_date)
);

CREATE TABLE IF NOT EXISTS trainers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    specialization VARCHAR(200),
    certifications TEXT,
    hourly_rate DECIMAL(10, 2),
    availability TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_specialization (specialization)
);

CREATE TABLE IF NOT EXISTS classes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    class_name VARCHAR(100) NOT NULL,
    description TEXT,
    trainer_id INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    max_capacity INT NOT NULL DEFAULT 10,
    current_bookings INT DEFAULT 0,
    status ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (trainer_id) REFERENCES trainers(id) ON DELETE CASCADE,
    INDEX idx_trainer_id (trainer_id),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status),
    CONSTRAINT chk_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_booking_capacity CHECK (current_bookings <= max_capacity)
);

CREATE TABLE IF NOT EXISTS class_bookings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    class_id INT NOT NULL,
    member_id INT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('CONFIRMED', 'CANCELLED', 'ATTENDED') DEFAULT 'CONFIRMED',
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    UNIQUE KEY unique_class_member (class_id, member_id),
    INDEX idx_class_id (class_id),
    INDEX idx_member_id (member_id),
    INDEX idx_booking_date (booking_date)
);

CREATE TABLE IF NOT EXISTS payments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    member_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method ENUM('CASH', 'CARD', 'ONLINE') NOT NULL,
    payment_type ENUM('MEMBERSHIP', 'CLASS', 'OTHER') NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'FAILED') DEFAULT 'COMPLETED',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255),

    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    INDEX idx_member_id (member_id),
    INDEX idx_payment_date (payment_date),
    INDEX idx_status (status),
    INDEX idx_payment_type (payment_type)
);

CREATE TABLE IF NOT EXISTS progress_tracking (
    id INT PRIMARY KEY AUTO_INCREMENT,
    member_id INT NOT NULL,
    trainer_id INT,
    measurement_date DATE NOT NULL,
    weight DECIMAL(5, 2),
    body_fat_percentage DECIMAL(4, 2),
    muscle_mass DECIMAL(5, 2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (trainer_id) REFERENCES trainers(id) ON DELETE SET NULL,
    INDEX idx_member_id (member_id),
    INDEX idx_measurement_date (measurement_date)
);

CREATE TABLE IF NOT EXISTS workout_sessions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    member_id INT NOT NULL,
    trainer_id INT,
    session_date TIMESTAMP NOT NULL,
    duration_minutes INT,
    exercises_performed TEXT,
    trainer_notes TEXT,
    member_feedback TEXT,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (trainer_id) REFERENCES trainers(id) ON DELETE SET NULL,
    INDEX idx_member_id (member_id),
    INDEX idx_trainer_id (trainer_id),
    INDEX idx_session_date (session_date)
);

CREATE TABLE IF NOT EXISTS equipment (
    id INT PRIMARY KEY AUTO_INCREMENT,
    equipment_name VARCHAR(100) NOT NULL,
    equipment_type VARCHAR(50),
    purchase_date DATE,
    last_maintenance_date DATE,
    next_maintenance_date DATE,
    status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'OUT_OF_ORDER') DEFAULT 'AVAILABLE',
    notes TEXT,

    INDEX idx_equipment_name (equipment_name),
    INDEX idx_status (status),
    INDEX idx_next_maintenance (next_maintenance_date)
);

-- Insert default membership plans (only if not exists)
INSERT IGNORE INTO membership_plans (plan_name, description, price, duration_in_months) VALUES
('Basic Monthly', 'Access to gym facilities during regular hours', 29.99, 1),
('Premium Monthly', 'Full access with group classes and premium amenities', 49.99, 1),
('Basic Annual', 'Annual basic membership with 2 months free', 299.99, 12),
('Premium Annual', 'Annual premium membership with 2 months free', 499.99, 12),
('Student Monthly', 'Discounted rate for students with valid ID', 19.99, 1);

-- Insert admin user (only if not exists)
INSERT IGNORE INTO users (username, password, email, role, first_name, last_name, phone) VALUES
('admin', 'admin123', 'admin@gymsystem.com', 'ADMIN', 'System', 'Administrator', '555-0000');

-- ===================================
-- TRIGGERS
-- ===================================

DROP TRIGGER IF EXISTS increment_booking_count;
DROP TRIGGER IF EXISTS decrement_booking_count;
DROP TRIGGER IF EXISTS update_membership_status;

DELIMITER //

CREATE TRIGGER increment_booking_count
AFTER INSERT ON class_bookings
FOR EACH ROW
BEGIN
    UPDATE classes
    SET current_bookings = current_bookings + 1
    WHERE id = NEW.class_id;
END//

CREATE TRIGGER decrement_booking_count
AFTER DELETE ON class_bookings
FOR EACH ROW
BEGIN
    UPDATE classes
    SET current_bookings = current_bookings - 1
    WHERE id = OLD.class_id;
END//

CREATE TRIGGER update_membership_status
BEFORE UPDATE ON members
FOR EACH ROW
BEGIN
    IF NEW.membership_end_date < CURDATE() AND NEW.membership_status = 'ACTIVE' THEN
        SET NEW.membership_status = 'EXPIRED';
    END IF;
END//

DELIMITER ;

-- ===================================
-- VIEWS
-- ===================================

CREATE OR REPLACE VIEW active_members_view AS
SELECT
    m.id as member_id,
    u.id as user_id,
    u.username,
    u.email,
    CONCAT(u.first_name, ' ', u.last_name) as full_name,
    u.phone,
    m.membership_status,
    m.membership_start_date,
    m.membership_end_date,
    mp.plan_name,
    mp.price
FROM members m
JOIN users u ON m.user_id = u.id
JOIN membership_plans mp ON m.membership_plan_id = mp.id
WHERE m.membership_status = 'ACTIVE';

CREATE OR REPLACE VIEW trainers_view AS
SELECT
    t.id as trainer_id,
    u.id as user_id,
    u.username,
    u.email,
    CONCAT(u.first_name, ' ', u.last_name) as full_name,
    u.phone,
    t.specialization,
    t.certifications,
    t.hourly_rate,
    t.availability
FROM trainers t
JOIN users u ON t.user_id = u.id;

CREATE OR REPLACE VIEW upcoming_classes_view AS
SELECT
    c.id as class_id,
    c.class_name,
    c.description,
    c.start_time,
    c.end_time,
    c.max_capacity,
    c.current_bookings,
    c.status,
    CONCAT(u.first_name, ' ', u.last_name) as trainer_name,
    t.specialization
FROM classes c
JOIN trainers t ON c.trainer_id = t.id
JOIN users u ON t.user_id = u.id
WHERE c.start_time > NOW()
AND c.status = 'SCHEDULED'
ORDER BY c.start_time;

-- Additional indexes
-- Additional indexes (skip if already exists)
-- Additional indexes (already exist, so commented out)
-- CREATE INDEX idx_users_created_at ON users(created_at);
-- CREATE INDEX idx_members_created_at ON members(created_at);
-- CREATE INDEX idx_payments_amount ON payments(amount);
-- CREATE INDEX idx_classes_datetime ON classes(start_time, end_time);

-- ===================================
-- SAMPLE DATA FOR TESTING
-- ===================================

-- Create a trainer user (only if not exists)
INSERT IGNORE INTO users (username, password, email, role, first_name, last_name, phone)
VALUES ('anbu.selvan', 'trainer123', 'anbu@gym.com', 'TRAINER', 'Anbu', 'Selvan', '555-5678');

-- Get the trainer user_id
SET @anbu_user_id = (SELECT id FROM users WHERE username = 'anbu.selvan');

-- Create trainer record (only if not exists)
INSERT IGNORE INTO trainers (user_id, specialization, certifications, hourly_rate, availability)
VALUES (@anbu_user_id, 'Yoga & Cardio', 'Certified Personal Trainer', 50.00, 'Mon-Fri 9AM-5PM');

-- Get trainer_id
SET @trainer_id = (SELECT id FROM trainers WHERE user_id = @anbu_user_id LIMIT 1);

-- Create sample classes (only if not exists - checking by class_name and trainer_id)
INSERT INTO classes (class_name, description, trainer_id, start_time, end_time, max_capacity, current_bookings, status)
SELECT * FROM (SELECT
    'yoga class' as class_name,
    'Morning yoga session for beginners' as description,
    @trainer_id as trainer_id,
    '2025-10-26 10:00:00' as start_time,
    '2025-10-26 11:00:00' as end_time,
    20 as max_capacity,
    0 as current_bookings,
    'SCHEDULED' as status
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM classes WHERE class_name = 'yoga class' AND trainer_id = @trainer_id
) LIMIT 1;

INSERT INTO classes (class_name, description, trainer_id, start_time, end_time, max_capacity, current_bookings, status)
SELECT * FROM (SELECT
    'Cardio Workout' as class_name,
    'High intensity cardio training' as description,
    @trainer_id as trainer_id,
    '2025-10-27 11:00:00' as start_time,
    '2025-10-27 12:00:00' as end_time,
    15 as max_capacity,
    0 as current_bookings,
    'SCHEDULED' as status
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM classes WHERE class_name = 'Cardio Workout' AND trainer_id = @trainer_id
) LIMIT 1;

INSERT INTO classes (class_name, description, trainer_id, start_time, end_time, max_capacity, current_bookings, status)
SELECT * FROM (SELECT
    'Strength Training' as class_name,
    'Weight lifting and strength building' as description,
    @trainer_id as trainer_id,
    '2025-10-28 14:00:00' as start_time,
    '2025-10-28 15:30:00' as end_time,
    12 as max_capacity,
    0 as current_bookings,
    'SCHEDULED' as status
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM classes WHERE class_name = 'Strength Training' AND trainer_id = @trainer_id
) LIMIT 1;

INSERT INTO classes (class_name, description, trainer_id, start_time, end_time, max_capacity, current_bookings, status)
SELECT * FROM (SELECT
    'Zumba Dance' as class_name,
    'Fun dance workout session' as description,
    @trainer_id as trainer_id,
    '2025-10-29 16:00:00' as start_time,
    '2025-10-29 17:00:00' as end_time,
    25 as max_capacity,
    0 as current_bookings,
    'SCHEDULED' as status
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM classes WHERE class_name = 'Zumba Dance' AND trainer_id = @trainer_id
) LIMIT 1;

COMMIT;

-- Verify the setup
SELECT 'Database setup completed!' as Status;
SELECT COUNT(*) as total_users FROM users;
SELECT COUNT(*) as total_trainers FROM trainers;
SELECT COUNT(*) as total_classes FROM classes;
SELECT COUNT(*) as total_membership_plans FROM membership_plans;