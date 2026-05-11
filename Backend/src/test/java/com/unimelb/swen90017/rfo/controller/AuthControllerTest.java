package com.unimelb.swen90017.rfo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimelb.swen90017.rfo.common.BusinessException;
import com.unimelb.swen90017.rfo.pojo.constants.BaseConstants;
import com.unimelb.swen90017.rfo.pojo.vo.AuthResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.LoginRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.RegisterRequestVO;
import com.unimelb.swen90017.rfo.security.CustomUserDetailsService;
import com.unimelb.swen90017.rfo.security.JwtAuthenticationFilter;
import com.unimelb.swen90017.rfo.service.UserService;
import com.unimelb.swen90017.rfo.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController.
 *
 * Covers test cases AC-001 through AC-010, AC-014, AC-015 as defined in Test-Plan.md §5.2.
 * Uses @WebMvcTest to load only the Controller layer.
 * Security filters are disabled via @AutoConfigureMockMvc(addFilters = false).
 *
 * Security infrastructure beans (JwtAuthenticationFilter, JwtUtil, CustomUserDetailsService)
 * are mocked via @MockBean so that SecurityConfig can wire up without NoSuchBeanDefinitionException.
 *
 * Skipped (covered by integration tests):
 * - AC-004 (Malformed JSON) – returned 500 by current GlobalExceptionHandler
 * - AC-011, AC-012 (Wrong HTTP method) – returns 500 instead of 4xx
 * - AC-013 (Wrong Content-Type) – returns 500 instead of 4xx
 *
 * @see com.unimelb.swen90017.rfo.controller.AuthController
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Primary service under test
    @MockBean
    private UserService userService;

    // Security infrastructure beans required by SecurityConfig to load correctly
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // ==================== Test Fixtures ====================

    private LoginRequestVO loginRequest;
    private RegisterRequestVO registerRequest;
    private AuthResponseVO authResponse;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequestVO();
        loginRequest.setEmail("marker1@example.com");
        loginRequest.setPassword("marker123");

        registerRequest = new RegisterRequestVO();
        registerRequest.setUsername("newmarker");
        registerRequest.setPassword("newpassword");
        registerRequest.setEmail("newmarker@example.com");
        registerRequest.setRole(BaseConstants.USER_ROLE_MARKER);

        authResponse = AuthResponseVO.builder()
                .token("mock-jwt-token-abc123")
                .userId(2L)
                .username("marker1")
                .email("marker1@example.com")
                .role(BaseConstants.USER_ROLE_MARKER)
                .build();
    }

    // ==================== Login Tests ====================

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        /**
         * AC-001: Login success – POST /api/login returns 200 with JWT token.
         */
        @Test
        @DisplayName("AC-001: Successful login returns 200 with JWT token and user info")
        void login_withValidCredentials_returns200WithToken() throws Exception {
            // Arrange
            when(userService.login(any(LoginRequestVO.class))).thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"))
                    .andExpect(jsonPath("$.data.token").value("mock-jwt-token-abc123"))
                    .andExpect(jsonPath("$.data.userId").value(2))
                    .andExpect(jsonPath("$.data.username").value("marker1"))
                    .andExpect(jsonPath("$.data.email").value("marker1@example.com"))
                    .andExpect(jsonPath("$.data.role").value(BaseConstants.USER_ROLE_MARKER));

            verify(userService).login(any(LoginRequestVO.class));
        }

        /**
         * AC-002: Login failure – service throws exception, returns 500 with error message.
         */
        @Test
        @DisplayName("AC-002: Login failure returns 500 with error message")
        void login_withInvalidCredentials_returns500WithError() throws Exception {
            // Arrange
            when(userService.login(any(LoginRequestVO.class)))
                    .thenThrow(new BusinessException("Incorrect email or password"));

            loginRequest.setPassword("wrongpassword");

            // Act & Assert
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())             // Controller catches and wraps
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Incorrect email or password"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(userService).login(any(LoginRequestVO.class));
        }

        /**
         * AC-003: Login with missing fields – null email/password handled gracefully.
         * Service is still called; behavior depends on service layer validation.
         */
        @Test
        @DisplayName("AC-003: Login with missing fields is handled gracefully")
        void login_withMissingFields_handledGracefully() throws Exception {
            // Arrange – send request with null fields
            when(userService.login(any(LoginRequestVO.class)))
                    .thenThrow(new BusinessException("Incorrect email or password"));

            LoginRequestVO emptyRequest = new LoginRequestVO();
            // email = null, password = null

            // Act & Assert
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        /**
         * AC-005: Login with empty request body – empty JSON object is deserialized,
         * service is called with all-null fields.
         */
        @Test
        @DisplayName("AC-005: Login with empty JSON body handled by service")
        void login_withEmptyBody_handledByService() throws Exception {
            // Arrange
            when(userService.login(any(LoginRequestVO.class)))
                    .thenThrow(new BusinessException("Incorrect email or password"));

            // Act & Assert
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        /**
         * AC-014: Login response format verification – response contains all required fields.
         */
        @Test
        @DisplayName("AC-014: Login response contains all required fields (code, message, data, timestamp)")
        void login_responseFormat_containsAllRequiredFields() throws Exception {
            // Arrange
            when(userService.login(any(LoginRequestVO.class))).thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        /**
         * AC-002 supplement: Verify service is called with exactly the correct request fields.
         */
        @Test
        @DisplayName("AC-002 Supplement: Login passes correct email and password to service")
        void login_passesCorrectFieldsToService() throws Exception {
            // Arrange
            when(userService.login(any(LoginRequestVO.class))).thenReturn(authResponse);

            // Act
            mockMvc.perform(post("/api/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // Assert - service was called with matching fields
            verify(userService).login(argThat(req ->
                    "marker1@example.com".equals(req.getEmail())
                            && "marker123".equals(req.getPassword())
            ));
        }
    }

    // ==================== Registration Tests ====================

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        /**
         * AC-006: Registration success – POST /api/register returns 200.
         */
        @Test
        @DisplayName("AC-006: Successful registration returns 200")
        void register_withValidRequest_returns200() throws Exception {
            // Arrange
            doNothing().when(userService).register(any(RegisterRequestVO.class));

            // Act & Assert
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"));

            verify(userService).register(any(RegisterRequestVO.class));
        }

        /**
         * AC-007: Registration with duplicate email – service throws exception, returns 500.
         */
        @Test
        @DisplayName("AC-007: Registration with duplicate email returns 500 with error message")
        void register_withDuplicateEmail_returns500WithError() throws Exception {
            // Arrange
            doThrow(new BusinessException("Email address already exists."))
                    .when(userService).register(any(RegisterRequestVO.class));

            registerRequest.setEmail("marker1@example.com"); // already exists

            // Act & Assert
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Email address already exists."));
        }

        /**
         * AC-008: Registration with duplicate username – service throws exception, returns 500.
         */
        @Test
        @DisplayName("AC-008: Registration with duplicate username returns 500 with error message")
        void register_withDuplicateUsername_returns500WithError() throws Exception {
            // Arrange
            doThrow(new BusinessException("Username already exists"))
                    .when(userService).register(any(RegisterRequestVO.class));

            registerRequest.setUsername("marker1"); // already exists

            // Act & Assert
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Username already exists"));
        }

        /**
         * AC-009: Registration with missing required fields – null fields handled gracefully.
         */
        @Test
        @DisplayName("AC-009: Registration with missing fields is handled gracefully")
        void register_withMissingFields_handledGracefully() throws Exception {
            // Arrange – only username set, email/password null
            doThrow(new BusinessException("Email address already exists."))
                    .when(userService).register(any(RegisterRequestVO.class));

            RegisterRequestVO incompleteRequest = new RegisterRequestVO();
            incompleteRequest.setUsername("newuser");
            // email = null, password = null

            // Act & Assert
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(incompleteRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        /**
         * AC-010: Registration with default role – when role is null, service defaults to MARKER.
         * The Controller passes the request as-is; default role logic is in the service.
         */
        @Test
        @DisplayName("AC-010: Registration passes null role to service (service handles default)")
        void register_withNullRole_passesNullRoleToService() throws Exception {
            // Arrange
            doNothing().when(userService).register(any(RegisterRequestVO.class));
            registerRequest.setRole(null); // no role specified

            // Act
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // Assert – service received request with null role; default is applied in service
            verify(userService).register(argThat(req -> req.getRole() == null));
        }

        /**
         * AC-015: Registration response format verification – response contains code and message.
         */
        @Test
        @DisplayName("AC-015: Registration response contains code and message fields")
        void register_responseFormat_containsCodeAndMessage() throws Exception {
            // Arrange
            doNothing().when(userService).register(any(RegisterRequestVO.class));

            // Act & Assert
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        /**
         * AC-006 supplement: Verify registration passes all fields to service correctly.
         */
        @Test
        @DisplayName("AC-006 Supplement: Registration passes username, email, password, role to service")
        void register_passesAllFieldsToService() throws Exception {
            // Arrange
            doNothing().when(userService).register(any(RegisterRequestVO.class));

            // Act
            mockMvc.perform(post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)));

            // Assert
            verify(userService).register(argThat(req ->
                    "newmarker".equals(req.getUsername())
                            && "newpassword".equals(req.getPassword())
                            && "newmarker@example.com".equals(req.getEmail())
                            && BaseConstants.USER_ROLE_MARKER.equals(req.getRole())
            ));
        }

        /**
         * AC-006 supplement: Success response data field is absent (register returns no data).
         */
        @Test
        @DisplayName("AC-006 Supplement: Successful registration response has no data payload")
        void register_success_responseDataIsNull() throws Exception {
            // Arrange
            doNothing().when(userService).register(any(RegisterRequestVO.class));

            // Act & Assert
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
}
