package com.unimelb.swen90017.rfo.service;

import com.unimelb.swen90017.rfo.common.BusinessException;
import com.unimelb.swen90017.rfo.dao.UserDao;
import com.unimelb.swen90017.rfo.pojo.constants.BaseConstants;
import com.unimelb.swen90017.rfo.pojo.po.UserPO;
import com.unimelb.swen90017.rfo.pojo.vo.AuthResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.UserProfileVO;
import com.unimelb.swen90017.rfo.pojo.vo.UserResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.LoginRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.RegisterRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.UpdatePasswordRequestVO;
import com.unimelb.swen90017.rfo.security.CustomUserDetails;
import com.unimelb.swen90017.rfo.service.impl.UserServiceImpl;
import com.unimelb.swen90017.rfo.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl.
 *
 * Covers test cases US-001 through US-009 as defined in Test-Plan.md.
 * Uses Mockito to isolate from database and security infrastructure.
 *
 * @see com.unimelb.swen90017.rfo.service.impl.UserServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserServiceImpl userService;

    // ==================== Test Fixtures ====================

    private UserPO testUser;
    private LoginRequestVO loginRequest;
    private RegisterRequestVO registerRequest;

    @BeforeEach
    void setUp() {
        // Standard test user
        testUser = UserPO.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encodedPassword")
                .email("test@unimelb.edu.au")
                .role(BaseConstants.USER_ROLE_MARKER)
                .deleteStatus(BaseConstants.DELETE_STATUS_NOT_DELETED)
                .build();

        // Standard login request
        loginRequest = new LoginRequestVO();
        loginRequest.setEmail("test@unimelb.edu.au");
        loginRequest.setPassword("password123");

        // Standard register request
        registerRequest = new RegisterRequestVO();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("newpassword");
        registerRequest.setEmail("new@unimelb.edu.au");
        registerRequest.setRole(BaseConstants.USER_ROLE_MARKER);
    }

    // ==================== Login Tests ====================

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        /**
         * US-001: Successful login with valid email and password.
         * Verifies that a JWT token and user info are returned.
         */
        @Test
        @DisplayName("US-001: Successful login returns JWT token and user info")
        void login_withValidCredentials_returnsAuthResponse() {
            // Arrange
            CustomUserDetails userDetails = new CustomUserDetails(testUser);
            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(jwtUtil.generateToken(testUser.getEmail(), testUser.getId(), testUser.getRole()))
                    .thenReturn("mock-jwt-token");

            // Act
            AuthResponseVO response = userService.login(loginRequest);

            // Assert
            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals(testUser.getId(), response.getUserId());
            assertEquals(testUser.getUsername(), response.getUsername());
            assertEquals(testUser.getEmail(), response.getEmail());
            assertEquals(testUser.getRole(), response.getRole());

            // Verify interactions
            verify(authenticationManager).authenticate(
                    argThat(auth ->
                            auth.getPrincipal().equals(loginRequest.getEmail())
                                    && auth.getCredentials().equals(loginRequest.getPassword())
                    )
            );
            verify(jwtUtil).generateToken(testUser.getEmail(), testUser.getId(), testUser.getRole());
        }

        /**
         * US-002: Login fails when user email does not exist.
         * AuthenticationManager throws BadCredentialsException → wrapped as BusinessException.
         */
        @Test
        @DisplayName("US-002: Login with non-existent email throws BusinessException")
        void login_withNonExistentEmail_throwsBusinessException() {
            // Arrange
            loginRequest.setEmail("nonexistent@unimelb.edu.au");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.login(loginRequest));
            assertEquals("Incorrect email or password", exception.getMessage());

            // Verify JWT is never generated on failure
            verify(jwtUtil, never()).generateToken(anyString(), anyLong(), anyInt());
        }

        /**
         * US-003: Login fails with correct email but incorrect password.
         * Same mechanism as US-002 but explicitly tests wrong password scenario.
         */
        @Test
        @DisplayName("US-003: Login with incorrect password throws BusinessException")
        void login_withIncorrectPassword_throwsBusinessException() {
            // Arrange
            loginRequest.setPassword("wrongpassword");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.login(loginRequest));
            assertEquals("Incorrect email or password", exception.getMessage());

            verify(jwtUtil, never()).generateToken(anyString(), anyLong(), anyInt());
        }

        /**
         * US-004: Login fails when user has been soft-deleted (delete_status=1).
         * Spring Security's isEnabled() returns false, which triggers a DisabledException
         * or BadCredentialsException depending on the AuthenticationManager config.
         * Either way, it's caught as AuthenticationException and re-thrown as BusinessException.
         */
        @Test
        @DisplayName("US-004: Login with deleted user throws BusinessException")
        void login_withDeletedUser_throwsBusinessException() {
            // Arrange - AuthenticationManager rejects disabled users
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("User is disabled"));

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.login(loginRequest));
            assertEquals("Incorrect email or password", exception.getMessage());
        }

        /**
         * US-001 supplement: Verify login with ADMIN role user returns correct role in response.
         */
        @Test
        @DisplayName("US-001 Supplement: Admin login returns role=1")
        void login_withAdminUser_returnsAdminRole() {
            // Arrange
            UserPO adminUser = UserPO.builder()
                    .id(2L)
                    .username("admin")
                    .password("$2a$10$encodedPassword")
                    .email("admin@unimelb.edu.au")
                    .role(BaseConstants.USER_ROLE_ADMIN)
                    .deleteStatus(BaseConstants.DELETE_STATUS_NOT_DELETED)
                    .build();

            CustomUserDetails userDetails = new CustomUserDetails(adminUser);
            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(jwtUtil.generateToken(adminUser.getEmail(), adminUser.getId(), adminUser.getRole()))
                    .thenReturn("admin-jwt-token");

            loginRequest.setEmail("admin@unimelb.edu.au");

            // Act
            AuthResponseVO response = userService.login(loginRequest);

            // Assert
            assertEquals(BaseConstants.USER_ROLE_ADMIN, response.getRole());
            assertEquals("admin-jwt-token", response.getToken());
        }
    }

    // ==================== Registration Tests ====================

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        /**
         * US-005: Successful registration with valid user information.
         * Verifies user is created and inserted into database.
         */
        @Test
        @DisplayName("US-005: Successful registration creates user")
        void register_withValidInfo_createsUser() {
            // Arrange
            when(userDao.selectByUsernameOrEmail(registerRequest.getUsername(), registerRequest.getEmail()))
                    .thenReturn(null);  // No duplicate
            when(passwordEncoder.encode(registerRequest.getPassword()))
                    .thenReturn("$2a$10$encodedNewPassword");
            when(userDao.insert(any(UserPO.class))).thenReturn(1);

            // Act
            userService.register(registerRequest);

            // Assert - Capture the UserPO passed to insert
            ArgumentCaptor<UserPO> userCaptor = ArgumentCaptor.forClass(UserPO.class);
            verify(userDao).insert(userCaptor.capture());

            UserPO insertedUser = userCaptor.getValue();
            assertEquals("newuser", insertedUser.getUsername());
            assertEquals("$2a$10$encodedNewPassword", insertedUser.getPassword());
            assertEquals("new@unimelb.edu.au", insertedUser.getEmail());
            assertEquals(BaseConstants.USER_ROLE_MARKER, insertedUser.getRole());
            assertEquals(BaseConstants.DELETE_STATUS_NOT_DELETED, insertedUser.getDeleteStatus());
        }

        /**
         * US-006: Registration fails because email already exists.
         * checkDuplicate() finds a user with matching email and throws BusinessException.
         */
        @Test
        @DisplayName("US-006: Registration with duplicate email throws BusinessException")
        void register_withDuplicateEmail_throwsBusinessException() {
            // Arrange - existing user has the same email but different username
            UserPO existingUser = UserPO.builder()
                    .id(99L)
                    .username("differentuser")
                    .email("new@unimelb.edu.au")  // Same email as registerRequest
                    .build();
            when(userDao.selectByUsernameOrEmail(registerRequest.getUsername(), registerRequest.getEmail()))
                    .thenReturn(existingUser);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.register(registerRequest));
            assertEquals("Email address already exists.", exception.getMessage());

            // Verify insert is never called
            verify(userDao, never()).insert(any(UserPO.class));
        }

        /**
         * US-006 supplement: Registration fails because username already exists.
         * checkDuplicate() finds a user with matching username and throws BusinessException.
         */
        @Test
        @DisplayName("US-006 Supplement: Registration with duplicate username throws BusinessException")
        void register_withDuplicateUsername_throwsBusinessException() {
            // Arrange - existing user has the same username
            UserPO existingUser = UserPO.builder()
                    .id(99L)
                    .username("newuser")  // Same username as registerRequest
                    .email("other@unimelb.edu.au")
                    .build();
            when(userDao.selectByUsernameOrEmail(registerRequest.getUsername(), registerRequest.getEmail()))
                    .thenReturn(existingUser);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.register(registerRequest));
            assertEquals("Username already exists", exception.getMessage());

            verify(userDao, never()).insert(any(UserPO.class));
        }

        /**
         * US-007: Verify password is encrypted with BCrypt before storage.
         * The raw password must NOT appear in the persisted UserPO.
         */
        @Test
        @DisplayName("US-007: Password is BCrypt encrypted before storage")
        void register_passwordIsEncryptedBeforeStorage() {
            // Arrange
            String rawPassword = "plainTextPassword";
            String encodedPassword = "$2a$10$xyzEncodedPasswordHash";
            registerRequest.setPassword(rawPassword);

            when(userDao.selectByUsernameOrEmail(anyString(), anyString())).thenReturn(null);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
            when(userDao.insert(any(UserPO.class))).thenReturn(1);

            // Act
            userService.register(registerRequest);

            // Assert
            ArgumentCaptor<UserPO> userCaptor = ArgumentCaptor.forClass(UserPO.class);
            verify(userDao).insert(userCaptor.capture());

            UserPO insertedUser = userCaptor.getValue();
            assertEquals(encodedPassword, insertedUser.getPassword());
            assertNotEquals(rawPassword, insertedUser.getPassword());

            // Verify passwordEncoder.encode was called with the raw password
            verify(passwordEncoder).encode(rawPassword);
        }

        /**
         * US-005 supplement: Registration with no role specified defaults to MARKER (role=2).
         */
        @Test
        @DisplayName("US-005 Supplement: Default role is MARKER when role is null")
        void register_withNullRole_defaultsToMarker() {
            // Arrange
            registerRequest.setRole(null);
            when(userDao.selectByUsernameOrEmail(anyString(), anyString())).thenReturn(null);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
            when(userDao.insert(any(UserPO.class))).thenReturn(1);

            // Act
            userService.register(registerRequest);

            // Assert
            ArgumentCaptor<UserPO> userCaptor = ArgumentCaptor.forClass(UserPO.class);
            verify(userDao).insert(userCaptor.capture());
            assertEquals(BaseConstants.USER_ROLE_MARKER, userCaptor.getValue().getRole());
        }

        /**
         * US-005 supplement: Registration with ADMIN role sets role=1 correctly.
         */
        @Test
        @DisplayName("US-005 Supplement: Registration with ADMIN role sets role correctly")
        void register_withAdminRole_setsAdminRole() {
            // Arrange
            registerRequest.setRole(BaseConstants.USER_ROLE_ADMIN);
            when(userDao.selectByUsernameOrEmail(anyString(), anyString())).thenReturn(null);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
            when(userDao.insert(any(UserPO.class))).thenReturn(1);

            // Act
            userService.register(registerRequest);

            // Assert
            ArgumentCaptor<UserPO> userCaptor = ArgumentCaptor.forClass(UserPO.class);
            verify(userDao).insert(userCaptor.capture());
            assertEquals(BaseConstants.USER_ROLE_ADMIN, userCaptor.getValue().getRole());
        }
    }

    // ==================== GetAllMarkers Tests ====================

    @Nested
    @DisplayName("GetAllMarkers Tests")
    class GetAllMarkersTests {

        /**
         * US-008: Get all markers returns users with role=2.
         */
        @Test
        @DisplayName("US-008: Get all markers returns marker users")
        void getAllMarkers_returnsMarkerUsers() {
            // Arrange
            UserPO marker1 = UserPO.builder().id(1L).username("marker1").role(BaseConstants.USER_ROLE_MARKER).build();
            UserPO marker2 = UserPO.builder().id(2L).username("marker2").role(BaseConstants.USER_ROLE_MARKER).build();
            when(userDao.getAllMarkers()).thenReturn(Arrays.asList(marker1, marker2));

            // Act
            List<UserResponseVO> result = userService.getAllMarkers();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());

            assertEquals(1L, result.get(0).getUserId());
            assertEquals("marker1", result.get(0).getUserName());
            assertEquals(BaseConstants.USER_ROLE_MARKER, result.get(0).getRole());

            assertEquals(2L, result.get(1).getUserId());
            assertEquals("marker2", result.get(1).getUserName());
            assertEquals(BaseConstants.USER_ROLE_MARKER, result.get(1).getRole());

            verify(userDao).getAllMarkers();
        }

        /**
         * US-009: Deleted users (delete_status=1) are filtered out.
         * The filtering is done in the SQL query of UserDao.getAllMarkers(),
         * so we verify the service correctly passes through the DAO result.
         */
        @Test
        @DisplayName("US-009: Deleted users are filtered by DAO query")
        void getAllMarkers_deletedUsersFilteredByDao() {
            // Arrange - DAO already filters deleted users in SQL (WHERE delete_status = 0)
            UserPO activeMarker = UserPO.builder().id(1L).username("active").role(BaseConstants.USER_ROLE_MARKER).build();
            // Deleted markers are NOT returned by the DAO query
            when(userDao.getAllMarkers()).thenReturn(Collections.singletonList(activeMarker));

            // Act
            List<UserResponseVO> result = userService.getAllMarkers();

            // Assert - only 1 active marker returned
            assertEquals(1, result.size());
            assertEquals("active", result.get(0).getUserName());
        }

        /**
         * US-008 supplement: Get all markers when no markers exist returns empty list.
         */
        @Test
        @DisplayName("US-008 Supplement: No markers returns empty list")
        void getAllMarkers_noMarkers_returnsEmptyList() {
            // Arrange
            when(userDao.getAllMarkers()).thenReturn(Collections.emptyList());

            // Act
            List<UserResponseVO> result = userService.getAllMarkers();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        /**
         * US-008 supplement: Verify PO → VO field mapping correctness.
         */
        @Test
        @DisplayName("US-008 Supplement: PO to VO field mapping is correct")
        void getAllMarkers_fieldMappingIsCorrect() {
            // Arrange
            UserPO marker = UserPO.builder()
                    .id(42L)
                    .username("test_marker")
                    .role(BaseConstants.USER_ROLE_MARKER)
                    .build();
            when(userDao.getAllMarkers()).thenReturn(Collections.singletonList(marker));

            // Act
            List<UserResponseVO> result = userService.getAllMarkers();

            // Assert - verify each field maps correctly
            UserResponseVO vo = result.get(0);
            assertEquals(marker.getId(), vo.getUserId());           // id → userId
            assertEquals(marker.getUsername(), vo.getUserName());    // username → userName
            assertEquals(marker.getRole(), vo.getRole());           // role → role
        }
    }

    // ==================== UpdateProfile Tests ====================

    @Nested
    @DisplayName("UpdateProfile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("US-010: Should update username successfully")
        void updateProfile_success() {
            when(userDao.selectById(1L)).thenReturn(testUser);
            when(userDao.selectCount(any())).thenReturn(0L);
            when(userDao.updateById(any(UserPO.class))).thenReturn(1);

            UserProfileVO result = userService.updateProfile(1L, "newUsername");

            assertNotNull(result);
            assertEquals(1L, result.getUserId());
            assertEquals("newUsername", result.getUsername());
            verify(userDao).updateById(any(UserPO.class));
        }

        @Test
        @DisplayName("US-011: Should throw when username is empty")
        void updateProfile_emptyUsername_throws() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updateProfile(1L, ""));
            assertEquals("Username cannot be empty", ex.getMessage());
            verify(userDao, never()).updateById(any());
        }

        @Test
        @DisplayName("US-012: Should throw when user not found")
        void updateProfile_userNotFound_throws() {
            when(userDao.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updateProfile(999L, "name"));
            assertEquals("User not found", ex.getMessage());
        }

        @Test
        @DisplayName("US-013: Should throw when username already taken")
        void updateProfile_duplicateUsername_throws() {
            when(userDao.selectById(1L)).thenReturn(testUser);
            when(userDao.selectCount(any())).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updateProfile(1L, "takenName"));
            assertEquals("Username already exists", ex.getMessage());
            verify(userDao, never()).updateById(any());
        }
    }

    // ==================== UpdatePassword Tests ====================

    @Nested
    @DisplayName("UpdatePassword Tests")
    class UpdatePasswordTests {

        private UpdatePasswordRequestVO passwordRequest;

        @BeforeEach
        void setUp() {
            passwordRequest = new UpdatePasswordRequestVO();
            passwordRequest.setUserId(1L);
            passwordRequest.setOldPassword("oldPass");
            passwordRequest.setNewPassword("newPass");
        }

        @Test
        @DisplayName("US-014: Should update password successfully")
        void updatePassword_success() {
            when(userDao.selectById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches("oldPass", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.matches("newPass", testUser.getPassword())).thenReturn(false);
            when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$newEncodedPass");
            when(userDao.updateById(any(UserPO.class))).thenReturn(1);

            assertDoesNotThrow(() -> userService.updatePassword(passwordRequest));

            verify(userDao).updateById(any(UserPO.class));
            verify(passwordEncoder).encode("newPass");
        }

        @Test
        @DisplayName("US-015: Should throw when user not found")
        void updatePassword_userNotFound_throws() {
            when(userDao.selectById(1L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updatePassword(passwordRequest));
            assertEquals("User not found", ex.getMessage());
        }

        @Test
        @DisplayName("US-016: Should throw when old password incorrect")
        void updatePassword_wrongOldPassword_throws() {
            when(userDao.selectById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches("oldPass", testUser.getPassword())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updatePassword(passwordRequest));
            assertEquals("Incorrect old password", ex.getMessage());
            verify(userDao, never()).updateById(any());
        }

        @Test
        @DisplayName("US-017: Should throw when new password same as old")
        void updatePassword_sameasOld_throws() {
            when(userDao.selectById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches("oldPass", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.matches("newPass", testUser.getPassword())).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updatePassword(passwordRequest));
            assertEquals("New password cannot be the same as the old password", ex.getMessage());
            verify(userDao, never()).updateById(any());
        }

        @Test
        @DisplayName("US-018: Should throw when user is deleted")
        void updatePassword_deletedUser_throws() {
            UserPO deletedUser = UserPO.builder()
                    .id(1L)
                    .deleteStatus(BaseConstants.DELETE_STATUS_DELETED)
                    .build();
            when(userDao.selectById(1L)).thenReturn(deletedUser);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updatePassword(passwordRequest));
            assertEquals("User not found", ex.getMessage());
        }
    }
}
