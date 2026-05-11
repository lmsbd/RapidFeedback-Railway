package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.po.StudentPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * DAO tests for StudentDao.
 *
 * Key fixtures from test-data.sql:
 * - Student PK id=1 has business student_id=1510000 and name Alice Chen.
 * - Student PK id=10 has business student_id=1510009 and name Jack Yang.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("StudentDao Tests")
class StudentDaoTest {

    @Autowired
    private StudentDao studentDao;

    @Nested
    @DisplayName("Business student number lookup")
    class StudentNumberLookup {

        @Test
        @DisplayName("SD-001: findByStudentId returns an active student by business student number")
        void findByStudentId_existing_returnsStudent() {
            StudentPO student = studentDao.findByStudentId(1510000L);

            assertNotNull(student);
            assertEquals(1L, student.getId());
            assertEquals(1510000L, student.getStudentId());
            assertEquals("Alice", student.getFirstName());
            assertEquals("Chen", student.getSurname());
        }

        @Test
        @DisplayName("SD-002: findByStudentId returns null for missing student number")
        void findByStudentId_missing_returnsNull() {
            assertNull(studentDao.findByStudentId(9999999L));
        }

        @Test
        @DisplayName("SD-003: findByStudentId excludes soft-deleted students")
        void findByStudentId_excludesSoftDeletedStudent() {
            StudentPO student = studentDao.selectById(1L);
            assertNotNull(student);
            student.setDeleteStatus("1");
            assertEquals(1, studentDao.updateById(student));

            assertNull(studentDao.findByStudentId(1510000L));
        }
    }

    @Nested
    @DisplayName("Primary key lookup")
    class PrimaryKeyLookup {

        @Test
        @DisplayName("SD-004: findById returns an active student by primary key")
        void findById_existing_returnsStudent() {
            StudentPO student = studentDao.findById(10L);

            assertNotNull(student);
            assertEquals(10L, student.getId());
            assertEquals(1510009L, student.getStudentId());
            assertEquals("Jack", student.getFirstName());
            assertEquals("Yang", student.getSurname());
        }

        @Test
        @DisplayName("SD-005: findById treats primary key and business student number separately")
        void findById_usesPrimaryKeyNotBusinessStudentNumber() {
            assertNull(studentDao.findById(1510000L));
            assertNotNull(studentDao.findByStudentId(1510000L));
        }

        @Test
        @DisplayName("SD-006: findById excludes soft-deleted students")
        void findById_excludesSoftDeletedStudent() {
            StudentPO student = studentDao.selectById(10L);
            assertNotNull(student);
            student.setDeleteStatus("1");
            assertEquals(1, studentDao.updateById(student));

            assertNull(studentDao.findById(10L));
        }
    }

    @Nested
    @DisplayName("Persistence behavior")
    class PersistenceBehavior {

        @Test
        @DisplayName("SD-007: insert creates a retrievable student")
        void insert_createsStudent() {
            StudentPO student = StudentPO.builder()
                    .studentId(1510999L)
                    .email("new.student@student.unimelb.edu.au")
                    .firstName("New")
                    .surname("Student")
                    .deleteStatus("0")
                    .build();

            assertEquals(1, studentDao.insert(student));

            StudentPO saved = studentDao.findByStudentId(1510999L);
            assertNotNull(saved);
            assertEquals(student.getId(), saved.getId());
            assertEquals("New", saved.getFirstName());
            assertEquals("Student", saved.getSurname());
        }

        @Test
        @DisplayName("SD-008: updateById persists student profile fields")
        void updateById_updatesStudent() {
            StudentPO student = studentDao.selectById(1L);
            assertNotNull(student);

            student.setEmail("alice.updated@student.unimelb.edu.au");
            student.setFirstName("Alicia");
            assertEquals(1, studentDao.updateById(student));

            StudentPO updated = studentDao.findById(1L);
            assertNotNull(updated);
            assertEquals("alice.updated@student.unimelb.edu.au", updated.getEmail());
            assertEquals("Alicia", updated.getFirstName());
        }

        @Test
        @DisplayName("SD-009: deleteById physically removes a student")
        void deleteById_removesStudent() {
            assertEquals(1, studentDao.deleteById(10L));

            assertNull(studentDao.selectById(10L));
            assertNull(studentDao.findById(10L));
            assertNull(studentDao.findByStudentId(1510009L));
        }
    }
}
