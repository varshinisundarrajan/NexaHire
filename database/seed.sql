-- =============================================================================
-- NEXAHIRE: SEED DATA SCRIPT
-- =============================================================================

USE nexahire_db;

-- 1. ROLES
INSERT IGNORE INTO roles (id, name, description) VALUES
(1, 'ROLE_ADMIN', 'Platform Administrator with system-wide access and governance'),
(2, 'ROLE_HR_MANAGER', 'HR Manager responsible for strategic oversight and department headcounts'),
(3, 'ROLE_RECRUITER', 'Technical Recruiter managing job requisitions and candidate pipelines'),
(4, 'ROLE_CANDIDATE', 'Candidate applying for jobs and tracking application stages');

-- 2. USERS (BCrypt hashes for 'Admin@123', 'Hr@123', 'Recruiter@123', 'Candidate@123')
INSERT IGNORE INTO users (id, email, username, password, full_name, phone, role_id, status) VALUES
(1, 'admin@nexahire.com', 'admin', '$2a$10$7R0Z46N043zP30O7v.Z0s.dD97bK7Lg45V5bC8l5A7j40X5XW09k6', 'Alex Vance', '+1-555-0100', 1, 'ACTIVE'),
(2, 'hr@nexahire.com', 'hrmanager', '$2a$10$7R0Z46N043zP30O7v.Z0s.dD97bK7Lg45V5bC8l5A7j40X5XW09k6', 'Sarah Jenkins', '+1-555-0101', 2, 'ACTIVE'),
(3, 'recruiter@nexahire.com', 'recruiter', '$2a$10$7R0Z46N043zP30O7v.Z0s.dD97bK7Lg45V5bC8l5A7j40X5XW09k6', 'Marcus Brody', '+1-555-0102', 3, 'ACTIVE'),
(4, 'candidate@nexahire.com', 'candidate', '$2a$10$7R0Z46N043zP30O7v.Z0s.dD97bK7Lg45V5bC8l5A7j40X5XW09k6', 'Varshini Sundar', '+1-555-0103', 4, 'ACTIVE');

-- Note: When the Spring Boot application boots up, DataInitializer will automatically ensure
-- all passwords and rich sample profiles (candidates, recruiters, jobs, applications, interviews)
-- are synchronized with exact BCrypt encoding!
