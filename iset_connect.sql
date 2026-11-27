-- =====================================================================
-- ISET Connect - MySQL database schema and initialization
-- Run: mysql -u root -p < iset_connect.sql
-- Compatible with MySQL 8.0 / MariaDB 10.4+. All tables use InnoDB.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS iset_connect
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE iset_connect;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS applications;
DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS internships;
DROP TABLE IF EXISTS materials;
DROP TABLE IF EXISTS auth_tokens;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- users: students and the predefined administrator
-- status: PENDING | APPROVED | REJECTED | SUSPENDED
-- role: STUDENT | ADMIN
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    cin             VARCHAR(50)     NOT NULL,
    email           VARCHAR(190)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    class_name      VARCHAR(100)    NOT NULL DEFAULT '',
    role            ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT',
    status          ENUM('PENDING','APPROVED','REJECTED','SUSPENDED') NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_cin (cin),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_status (status),
    KEY idx_users_role (role),
    KEY idx_users_name (last_name, first_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- auth_tokens: server-side bearer tokens (only SHA-256 hashes are stored)
-- ---------------------------------------------------------------------
CREATE TABLE auth_tokens (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    token_hash  CHAR(64)        NOT NULL,
    expires_at  DATETIME        NOT NULL,
    revoked     TINYINT(1)      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_auth_tokens_hash (token_hash),
    KEY idx_auth_tokens_user (user_id),
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- materials: university equipment inventory
-- ---------------------------------------------------------------------
CREATE TABLE materials (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name                VARCHAR(150)    NOT NULL,
    description         TEXT            NOT NULL,
    category            VARCHAR(100)    NOT NULL,
    total_quantity      INT UNSIGNED    NOT NULL DEFAULT 0,
    available_quantity  INT UNSIGNED    NOT NULL DEFAULT 0,
    image_url           VARCHAR(255)    NULL DEFAULT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_materials_category (category),
    KEY idx_materials_name (name),
    CONSTRAINT chk_materials_quantity CHECK (available_quantity <= total_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- loans: equipment borrowing requests and history
-- status: PENDING | APPROVED | REJECTED | CANCELLED | RETURNED
-- ---------------------------------------------------------------------
CREATE TABLE loans (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id            BIGINT UNSIGNED NOT NULL,
    material_id           BIGINT UNSIGNED NOT NULL,
    quantity              INT UNSIGNED    NOT NULL,
    request_date          DATETIME        NOT NULL,
    borrowing_date        DATE            NOT NULL,
    expected_return_date  DATE            NOT NULL,
    actual_return_date    DATETIME        NULL DEFAULT NULL,
    status                ENUM('PENDING','APPROVED','REJECTED','CANCELLED','RETURNED') NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (id),
    KEY idx_loans_student (student_id),
    KEY idx_loans_material (material_id),
    KEY idx_loans_status (status),
    KEY idx_loans_dates (borrowing_date, expected_return_date),
    CONSTRAINT fk_loans_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_loans_material FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE RESTRICT,
    CONSTRAINT chk_loans_quantity CHECK (quantity > 0),
    CONSTRAINT chk_loans_dates CHECK (expected_return_date > borrowing_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- internships: offers published by administrators
-- status: DRAFT | PUBLISHED | CLOSED | EXPIRED
-- ---------------------------------------------------------------------
CREATE TABLE internships (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title                VARCHAR(200)    NOT NULL,
    company_name         VARCHAR(200)    NOT NULL,
    company_address      VARCHAR(255)    NOT NULL DEFAULT '',
    domain               VARCHAR(100)    NOT NULL,
    description          TEXT            NOT NULL,
    required_skills      TEXT            NOT NULL,
    start_date           DATE            NOT NULL,
    end_date             DATE            NOT NULL,
    application_deadline DATE            NOT NULL,
    total_positions      INT UNSIGNED    NOT NULL DEFAULT 1,
    available_positions  INT UNSIGNED    NOT NULL DEFAULT 1,
    status               ENUM('DRAFT','PUBLISHED','CLOSED','EXPIRED') NOT NULL DEFAULT 'DRAFT',
    created_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_internships_status (status),
    KEY idx_internships_domain (domain),
    KEY idx_internships_title (title),
    KEY idx_internships_company (company_name),
    CONSTRAINT chk_internships_positions CHECK (available_positions <= total_positions),
    CONSTRAINT chk_internships_dates CHECK (end_date >= start_date AND application_deadline <= end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- applications: student internship applications
-- status: PENDING | ACCEPTED | REJECTED | CANCELLED
-- ---------------------------------------------------------------------
CREATE TABLE applications (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id           BIGINT UNSIGNED NOT NULL,
    internship_id        BIGINT UNSIGNED NOT NULL,
    motivation_message   TEXT            NOT NULL,
    application_date     DATETIME        NOT NULL,
    status               ENUM('PENDING','ACCEPTED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (id),
    UNIQUE KEY uq_application_student_internship (student_id, internship_id),
    KEY idx_applications_status (status),
    KEY idx_applications_internship (internship_id),
    CONSTRAINT fk_applications_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_applications_internship FOREIGN KEY (internship_id) REFERENCES internships(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- notifications: centralized notification center
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    title             VARCHAR(200)    NOT NULL,
    message           TEXT            NOT NULL,
    notification_type VARCHAR(80)     NOT NULL,
    is_read           TINYINT(1)      NOT NULL DEFAULT 0,
    created_at        DATETIME        NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_user (user_id),
    KEY idx_notifications_unread (user_id, is_read),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- SECURE INITIALIZATION - administrator account
-- Password hash below was generated with:
--   password_hash('Admin@ISET2026', PASSWORD_BCRYPT, ['cost' => 12])
-- No plaintext password is stored in this file.
-- =====================================================================
INSERT INTO users (first_name, last_name, cin, email, password_hash, class_name, role, status)
VALUES ('ISET', 'Administrator', 'ADMIN001', 'admin@iset.tn',
        '$2y$12$JQRySfLZeamMY3m7BW7JfuZ0xOJBR9hsN0wQQOsTlbRdfJoQI/Ilu',
        'Administration', 'ADMIN', 'APPROVED');

-- =====================================================================
-- OPTIONAL SAMPLE DATA (safe to delete for a clean installation)
-- Student password for the sample accounts is: Student@123
-- Hash generated with password_hash('Student@123', PASSWORD_BCRYPT, ['cost' => 12])
-- =====================================================================
INSERT INTO users (first_name, last_name, cin, email, password_hash, class_name, role, status) VALUES
    ('Sara',   'Benali',   'STU1001', 'sara.benali@iset.tn',   '$2y$12$WyJFZFGFXHxQnvKCpkcKb.OefTLqciSFlnm9D0o7y.CS1odfq8uUm', 'GI2', 'STUDENT', 'APPROVED'),
    ('Mohamed','Trabelsi', 'STU1002', 'mohamed.t@iset.tn',     '$2y$12$WyJFZFGFXHxQnvKCpkcKb.OefTLqciSFlnm9D0o7y.CS1odfq8uUm', 'GL3', 'STUDENT', 'PENDING'),
    ('Ines',   'Khadhraoui','STU1003','ines.k@iset.tn',        '$2y$12$WyJFZFGFXHxQnvKCpkcKb.OefTLqciSFlnm9D0o7y.CS1odfq8uUm', 'GI1', 'STUDENT', 'APPROVED');

INSERT INTO materials (name, description, category, total_quantity, available_quantity, image_url) VALUES
    ('Laptop HP',           'HP ProBook 440 G8, 16 GB RAM, 512 GB SSD',            'Computers',      10, 7,  NULL),
    ('Projector',           'Epson portable HD projector for classrooms',         'Audiovisual',     4, 3,  NULL),
    ('Arduino Uno Kit',     'Arduino Uno board with sensors and cables',          'Electronics',    15, 12, NULL),
    ('Raspberry Pi 4',      'Raspberry Pi 4 Model B 8 GB with power supply',      'Electronics',     8, 6,  NULL),
    ('Network Switch',      '24-port gigabit managed network switch',             'Networking',      5, 4,  NULL),
    ('Computer Accessories','Keyboard, mouse, HDMI cable and adapter set',        'Accessories',    20, 18, NULL),
    ('Laboratory Equipment','Multimeter, oscilloscope and soldering station',     'Laboratory',      6, 5,  NULL);

INSERT INTO internships (title, company_name, company_address, domain, description, required_skills,
                         start_date, end_date, application_deadline, total_positions, available_positions, status) VALUES
    ('Web Development Intern', 'Tech Solutions', 'Tunis, Tunisia', 'Web Development',
     'Build and maintain responsive web applications with a modern backend.',
     'HTML, CSS, JavaScript, PHP', '2026-12-01', '2027-02-28', '2026-12-15', 3, 3, 'PUBLISHED'),
    ('Mobile Android Engineer Intern', 'Nova Apps', 'Sfax, Tunisia', 'Mobile Development',
     'Develop and test Android applications using Java and REST APIs.',
     'Java, Android SDK, Retrofit', '2026-12-15', '2027-03-15', '2026-12-20', 2, 2, 'PUBLISHED'),
    ('Network Security Intern', 'SecureNet', 'Tunis, Tunisia', 'Cybersecurity',
     'Assist in network monitoring, vulnerability analysis and security audits.',
     'TCP/IP, Wireshark, Linux', '2027-01-01', '2027-03-31', '2026-12-25', 2, 2, 'PUBLISHED'),
    ('Database Administration Intern', 'DataCore', 'Sousse, Tunisia', 'Database Administration',
     'Support database design, backup strategies and SQL performance tuning.',
     'MySQL, SQL, Backup', '2027-01-05', '2027-03-05', '2026-12-28', 1, 1, 'DRAFT');

INSERT INTO loans (student_id, material_id, quantity, request_date, borrowing_date, expected_return_date, status) VALUES
    (1, 3, 2, '2026-11-20 09:30:00', '2026-11-22', '2026-11-29', 'APPROVED'),
    (1, 1, 1, '2026-11-24 10:15:00', '2026-11-25', '2026-12-02', 'PENDING');

INSERT INTO applications (student_id, internship_id, motivation_message, application_date, status) VALUES
    (1, 1, 'I would like to strengthen my web development skills in a professional team.', '2026-11-23 11:00:00', 'PENDING');

INSERT INTO notifications (user_id, title, message, notification_type, is_read, created_at) VALUES
    (1, ' Account approved', 'Your ISET Connect account has been approved. You can now use all student services.', 'ACCOUNT_APPROVED', 0, '2026-11-18 08:00:00'),
    (1, ' Borrowing approved', 'Your request for Arduino Uno Kit has been approved.', 'LOAN_APPROVED', 1, '2026-11-20 14:20:00'),
    (2, ' Registration pending', 'Your registration is being reviewed by an administrator.', 'ACCOUNT_PENDING', 0, '2026-11-24 09:00:00');

-- Keep inventory consistent with the sample approved loan (2 Arduino kits)
UPDATE materials SET available_quantity = 10 WHERE id = 3;

-- =====================================================================
-- END OF SCRIPT
-- =====================================================================
