Drop Database IF EXISTS rfo_db;
-- Create database
CREATE DATABASE IF NOT EXISTS rfo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rfo_db;

-- ============================================
-- User table
-- ============================================
CREATE TABLE IF NOT EXISTS user
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'User ID',
    username      VARCHAR(32)  NOT NULL COMMENT 'User name',
    password      VARCHAR(255) NOT NULL COMMENT 'User password',
    email         VARCHAR(100) NOT NULL COMMENT 'User email',
    role          TINYINT      NOT NULL COMMENT 'User role: 1 is admin, 2 is marker',
    delete_status TINYINT DEFAULT 0 COMMENT 'Delete status: 1 is deleted, 0 is not deleted',
    avatar        VARCHAR(255)     DEFAULT NULL COMMENT 'User avatar URL'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='User table';

-- ============================================
-- Student table
-- ============================================
CREATE TABLE IF NOT EXISTS student
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    student_id    BIGINT       NOT NULL COMMENT 'Student ID',
    email         VARCHAR(100) NOT NULL COMMENT 'Student email',
    first_name    VARCHAR(100) NOT NULL COMMENT 'Student first name',
    surname       VARCHAR(100) NOT NULL COMMENT 'Student last name',
    delete_status TINYINT DEFAULT 0 COMMENT 'Delete status: 1 is deleted, 0 is not deleted'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Student table';

-- ============================================
-- Subject table
-- ============================================
CREATE TABLE IF NOT EXISTS subject
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Subject ID',
    name          VARCHAR(100) NOT NULL COMMENT 'Subject name',
    description   VARCHAR(500) COMMENT 'Subject description',
    delete_status TINYINT DEFAULT 0 COMMENT 'Delete status: 1 is deleted, 0 is not deleted'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Subject table';

-- ============================================
-- Template table
-- ============================================
CREATE TABLE IF NOT EXISTS template
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Template ID',
    template_name VARCHAR(100) NOT NULL COMMENT 'Template name',
    creator_id    BIGINT COMMENT 'creator ID create the template',
    delete_status TINYINT DEFAULT 0 COMMENT 'Delete status: 1 is deleted, 0 is not deleted'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='template table';

-- ============================================
-- Template element table
-- ============================================
CREATE TABLE IF NOT EXISTS template_element
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Template element ID',
    name            VARCHAR(100) NOT NULL COMMENT 'Template element name',
    weighting       INT          NOT NULL COMMENT 'Weighting',
    maximum_mark    INT          NOT NULL COMMENT 'Maximum mark',
    mark_increments DOUBLE       NOT NULL COMMENT 'Mark increments',
    delete_status   TINYINT DEFAULT 0 COMMENT 'Delete status: 1 is deleted, 0 is not deleted'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Template element table';

-- Insert test data (using INSERT IGNORE to handle duplicates)
INSERT IGNORE INTO template_element (id, name, weighting, maximum_mark, mark_increments) VALUES
    (1, 'Voice, Pace and Confidence', 15, 100, 0.5),
    (2, 'Presentation Structure', 15, 100, 0.5),
    (3, 'Quality of Slides/Visual Aids', 15, 100, 0.5),
    (4, 'Knowledge of the Material', 20, 100, 0.5),
    (5, 'Content', 20, 100, 0.5),
    (6, 'Concluding Remarks', 10, 100, 0.5),
    (7, 'Other Comments', 5, 100, 0.5);

-- ============================================
-- Comment library table
-- ============================================
CREATE TABLE IF NOT EXISTS comment_library
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'comment ID',
    template_element_id BIGINT       NOT NULL COMMENT 'Linked template element ID',
    content             VARCHAR(100) NOT NULL COMMENT 'content',
    comment_type        INT COMMENT 'comment type',
    delete_status       TINYINT DEFAULT 0 COMMENT 'Delete status: 1 is deleted, 0 is not deleted'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='comment table';

-- Insert test data for comment library
-- Comment types: 0=negative, 1=neutral, 2=positive
INSERT IGNORE INTO comment_library (template_element_id, content, comment_type)
VALUES
    -- Voice, Pace and Confidence (template_element_id=1)
    (1, 'Excellent vocal clarity and confident delivery throughout.', 2),
    (1, 'Good pacing, allowing audience to absorb information.', 2),
    (1, 'Speaking too quickly, hard to follow at times.', 0),
    (1, 'Low voice volume, difficult to hear in the back.', 0),
    (1, 'Consider varying your pace for emphasis.', 1),
    -- Presentation Structure (template_element_id=2)
    (2, 'Well-organized with clear introduction and conclusion.', 2),
    (2, 'Logical flow between sections, easy to follow.', 2),
    (2, 'Lacks clear structure, transitions are abrupt.', 0),
    (2, 'Missing introduction or conclusion.', 0),
    (2, 'Consider adding transition slides between sections.', 1),
    -- Quality of Slides/Visual Aids (template_element_id=3)
    (3, 'Professional slides with clear, readable fonts.', 2),
    (3, 'Effective use of diagrams and visual elements.', 2),
    (3, 'Too much text on slides, overwhelming.', 0),
    (3, 'Poor color contrast, hard to read.', 0),
    (3, 'Consider using more visual aids to support points.', 1),
    -- Knowledge of the Material (template_element_id=4)
    (4, 'Demonstrates deep understanding of the topic.', 2),
    (4, 'Confidently answered all questions from audience.', 2),
    (4, 'Struggled with basic concepts, needs more preparation.', 0),
    (4, 'Unable to answer clarification questions.', 0),
    (4, 'Review core concepts before next presentation.', 1),
    -- Content (template_element_id=5)
    (5, 'Comprehensive coverage of all required topics.', 2),
    (5, 'Excellent use of examples and case studies.', 2),
    (5, 'Content is too shallow, lacks depth.', 0),
    (5, 'Missing key components of the assignment.', 0),
    (5, 'Consider adding more real-world examples.', 1),
    -- Concluding Remarks (template_element_id=6)
    (6, 'Strong conclusion that summarizes key points well.', 2),
    (6, 'Memorable closing statement.', 2),
    (6, 'Conclusion is rushed and incomplete.', 0),
    (6, 'No clear takeaway message.', 0),
    (6, 'Strengthen conclusion with call-to-action.', 1),
    -- Other Comments (template_element_id=7)
    (7, 'Overall excellent presentation.', 2),
    (7, 'Good effort, shows improvement from last time.', 2),
    (7, 'Needs significant improvement in multiple areas.', 0),
    (7, 'Practice more before next presentation.', 1);

-- ============================================
-- Assessment criteria table
-- ============================================
CREATE TABLE IF NOT EXISTS assessment_criteria
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Assessment criteria ID',
    template_id         BIGINT NOT NULL COMMENT 'Linked template ID',
    template_element_id BIGINT NOT NULL COMMENT 'Linked template element ID',
    template_element_name VARCHAR(100) COMMENT 'Template element name',
    weighting           INT    NOT NULL COMMENT 'Weighting',
    maximum_mark        INT    NOT NULL COMMENT 'Maximum mark',
    mark_increments     DOUBLE NOT NULL COMMENT 'Mark increments',
    delete_status       TINYINT DEFAULT 0 COMMENT 'Soft delete: 1=deleted, 0=active'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Assessment criteria table';

-- ============================================
-- Project table
-- ============================================
CREATE TABLE IF NOT EXISTS project
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Project ID',
    project_name  VARCHAR(100) NOT NULL COMMENT 'Project name',
    countdown     BIGINT COMMENT 'Countdown time for the project presentation',
    subject_id    BIGINT       NOT NULL COMMENT 'Subject ID associated with the project',
    template_id   BIGINT COMMENT 'template_id',
    project_type  VARCHAR(20)  NOT NULL DEFAULT 'individual' COMMENT 'Project type: individual or group',
    delete_status TINYINT DEFAULT 0 COMMENT 'Delete status: 1 is deleted, 0 is not deleted'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Project table';

-- ============================================
-- Group table
-- ============================================
CREATE TABLE IF NOT EXISTS project_group
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Group ID',
    group_name    VARCHAR(100) NOT NULL COMMENT 'Group name',
    project_id    BIGINT       NOT NULL COMMENT 'project_id',
    delete_status TINYINT DEFAULT 0 COMMENT 'Delete status: 1 is deleted, 0 is not deleted'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Group table';

-- ============================================
-- Group-student association table
-- ============================================
CREATE TABLE IF NOT EXISTS group_student
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Table ID',
    student_id    BIGINT COMMENT 'Student ID',
    group_id      BIGINT COMMENT 'group ID',
    delete_status TINYINT DEFAULT 0 COMMENT 'Delete status: 1 is deleted, 0 is not deleted'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='group-student association table';

-- ============================================
-- Student-project association table
-- ============================================
CREATE TABLE IF NOT EXISTS student_project
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    student_id BIGINT COMMENT 'Student ID',
    subject_id BIGINT COMMENT 'Subject ID',
    project_id BIGINT COMMENT 'Project ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Student-project association table';

-- ============================================
-- Student-Subject association table
-- ============================================
CREATE TABLE IF NOT EXISTS student_subject
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    student_id BIGINT COMMENT 'Student ID',
    subject_id BIGINT COMMENT 'Subject ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Student-Subject association table';

-- ============================================
-- User-project association table
-- ============================================
CREATE TABLE IF NOT EXISTS user_project
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    user_id    BIGINT COMMENT 'User ID',
    subject_id BIGINT COMMENT 'Subject ID',
    project_id BIGINT COMMENT 'Project ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='User-project association table';

-- ============================================
-- User-Subject association table
-- ============================================
CREATE TABLE IF NOT EXISTS user_subject
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    user_id    BIGINT COMMENT 'User ID',
    subject_id BIGINT COMMENT 'Subject ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='User-Subject association table';

-- ============================================
-- Mark record table
-- ============================================
CREATE TABLE IF NOT EXISTS mark_record
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Mark record ID',
    project_id  BIGINT       NOT NULL COMMENT 'Linked project ID',
    student_id  BIGINT       NOT NULL COMMENT 'Student primary key (references student.id)',
    marker_id   BIGINT       NOT NULL COMMENT 'Linked user ID (marker)',
    total_score DECIMAL(6,2) COMMENT 'Weighted total score, calculated on submission',
    group_score DECIMAL(6,2) COMMENT 'Group score from group_mark_record, nullable',
    mark_time   DATETIME     COMMENT 'Submission timestamp',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = 'Mark record table';

  -- ============================================
  -- Mark detail table
  -- ============================================
CREATE TABLE IF NOT EXISTS mark_detail
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Mark detail ID',
    mark_record_id BIGINT       NOT NULL COMMENT 'Linked mark_record ID',
    criteria_id    BIGINT       NOT NULL COMMENT 'Linked assessment_criteria ID',
    score          DECIMAL(6,2) COMMENT 'Score for this criteria',
    comment        TEXT         COMMENT 'Comment for this criteria',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '0=first mark, 1=changed',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_criteria (mark_record_id, criteria_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = 'Mark detail table';


-- ============================================
-- Group mark record table
-- ============================================
CREATE TABLE IF NOT EXISTS group_mark_record
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Group mark record ID',
    project_id  BIGINT       NOT NULL COMMENT 'Linked project ID',
    group_id    BIGINT       NOT NULL COMMENT 'Linked project_group ID',
    marker_id   BIGINT       NOT NULL COMMENT 'user.id of the marker who wrote this group comment',
    comment     TEXT         COMMENT 'Overall comment for the whole group from this marker',
    mark_time   DATETIME     COMMENT 'Last updated timestamp',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_group_marker (project_id, group_id, marker_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Group mark record table';

-- ============================================
-- Marker-student assignment table (per-student marker for individual projects)
-- ============================================
CREATE TABLE IF NOT EXISTS marker_student
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    project_id BIGINT NOT NULL COMMENT 'Linked project ID',
    student_id BIGINT NOT NULL COMMENT 'student.id (primary key)',
    marker_id  BIGINT NOT NULL COMMENT 'user.id of the assigned marker'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Marker-student assignment table';

-- ============================================
-- Marker-group assignment table (per-group marker for group projects)
-- ============================================
CREATE TABLE IF NOT EXISTS marker_group
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    project_id BIGINT NOT NULL COMMENT 'Linked project ID',
    group_id   BIGINT NOT NULL COMMENT 'project_group.id',
    marker_id  BIGINT NOT NULL COMMENT 'user.id of the assigned marker'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Marker-group assignment table';

-- ============================================
-- Final mark table (admin-set final scores)
-- ============================================
CREATE TABLE IF NOT EXISTS final_mark
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Final mark ID',
    project_id  BIGINT       NOT NULL COMMENT 'Linked project ID',
    student_id  BIGINT       DEFAULT NULL COMMENT 'student.id for individual projects',
    group_id    BIGINT       DEFAULT NULL COMMENT 'project_group.id for group projects',
    final_score DECIMAL(6,2) DEFAULT NULL COMMENT 'Admin-set final score',
    is_locked   TINYINT      NOT NULL DEFAULT 0 COMMENT '0=unlocked, 1=locked',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Admin final mark table';
