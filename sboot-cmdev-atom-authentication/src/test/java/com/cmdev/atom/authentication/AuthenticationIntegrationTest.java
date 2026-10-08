package com.cmdev.atom.authentication;

import com.cmdev.atom.authentication.domain.dto.AuthResponse;
import com.cmdev.atom.authentication.domain.dto.LoginRequest;
import com.cmdev.atom.authentication.domain.dto.RefreshTokenRequest;
import com.cmdev.atom.authentication.domain.dto.RegisterRequest;
import com.cmdev.atom.authentication.domain.dto.UpdateUserRequest;
import com.cmdev.atom.authentication.domain.dto.UserResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Carlos Santos", "carlos@empresa.com", "Senha@123");

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Carlos Santos"))
                .andExpect(jsonPath("$.email").value("carlos@empresa.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        UserResponse user = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
        assertThat(user.id()).isNotNull();
    }

    @Test
    @Order(2)
    void shouldRejectRegisterWithDuplicateEmail() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Carlos Duplicado", "carlos@empresa.com", "Senha@123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(3)
    void shouldRejectRegisterWithInvalidData() throws Exception {
        RegisterRequest invalidReq = new RegisterRequest("", "invalid-email", "123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @Order(4)
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        LoginRequest loginReq = new LoginRequest("carlos@empresa.com", "Senha@123");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn();

        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        assertThat(auth.accessToken()).isNotBlank();
        assertThat(auth.refreshToken()).isNotBlank();
    }

    @Test
    @Order(5)
    void shouldRejectLoginWithWrongPassword() throws Exception {
        LoginRequest loginReq = new LoginRequest("carlos@empresa.com", "SenhaErrada");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @Order(6)
    void shouldRefreshTokenSuccessfully() throws Exception {
        // First login to get a refresh token
        LoginRequest loginReq = new LoginRequest("carlos@empresa.com", "Senha@123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginAuth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // Now refresh token
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(loginAuth.refreshToken());
        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @Order(7)
    void shouldRejectAccessToProtectedUsersEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    void shouldListUsersWithValidBearerToken() throws Exception {
        // Login as admin
        LoginRequest loginReq = new LoginRequest("admin@empresa.com", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        MvcResult usersResult = mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        List<UserResponse> users = objectMapper.readValue(
                usersResult.getResponse().getContentAsString(),
                new TypeReference<List<UserResponse>>() {}
        );

        assertThat(users).isNotEmpty();
        assertThat(users.stream().anyMatch(u -> "admin@empresa.com".equals(u.email()))).isTrue();
    }

    @Test
    @Order(9)
    void shouldFindUserById() throws Exception {
        LoginRequest loginReq = new LoginRequest("admin@empresa.com", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn();
        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // Fetch users to get an id
        MvcResult usersResult = mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andReturn();
        List<UserResponse> users = objectMapper.readValue(
                usersResult.getResponse().getContentAsString(),
                new TypeReference<List<UserResponse>>() {}
        );
        Long targetId = users.get(0).id();

        mockMvc.perform(get("/users/" + targetId)
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId));
    }

    @Test
    @Order(10)
    void shouldReturn404WhenUserNotFound() throws Exception {
        LoginRequest loginReq = new LoginRequest("admin@empresa.com", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn();
        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc.perform(get("/users/999999")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @Order(11)
    void shouldUpdateUser() throws Exception {
        // Register a temporary user to update
        RegisterRequest registerReq = new RegisterRequest("User Para Atualizar", "update.me@empresa.com", "Senha@123");
        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(regResult.getResponse().getContentAsString(), UserResponse.class);

        LoginRequest loginReq = new LoginRequest("admin@empresa.com", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn();
        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        UpdateUserRequest updateReq = new UpdateUserRequest("User Atualizado", "updated.user@empresa.com");

        mockMvc.perform(put("/users/" + created.id())
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User Atualizado"))
                .andExpect(jsonPath("$.email").value("updated.user@empresa.com"));
    }

    @Test
    @Order(12)
    void shouldDeleteUser() throws Exception {
        // Register a temporary user to delete
        RegisterRequest registerReq = new RegisterRequest("User Para Deletar", "delete.me@empresa.com", "Senha@123");
        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(regResult.getResponse().getContentAsString(), UserResponse.class);

        LoginRequest loginReq = new LoginRequest("admin@empresa.com", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn();
        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // Delete user
        mockMvc.perform(delete("/users/" + created.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isNoContent());

        // Verify user is gone
        mockMvc.perform(get("/users/" + created.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(13)
    void shouldLogoutAndBlacklistToken() throws Exception {
        LoginRequest loginReq = new LoginRequest("admin@empresa.com", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn();
        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // Perform logout
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout realizado com sucesso"));

        // Now using that blacklisted token should fail with 401
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isUnauthorized());
    }
}
