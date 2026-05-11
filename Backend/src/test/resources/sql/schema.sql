-- ============================================
-- RFO Test Database Schema (H2 Compatible)
-- Converted from init.sql for H2 in-memory testing
-- ============================================

-- Drop tables in reverse dependency order to avoid FK issues
DROP TABLE IF EXISTS group_mark_detail;
DROP TABLE IF EXISTS final_mark;
DROP TABLE IF EXISTS group_mark_record;
DROP TABLE IF EXISTS mark_detail;
DROP TABLE IF EXISTS mark_record;
DROP TABLE IF EXISTS group_student;
DROP TABLE IF EXISTS student_project;
DROP TABLE IF EXISTS student_subject;
DROP TABLE IF EXISTS user_project;
DROP TABLE IF EXISTS user_subject;
DROP TABLE IF EXISTS project_group;
DROP TABLE IF EXISTS assessment_criteria;
DROP TABLE IF EXISTS comment_library;
DROP TABLE IF EXISTS template_element;
DROP TABLE IF EXISTS template;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS subject;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS user;

-- ============================================
-- User table
-- ============================================
CREATE TABLE IF NOT EXISTS user
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,
    password      VARCHAR(255) NOT NULL,
    email         VARCHAR(100) NOT NULL,
    role          INT          NOT NULL,
    avatar        VARCHAR(500),
    delete_status INT DEFAULT 0
);

-- ============================================
-- Student table
-- ============================================
CREATE TABLE IF NOT EXISTS student
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT       NOT NULL,
    email         VARCHAR(100) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    surname       VARCHAR(100) NOT NULL,
    delete_status INT DEFAULT 0
);

-- ============================================
-- Subject table
-- ============================================
CREATE TABLE IF NOT EXISTS subject
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    delete_status INT DEFAULT 0
);

-- ============================================
-- Template table
-- ============================================
CREATE TABLE IF NOT EXISTS template
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    creator_id    BIGINT,
    delete_status INT DEFAULT 0
);

-- ============================================
-- Template element table
-- ============================================
CREATE TABLE IF NOT EXISTS template_element
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    weighting       INT          NOT NULL,
    maximum_mark    INT          NOT NULL,
    mark_increments DOUBLE       NOT NULL,
    delete_status   INT DEFAULT 0
);

-- ============================================
-- Comment library table
-- ============================================
CREATE TABLE IF NOT EXISTS comment_library
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_element_id BIGINT       NOT NULL,
    content             VARCHAR(100) NOT NULL,
    comment_type        INT,
    delete_status       INT DEFAULT 0
);

-- ============================================
-- Assessment criteria table
-- ============================================
CREATE TABLE IF NOT EXISTS assessment_criteria
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id           BIGINT NOT NULL,
    template_element_id   BIGINT NOT NULL,
    template_element_name VARCHAR(100),
    weighting             INT    NOT NULL,
    maximum_mark          INT    NOT NULL,
    mark_increments       DOUBLE NOT NULL,
    delete_status         INT DEFAULT 0
);

-- ============================================
-- Project table
-- ============================================
CREATE TABLE IF NOT EXISTS project
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_name  VARCHAR(100) NOT NULL,
    countdown     BIGINT,
    subject_id    BIGINT       NOT NULL,
    template_id   BIGINT,
    project_type  VARCHAR(20)  NOT NULL DEFAULT 'individual',
    delete_status INT DEFAULT 0
);

-- ============================================
-- Group table
-- ============================================
CREATE TABLE IF NOT EXISTS project_group
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name    VARCHAR(100) NOT NULL,
    project_id    BIGINT       NOT NULL,
    delete_status INT DEFAULT 0
);

-- ============================================
-- Group-student association table
-- ============================================
CREATE TABLE IF NOT EXISTS group_student
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT,
    group_id      BIGINT,
    delete_status INT DEFAULT 0
);

-- ============================================
-- Student-project association table
-- ============================================
CREATE TABLE IF NOT EXISTS student_project
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    subject_id BIGINT,
    project_id BIGINT
);

-- ============================================
-- Student-Subject association table
-- ============================================
CREATE TABLE IF NOT EXISTS student_subject
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    subject_id BIGINT
);

-- ============================================
-- User-project association table
-- ============================================
CREATE TABLE IF NOT EXISTS user_project
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT,
    subject_id BIGINT,
    project_id BIGINT
);

-- ============================================
-- User-Subject association table
-- ============================================
CREATE TABLE IF NOT EXISTS user_subject
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT,
    subject_id BIGINT
);

-- ============================================
-- Marker-student assignment table
-- ============================================
CREATE TABLE IF NOT EXISTS marker_student
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    student_id BIGINT,
    marker_id  BIGINT
);

-- ============================================
-- Marker-group assignment table
-- ============================================
CREATE TABLE IF NOT EXISTS marker_group
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    group_id   BIGINT,
    marker_id  BIGINT
);

-- ============================================
-- Mark record table
-- ============================================
CREATE TABLE IF NOT EXISTS mark_record
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT         NOT NULL,
    student_id  BIGINT         NOT NULL,
    marker_id   BIGINT         NOT NULL,
    total_score DECIMAL(6, 2),
    group_score DECIMAL(6, 2),
    mark_time   DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Mark detail table
-- ============================================
CREATE TABLE IF NOT EXISTS mark_detail
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    mark_record_id BIGINT         NOT NULL,
    criteria_id    BIGINT         NOT NULL,
    score          DECIMAL(6, 2),
    comment        CLOB,
    status         INT            NOT NULL DEFAULT 0,
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_record_criteria UNIQUE (mark_record_id, criteria_id)
);

-- ============================================
-- Group mark record table
-- ============================================
CREATE TABLE IF NOT EXISTS group_mark_record
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT         NOT NULL,
    group_id    BIGINT         NOT NULL,
    marker_id   BIGINT         NOT NULL,
    comment     CLOB,
    mark_time   DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_project_group_marker UNIQUE (project_id, group_id, marker_id)
);

-- ============================================
-- Final mark table
-- ============================================
CREATE TABLE IF NOT EXISTS final_mark
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT         NOT NULL,
    student_id  BIGINT,
    group_id    BIGINT,
    final_score DECIMAL(6, 2),
    is_locked   INT            NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Group mark detail table
-- ============================================
CREATE TABLE IF NOT EXISTS group_mark_detail
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_mark_record_id BIGINT         NOT NULL,
    criteria_id          BIGINT         NOT NULL,
    score                DECIMAL(6, 2),
    comment              CLOB,
    status               INT            NOT NULL DEFAULT 0,
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_record_criteria UNIQUE (group_mark_record_id, criteria_id)
);
