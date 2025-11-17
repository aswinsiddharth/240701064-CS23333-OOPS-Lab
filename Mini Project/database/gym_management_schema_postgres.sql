CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'TRAINER', 'MEMBER')),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_role ON users(role);
CREATE TABLE IF NOT EXISTS membership_plans (
    id SERIAL PRIMARY KEY,
    plan_name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    duration_in_months INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_plan_name ON membership_plans(plan_name);
CREATE INDEX IF NOT EXISTS idx_is_active ON membership_plans(is_active);
CREATE TABLE IF NOT EXISTS members (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    emergency_contact VARCHAR(100),
    medical_conditions TEXT,
    membership_plan_id INT NOT NULL,
    membership_start_date DATE NOT NULL,
    membership_end_date DATE NOT NULL,
    membership_status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (membership_status IN ('ACTIVE', 'EXPIRED', 'SUSPENDED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (membership_plan_id) REFERENCES membership_plans(id)
);

CREATE INDEX IF NOT EXISTS idx_member_user_id ON members(user_id);
CREATE INDEX IF NOT EXISTS idx_membership_status ON members(membership_status);
CREATE INDEX IF NOT EXISTS idx_membership_end_date ON members(membership_end_date);
CREATE TABLE IF NOT EXISTS trainers (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    specialization VARCHAR(200),
    certifications TEXT,
    hourly_rate DECIMAL(10, 2),
    availability TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_trainer_user_id ON trainers(user_id);
CREATE INDEX IF NOT EXISTS idx_specialization ON trainers(specialization);
CREATE TABLE IF NOT EXISTS classes (
    id SERIAL PRIMARY KEY,
    class_name VARCHAR(100) NOT NULL,
    description TEXT,
    trainer_id INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    max_capacity INT NOT NULL DEFAULT 10,
    current_bookings INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (trainer_id) REFERENCES trainers(id) ON DELETE CASCADE,
    CONSTRAINT chk_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_booking_capacity CHECK (current_bookings <= max_capacity)
);
CREATE INDEX IF NOT EXISTS idx_class_trainer_id ON classes(trainer_id);
CREATE INDEX IF NOT EXISTS idx_start_time ON classes(start_time);
CREATE INDEX IF NOT EXISTS idx_class_status ON classes(status);
CREATE TABLE IF NOT EXISTS class_bookings (
    id SERIAL PRIMARY KEY,
    class_id INT NOT NULL,
    member_id INT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED', 'ATTENDED')),
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    UNIQUE (class_id, member_id)
);
CREATE INDEX IF NOT EXISTS idx_booking_class_id ON class_bookings(class_id);
CREATE INDEX IF NOT EXISTS idx_booking_member_id ON class_bookings(member_id);
CREATE INDEX IF NOT EXISTS idx_booking_date ON class_bookings(booking_date);
CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    member_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'CARD', 'ONLINE')),
    payment_type VARCHAR(20) NOT NULL CHECK (payment_type IN ('MEMBERSHIP', 'CLASS', 'OTHER')),
    status VARCHAR(20) DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255),
    
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_payment_member_id ON payments(member_id);
CREATE INDEX IF NOT EXISTS idx_payment_date ON payments(payment_date);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payment_type ON payments(payment_type);
INSERT INTO membership_plans (plan_name, description, price, duration_in_months) VALUES
('Basic Monthly', 'Access to gym facilities during regular hours', 29.99, 1),
('Premium Monthly', 'Full access with group classes and premium amenities', 49.99, 1),
('Basic Annual', 'Annual basic membership with 2 months free', 299.99, 12),
('Premium Annual', 'Annual premium membership with 2 months free', 499.99, 12),
('Student Monthly', 'Discounted rate for students with valid ID', 19.99, 1)
ON CONFLICT DO NOTHING;
INSERT INTO users (username, password, email, role, first_name, last_name, phone) VALUES
('admin', 'admin123', 'admin@gymsystem.com', 'ADMIN', 'System', 'Administrator', '555-0000')
ON CONFLICT DO NOTHING;
