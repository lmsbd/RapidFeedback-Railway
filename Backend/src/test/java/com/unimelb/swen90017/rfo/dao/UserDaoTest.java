package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.po.UserPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DAO tests for UserDao.
 *
 * Key fixtures from test-data.sql:
 * - User 1 is admin; users 2-6 are markers.
 * - Admin 1 is linked to subjects 1-5 via user_subject.
 * - Soft-delete behavior is tested per transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserDao Tests")
class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static List<Long> userIds(List<UserPO> users) {
        return users.stream().map(UserPO::getId).sorted().toList();
    }

    @Nested
    @DisplayName("Marker and admin list queries")
    class UserListQueries {

        @Test
        @DisplayName("UD-001: getAllMarkers returns active marker users")
        void getAllMarkers_returnsActiveMarkers() {
            List<UserPO> markers = userDao.getAllMarkers();

            assertEquals(List.of(2L, 3L, 4L, 5L, 6L), userIds(markers));
            markers.forEach(marker -> {
                assertEquals(2, marker.getRole());
                assertNull(marker.getEmail());
            });
        }

        @Test
        @DisplayName("UD-002: getAllMarkers excludes soft-deleted markers and admins")
        void getAllMarkers_excludesSoftDeletedAndAdmins() {
            UserPO marker = userDao.selectById(6L);
            assertNotNull(marker);
            marker.setDeleteStatus(1);
            assertEquals(1, userDao.updateById(marker));

            List<UserPO> markers = userDao.getAllMarkers();

            assertEquals(List.of(2L, 3L, 4L, 5L), userIds(markers));
            assertTrue(markers.stream().noneMatch(user -> user.getRole() == 1));
        }

        @Test
        @DisplayName("UD-003: getAllAdmins returns active admins with email")
        void getAllAdmins_returnsActiveAdmins() {
            List<UserPO> admins = userDao.getAllAdmins();

            assertEquals(List.of(1L), userIds(admins));
            assertEquals("admin", admins.get(0).getUsername());
            assertEquals("admin@example.com", admins.get(0).getEmail());
            assertEquals(1, admins.get(0).getRole());
        }

        @Test
        @DisplayName("UD-004: getAllAdmins excludes soft-deleted admins")
        void getAllAdmins_excludesSoftDeletedAdmins() {
            UserPO admin = userDao.selectById(1L);
            assertNotNull(admin);
            admin.setDeleteStatus(1);
            assertEquals(1, userDao.updateById(admin));

            assertEquals(List.of(), userDao.getAllAdmins());
        }

        @Test
        @DisplayName("UD-005: getAdminsBySubjectId joins through user_subject")
        void getAdminsBySubjectId_returnsSubjectAdmins() {
            List<UserPO> admins = userDao.getAdminsBySubjectId(5L);

            assertEquals(List.of(1L), userIds(admins));
            assertEquals("admin@example.com", admins.get(0).getEmail());
        }

        @Test
        @DisplayName("UD-006: getAdminsBySubjectId returns distinct active admins")
        void getAdminsBySubjectId_returnsDistinctActiveAdmins() {
            UserPO secondAdmin = UserPO.builder()
                    .username("admin2")
                    .password("encoded-password")
                    .email("admin2@example.com")
                    .role(1)
                    .deleteStatus(0)
                    .build();
            assertEquals(1, userDao.insert(secondAdmin));
            jdbcTemplate.update("INSERT INTO user_subject (user_id, subject_id) VALUES (?, ?)", secondAdmin.getId(), 5L);
            jdbcTemplate.update("INSERT INTO user_subject (user_id, subject_id) VALUES (?, ?)", secondAdmin.getId(), 5L);

            List<UserPO> admins = userDao.getAdminsBySubjectId(5L);

            assertEquals(List.of(1L, secondAdmin.getId()).stream().sorted().toList(), userIds(admins));
        }

        @Test
        @DisplayName("UD-007: getAdminsBySubjectId excludes soft-deleted linked admins")
        void getAdminsBySubjectId_excludesSoftDeletedAdmins() {
            UserPO admin = userDao.selectById(1L);
            assertNotNull(admin);
            admin.setDeleteStatus(1);
            assertEquals(1, userDao.updateById(admin));

            assertEquals(List.of(), userDao.getAdminsBySubjectId(5L));
        }
    }

    @Nested
    @DisplayName("Username and email lookup")
    class UsernameAndEmailLookup {

        @Test
        @DisplayName("UD-008: selectByUsernameOrEmail finds by username")
        void selectByUsernameOrEmail_findsByUsername() {
            UserPO user = userDao.selectByUsernameOrEmail("admin", null);

            assertNotNull(user);
            assertEquals(1L, user.getId());
            assertEquals("admin@example.com", user.getEmail());
        }

        @Test
        @DisplayName("UD-009: selectByUsernameOrEmail finds by email when username does not match")
        void selectByUsernameOrEmail_findsByEmail() {
            UserPO user = userDao.selectByUsernameOrEmail("missing-user", "marker1@example.com");

            assertNotNull(user);
            assertEquals(2L, user.getId());
            assertEquals("marker1", user.getUsername());
        }

        @Test
        @DisplayName("UD-010: selectByUsernameOrEmail ignores blank email")
        void selectByUsernameOrEmail_ignoresBlankEmail() {
            assertNull(userDao.selectByUsernameOrEmail("missing-user", ""));
        }

        @Test
        @DisplayName("UD-011: selectByUsernameOrEmail returns null for missing credentials")
        void selectByUsernameOrEmail_missing_returnsNull() {
            assertNull(userDao.selectByUsernameOrEmail("missing-user", "nobody@example.com"));
        }
    }

    @Nested
    @DisplayName("Persistence behavior")
    class PersistenceBehavior {

        @Test
        @DisplayName("UD-012: insert creates a retrievable user")
        void insert_createsUser() {
            UserPO user = UserPO.builder()
                    .username("new-marker")
                    .password("encoded-password")
                    .email("new.marker@example.com")
                    .role(2)
                    .deleteStatus(0)
                    .build();

            assertEquals(1, userDao.insert(user));

            UserPO saved = userDao.selectById(user.getId());
            assertNotNull(saved);
            assertEquals("new-marker", saved.getUsername());
            assertEquals("new.marker@example.com", saved.getEmail());
            assertEquals(2, saved.getRole());
        }

        @Test
        @DisplayName("UD-013: updateById persists profile fields")
        void updateById_updatesUser() {
            UserPO user = userDao.selectById(2L);
            assertNotNull(user);

            user.setUsername("renamed-marker");
            user.setAvatar("/avatars/2.png");
            assertEquals(1, userDao.updateById(user));

            UserPO updated = userDao.selectById(2L);
            assertNotNull(updated);
            assertEquals("renamed-marker", updated.getUsername());
            assertEquals("/avatars/2.png", updated.getAvatar());
        }

        @Test
        @DisplayName("UD-014: deleteById physically removes a user")
        void deleteById_removesUser() {
            assertEquals(1, userDao.deleteById(6L));

            assertNull(userDao.selectById(6L));
            assertEquals(List.of(2L, 3L, 4L, 5L), userIds(userDao.getAllMarkers()));
        }
    }
}
