-- ============================================
-- RFO Test Data (H2 Compatible)
-- DELETE-before-INSERT pattern for test isolation
-- ============================================

-- Clear all tables in reverse dependency order
DELETE FROM group_mark_detail;
DELETE FROM final_mark;
DELETE FROM group_mark_record;
DELETE FROM mark_detail;
DELETE FROM mark_record;
DELETE FROM marker_student;
DELETE FROM marker_group;
DELETE FROM group_student;
DELETE FROM student_project;
DELETE FROM student_subject;
DELETE FROM user_project;
DELETE FROM user_subject;
DELETE FROM project_group;
DELETE FROM assessment_criteria;
DELETE FROM comment_library;
DELETE FROM template_element;
DELETE FROM template;
DELETE FROM project;
DELETE FROM subject;
DELETE FROM student;
DELETE FROM user;

-- ============================================
-- User data
-- Passwords: admin=admin123, marker1-5=marker123
-- ============================================
INSERT INTO user (id, username, password, email, role) VALUES
    (1, 'admin',   '$2a$10$aET9KZ4RTEmAxYIt41PIguYSYGN80610oTUof/mISIXt38VLEgkya', 'admin@example.com', 1),
    (2, 'marker1', '$2a$10$CbHYLHnou0.pJDawc.lqOO.30swJ3F3UUI48X9YWHwt7lba3KIgcG', 'marker1@example.com', 2),
    (3, 'marker2', '$2a$10$WPnlJxpT7UWUc5aAgOJiKuFxmgpINO44V0GcVB4/BVXqrFVXXXjue', 'marker2@example.com', 2),
    (4, 'marker3', '$2a$10$WPnlJxpT7UWUc5aAgOJiKuFxmgpINO44V0GcVB4/BVXqrFVXXXjue', 'marker3@example.com', 2),
    (5, 'marker4', '$2a$10$WPnlJxpT7UWUc5aAgOJiKuFxmgpINO44V0GcVB4/BVXqrFVXXXjue', 'marker4@example.com', 2),
    (6, 'marker5', '$2a$10$WPnlJxpT7UWUc5aAgOJiKuFxmgpINO44V0GcVB4/BVXqrFVXXXjue', 'marker5@example.com', 2);

-- ============================================
-- Student data
-- ============================================
INSERT INTO student (id, student_id, email, first_name, surname) VALUES
    (1,  1510000, 'alice.chen@student.unimelb.edu.au',    'Alice',   'Chen'),
    (2,  1510001, 'bob.smith@student.unimelb.edu.au',     'Bob',     'Smith'),
    (3,  1510002, 'charlie.wang@student.unimelb.edu.au',  'Charlie', 'Wang'),
    (4,  1510003, 'diana.lee@student.unimelb.edu.au',     'Diana',   'Lee'),
    (5,  1510004, 'emma.brown@student.unimelb.edu.au',    'Emma',    'Brown'),
    (6,  1510005, 'frank.zhang@student.unimelb.edu.au',   'Frank',   'Zhang'),
    (7,  1510006, 'grace.wilson@student.unimelb.edu.au',  'Grace',   'Wilson'),
    (8,  1510007, 'henry.liu@student.unimelb.edu.au',     'Henry',   'Liu'),
    (9,  1510008, 'iris.johnson@student.unimelb.edu.au',  'Iris',    'Johnson'),
    (10, 1510009, 'jack.yang@student.unimelb.edu.au',     'Jack',    'Yang');

-- ============================================
-- Subject data
-- ============================================
INSERT INTO subject (id, name, description) VALUES
    (1, 'Mathematics',      'Advanced mathematics and calculus'),
    (2, 'Physics',          'Classical and modern physics'),
    (3, 'Chemistry',        'Organic and inorganic chemistry'),
    (4, 'Biology',          'Cell biology and genetics'),
    (5, 'Computer Science', 'Programming and algorithms');

-- ============================================
-- Template data
-- ============================================
INSERT INTO template (id, template_name, creator_id) VALUES
    (1, 'Presentation Assessment Template', 1),
    (2, 'Lab Report Template', 1),
    (3, 'Project Proposal Template', 2);

-- ============================================
-- Template element data
-- ============================================
INSERT INTO template_element (id, name, weighting, maximum_mark, mark_increments) VALUES
    (1, 'Voice, Pace and Confidence',   15, 100, 0.5),
    (2, 'Presentation Structure',       15, 100, 0.5),
    (3, 'Quality of Slides/Visual Aids', 15, 100, 0.5),
    (4, 'Knowledge of the Material',    20, 100, 0.5),
    (5, 'Content',                       20, 100, 0.5),
    (6, 'Concluding Remarks',           10, 100, 0.5),
    (7, 'Other Comments',                5, 100, 0.5);

-- ============================================
-- Comment library data
-- ============================================
INSERT INTO comment_library (template_element_id, content, comment_type) VALUES
    -- Voice, Pace and Confidence (element 1)
    (1, 'Excellent vocal clarity and confident delivery throughout.', 2),
    (1, 'Good pacing, allowing audience to absorb information.', 2),
    (1, 'Speaking too quickly, hard to follow at times.', 0),
    (1, 'Low voice volume, difficult to hear in the back.', 0),
    (1, 'Consider varying your pace for emphasis.', 1),
    -- Presentation Structure (element 2)
    (2, 'Well-organized with clear introduction and conclusion.', 2),
    (2, 'Logical flow between sections, easy to follow.', 2),
    (2, 'Lacks clear structure, transitions are abrupt.', 0),
    (2, 'Missing introduction or conclusion.', 0),
    (2, 'Consider adding transition slides between sections.', 1),
    -- Quality of Slides/Visual Aids (element 3)
    (3, 'Professional slides with clear, readable fonts.', 2),
    (3, 'Effective use of diagrams and visual elements.', 2),
    (3, 'Too much text on slides, overwhelming.', 0),
    (3, 'Poor color contrast, hard to read.', 0),
    (3, 'Consider using more visual aids to support points.', 1),
    -- Knowledge of the Material (element 4)
    (4, 'Demonstrates deep understanding of the topic.', 2),
    (4, 'Confidently answered all questions from audience.', 2),
    (4, 'Struggled with basic concepts, needs more preparation.', 0),
    (4, 'Unable to answer clarification questions.', 0),
    (4, 'Review core concepts before next presentation.', 1),
    -- Content (element 5)
    (5, 'Comprehensive coverage of all required topics.', 2),
    (5, 'Excellent use of examples and case studies.', 2),
    (5, 'Content is too shallow, lacks depth.', 0),
    (5, 'Missing key components of the assignment.', 0),
    (5, 'Consider adding more real-world examples.', 1),
    -- Concluding Remarks (element 6)
    (6, 'Strong conclusion that summarizes key points well.', 2),
    (6, 'Memorable closing statement.', 2),
    (6, 'Conclusion is rushed and incomplete.', 0),
    (6, 'No clear takeaway message.', 0),
    (6, 'Strengthen conclusion with call-to-action.', 1),
    -- Other Comments (element 7)
    (7, 'Overall excellent presentation.', 2),
    (7, 'Good effort, shows improvement from last time.', 2),
    (7, 'Needs significant improvement in multiple areas.', 0),
    (7, 'Practice more before next presentation.', 1);

-- ============================================
-- Assessment criteria data
-- ============================================
INSERT INTO assessment_criteria (id, template_id, template_element_id, template_element_name, weighting, maximum_mark, mark_increments) VALUES
    -- Template 1: Presentation Assessment Template
    (1, 1, 1, 'Voice, Pace and Confidence',   15, 100, 0.5),
    (2, 1, 2, 'Presentation Structure',        15, 100, 0.5),
    (3, 1, 3, 'Quality of Slides/Visual Aids', 15, 100, 0.5),
    (4, 1, 4, 'Knowledge of the Material',     20, 100, 0.5),
    (5, 1, 5, 'Content',                        20, 100, 0.5),
    (6, 1, 6, 'Concluding Remarks',            10, 100, 0.5),
    (7, 1, 7, 'Other Comments',                 5, 100, 0.5),
    -- Template 2: Lab Report Template
    (8,  2, 2, 'Presentation Structure',    25, 100, 1.0),
    (9,  2, 4, 'Knowledge of the Material', 30, 100, 1.0),
    (10, 2, 5, 'Content',                   35, 100, 1.0),
    (11, 2, 7, 'Other Comments',            10, 100, 1.0),
    -- Template 3: Project Proposal Template
    (12, 3, 2, 'Presentation Structure',    25, 100, 1.0),
    (13, 3, 4, 'Knowledge of the Material', 30, 100, 1.0),
    (14, 3, 5, 'Content',                   35, 100, 1.0),
    (15, 3, 6, 'Concluding Remarks',        10, 100, 1.0);

-- ============================================
-- Project data
-- ============================================
INSERT INTO project (id, project_name, countdown, subject_id, template_id, project_type) VALUES
    (1,  'Calculus Project',              1200000, 1, 1, 'individual'),
    (2,  'Linear Algebra Presentation',   1500000, 1, 1, 'individual'),
    (3,  'Physics Lab Experiment',         900000, 2, 2, 'individual'),
    (4,  'Quantum Mechanics Study',       1200000, 2, 1, 'individual'),
    (5,  'Chemistry Research',            1500000, 3, 3, 'individual'),
    (6,  'Organic Chemistry Synthesis',   1800000, 3, 2, 'individual'),
    (7,  'Biology Experiment',            1800000, 4, 1, 'individual'),
    (8,  'Genetics Research Project',     2000000, 4, 3, 'individual'),
    (9,  'Software Engineering Project',  1500000, 5, 1, 'individual'),
    (10, 'Machine Learning Application',  2000000, 5, 1, 'individual');

-- ============================================
-- Project group data
-- ============================================
INSERT INTO project_group (id, group_name, project_id) VALUES
    (1,  'Group A',       1),
    (2,  'Group B',       1),
    (3,  'Team Alpha',    2),
    (4,  'Team Beta',     2),
    (5,  'Lab Group 1',   3),
    (6,  'Lab Group 2',   3),
    (7,  'Lab Group 3',   3),
    (8,  'QM Team 1',     4),
    (9,  'QM Team 2',     4),
    (10, 'Chem Group A',  5),
    (11, 'Chem Group B',  5),
    (12, 'Synthesis Team', 6),
    (13, 'Bio Team 1',    7),
    (14, 'Bio Team 2',    7),
    (15, 'Genetics Group', 8),
    (16, 'SE Team Alpha', 9),
    (17, 'SE Team Beta',  9),
    (18, 'SE Team Gamma', 9),
    (19, 'ML Team 1',     10),
    (20, 'ML Team 2',     10);

-- ============================================
-- Group-student associations
-- ============================================
INSERT INTO group_student (student_id, group_id) VALUES
    -- Calculus Project groups (student PK: Alice=1, Bob=2, Charlie=3, Diana=4)
    (1, 1), (3, 1),
    (2, 2), (4, 2),
    -- Linear Algebra groups (Emma=5, Frank=6, Grace=7, Henry=8)
    (5, 3), (6, 3),
    (7, 4), (8, 4),
    -- Physics Lab groups
    (1, 5), (2, 6), (3, 7),
    -- QM groups
    (4, 8), (5, 9),
    -- Chemistry groups
    (6, 10), (7, 11), (8, 12),
    -- Biology groups
    (1, 13), (2, 14), (3, 15),
    -- Computer Science groups (Iris=9, Jack=10, Frank=6)
    -- Group 16 intentionally has two active members for group completion count tests.
    (9, 16), (6, 16), (10, 17), (6, 18),
    (7, 19), (8, 20);

-- ============================================
-- Student-project associations
-- ============================================
INSERT INTO student_project (student_id, subject_id, project_id) VALUES
    -- Mathematics projects (Alice=1, Bob=2, Charlie=3, Diana=4, Emma=5, Frank=6, Grace=7, Henry=8)
    (1, 1, 1), (3, 1, 1), (2, 1, 2), (4, 1, 2),
    (5, 1, 2), (6, 1, 1), (7, 1, 2), (8, 1, 1),
    -- Physics projects
    (1, 2, 3), (2, 2, 3), (3, 2, 3), (4, 2, 4), (5, 2, 4),
    -- Chemistry projects
    (6, 3, 5), (7, 3, 5), (8, 3, 6),
    -- Biology projects
    (1, 4, 7), (2, 4, 7), (3, 4, 8),
    -- Computer Science projects (Iris=9, Jack=10)
    (9, 5, 9), (10, 5, 9), (6, 5, 10), (7, 5, 10), (8, 5, 9);

-- ============================================
-- Student-Subject associations
-- ============================================
INSERT INTO student_subject (student_id, subject_id) VALUES
    -- Alice=1, Bob=2, Charlie=3, Diana=4, Emma=5, Frank=6, Grace=7, Henry=8, Iris=9, Jack=10
    (1, 1), (1, 2), (1, 4),
    (2, 1), (2, 2), (2, 4),
    (3, 1), (3, 2),
    (4, 1), (4, 2),
    (5, 1), (5, 2),
    (6, 1), (6, 3), (6, 5),
    (7, 1), (7, 3), (7, 5),
    (8, 1), (8, 3), (8, 5),
    (9, 5),
    (10, 5);

-- ============================================
-- User-project associations
-- ============================================
INSERT INTO user_project (user_id, subject_id, project_id) VALUES
    -- Admin oversees all projects
    (1, 1, 1), (1, 1, 2), (1, 2, 3), (1, 2, 4), (1, 3, 5),
    (1, 3, 6), (1, 4, 7), (1, 4, 8), (1, 5, 9), (1, 5, 10),
    -- Marker1: Mathematics and Physics
    (2, 1, 1), (2, 1, 2), (2, 2, 3), (2, 2, 4),
    -- Marker2: Chemistry and Biology
    (3, 3, 5), (3, 3, 6), (3, 4, 7), (3, 4, 8),
    -- Marker3: Computer Science
    (4, 5, 9), (4, 5, 10),
    -- Marker4: Mathematics and Computer Science
    (5, 1, 1), (5, 1, 2), (5, 5, 9),
    -- Marker5: Physics and Chemistry
    (6, 2, 3), (6, 2, 4), (6, 3, 5);

-- ============================================
-- User-Subject associations
-- ============================================
INSERT INTO user_subject (user_id, subject_id) VALUES
    -- Admin: all subjects
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
    -- Marker1: Mathematics, Physics
    (2, 1), (2, 2),
    -- Marker2: Chemistry, Biology
    (3, 3), (3, 4),
    -- Marker3: Computer Science
    (4, 5),
    -- Marker4: Mathematics, Computer Science
    (5, 1), (5, 5),
    -- Marker5: Physics, Chemistry
    (6, 2), (6, 3);

-- ============================================
-- Mark record data
-- ============================================
INSERT INTO mark_record (id, project_id, student_id, marker_id, total_score, group_score, mark_time) VALUES
    (1, 9, 9, 4, 76.75, 76.75, '2025-10-01 10:00:00'),
    (2, 9, 10, 4, NULL, NULL, NULL),
    -- Additional marker/group fixtures for incremental DAO tests:
    -- marker 5 has a completed individual mark for student 9 and a partial group score.
    (3, 9, 9, 5, 82.00, 70.00, '2025-10-01 11:00:00'),
    -- marker 4 has completed group scores for every active member of group 16.
    (4, 9, 6, 4, NULL, 76.75, '2025-10-01 10:05:00');

-- ============================================
-- Mark detail data
-- ============================================
INSERT INTO mark_detail (mark_record_id, criteria_id, score, comment, status) VALUES
    (1, 1, 80.0, 'Good vocal delivery', 1),
    (1, 2, 75.0, '', 1),
    (1, 3, 90.0, 'Clear slides', 1),
    (1, 4, 70.0, '', 1),
    (1, 5, 75.0, '', 1),
    (1, 6, 80.0, '', 1),
    (1, 7, 60.0, '', 1);

-- ============================================
-- Group mark record data
-- ============================================
INSERT INTO group_mark_record (id, project_id, group_id, marker_id, comment, mark_time) VALUES
    (1, 9, 16, 4, 'Good team effort from marker 3.', '2025-10-01 10:00:00'),
    (2, 9, 16, 5, 'Second marker comment for comparison.', '2025-10-01 11:00:00');

-- ============================================
-- Group mark detail data
-- ============================================
INSERT INTO group_mark_detail (group_mark_record_id, criteria_id, score, comment, status) VALUES
    (1, 1, 80.0, 'Good vocal delivery', 1),
    (1, 2, 75.0, '', 1),
    (1, 3, 90.0, 'Clear slides', 1),
    (1, 4, 70.0, '', 1),
    (1, 5, 75.0, '', 1),
    (1, 6, 80.0, '', 1),
    (1, 7, 60.0, '', 1);

-- ============================================
-- Marker-student assignments
-- ============================================
INSERT INTO marker_student (project_id, student_id, marker_id) VALUES
    -- Marker3(4) assigned students 9 and 10 in project 9
    (9, 9, 4),
    (9, 10, 4),
    -- Marker4(5) assigned student 9 in project 9
    (9, 9, 5);

-- ============================================
-- Marker-group assignments
-- ============================================
INSERT INTO marker_group (project_id, group_id, marker_id) VALUES
    -- Marker3(4) assigned group 16 (SE Team Alpha) in project 9
    (9, 16, 4),
    -- Marker4(5) assigned group 16 but only has partial member scores
    (9, 16, 5),
    -- Marker5(6) assigned group 17 but has no scores
    (9, 17, 6);

-- ============================================
-- Final mark data
-- ============================================
INSERT INTO final_mark (id, project_id, student_id, group_id, final_score, is_locked) VALUES
    (1, 9, 9, NULL, 80.00, 0),
    (2, 9, NULL, 16, 78.50, 1),
    (3, 9, 9, 16, 79.25, 0);
