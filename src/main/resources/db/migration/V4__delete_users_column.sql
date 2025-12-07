-- Drop unused columns from users table
ALTER TABLE users
    DROP COLUMN work_time,
    DROP COLUMN work_way,
    DROP COLUMN sido,
    DROP COLUMN year_count;

-- Rename detail column to bio and change size
ALTER TABLE users
    CHANGE COLUMN detail bio VARCHAR(1000);

-- Create user_skills table
CREATE TABLE user_skills (
    user_id BINARY(16) NOT NULL,
    skill VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, skill)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;