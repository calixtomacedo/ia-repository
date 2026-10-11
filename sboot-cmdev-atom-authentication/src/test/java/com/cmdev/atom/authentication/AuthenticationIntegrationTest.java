package com.cmdev.atom.authentication;

import com.cmdev.atom.authentication.domain.dto.AuthResponse;
import com.cmdev.atom.authentication.domain.dto.LoginRequest;
import com.cmdev.atom.authentication.domain.dto.RefreshTokenRequest;
import com.cmdev.atom.authentication.domain.dto.RegisterRequest;
import com.cmdev.atom.authentication.domain.dto.UpdateUserRequest;
import com.cmdev.atom.authentication.domain.dto.UserResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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
@DisplayName("Testes de Integração - Autenticação e Usuários")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String obtainAdminAccessToken() throws Exception {
        LoginRequest loginReq = new LoginRequest("admin@cmdev.com.br", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        return auth.accessToken();
    }

    @Test
    @Order(1)
    @DisplayName("Deve registrar um novo usuário com sucesso quando os dados forem válidos")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        String adminToken = obtainAdminAccessToken();
        RegisterRequest registerReq = new RegisterRequest("Carlos Santos", "carlos@empresa.com", "Senha@123", false);

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
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
    @DisplayName("Deve rejeitar o cadastro de usuário com email duplicado")
    void shouldRejectRegisterWithDuplicateEmail() throws Exception {
        String adminToken = obtainAdminAccessToken();
        RegisterRequest registerReq = new RegisterRequest("Carlos Duplicado", "carlos@empresa.com", "Senha@123", false);

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(3)
    @DisplayName("Deve rejeitar o cadastro de usuário com dados inválidos")
    void shouldRejectRegisterWithInvalidData() throws Exception {
        String adminToken = obtainAdminAccessToken();
        RegisterRequest invalidReq = new RegisterRequest("", "invalid-email", "123", false);

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("Deve realizar login com sucesso com credenciais válidas e retornar tokens JWT")
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
    @DisplayName("Deve rejeitar o login quando a senha estiver incorreta")
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
    @DisplayName("Deve renovar o token JWT com sucesso utilizando um refresh token válido")
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
    @DisplayName("Deve rejeitar acesso ao endpoint protegido /users sem token de autenticação")
    void shouldRejectAccessToProtectedUsersEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("Deve listar todos os usuários com sucesso quando autenticado com token Bearer válido")
    void shouldListUsersWithValidBearerToken() throws Exception {
        String adminToken = obtainAdminAccessToken();

        MvcResult usersResult = mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        List<UserResponse> users = objectMapper.readValue(
                usersResult.getResponse().getContentAsString(),
                new TypeReference<List<UserResponse>>() {
                }
        );

        assertThat(users).isNotEmpty();
        assertThat(users.stream().anyMatch(u -> "admin@cmdev.com.br".equals(u.email()))).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("Deve buscar e retornar um usuário por ID com sucesso")
    void shouldFindUserById() throws Exception {
        String adminToken = obtainAdminAccessToken();

        // Fetch users to get an id
        MvcResult usersResult = mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        List<UserResponse> users = objectMapper.readValue(
                usersResult.getResponse().getContentAsString(),
                new TypeReference<List<UserResponse>>() {
                }
        );
        Long targetId = users.get(0).id();

        mockMvc.perform(get("/users/" + targetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId));
    }

    @Test
    @Order(10)
    @DisplayName("Deve retornar status 404 quando o usuário não for encontrado pelo ID")
    void shouldReturn404WhenUserNotFound() throws Exception {
        String adminToken = obtainAdminAccessToken();

        mockMvc.perform(get("/users/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @Order(11)
    @DisplayName("Deve atualizar os dados do usuário com sucesso")
    void shouldUpdateUser() throws Exception {
        String adminToken = obtainAdminAccessToken();

        // Register a temporary user to update
        RegisterRequest registerReq = new RegisterRequest("User Para Atualizar", "update.me@empresa.com", "Senha@123", false);
        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(regResult.getResponse().getContentAsString(), UserResponse.class);

        UpdateUserRequest updateReq = new UpdateUserRequest("User Atualizado", "updated.user@empresa.com");

        mockMvc.perform(put("/users/" + created.id())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User Atualizado"))
                .andExpect(jsonPath("$.email").value("updated.user@empresa.com"));
    }

    @Test
    @Order(12)
    @DisplayName("Deve excluir um usuário por ID e confirmar sua remoção")
    void shouldDeleteUser() throws Exception {
        String adminToken = obtainAdminAccessToken();

        // Register a temporary user to delete
        RegisterRequest registerReq = new RegisterRequest("User Para Deletar", "delete.me@empresa.com", "Senha@123", false);
        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(regResult.getResponse().getContentAsString(), UserResponse.class);

        // Delete user
        mockMvc.perform(delete("/users/" + created.id())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify user is gone
        mockMvc.perform(get("/users/" + created.id())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(13)
    @DisplayName("Deve realizar logout com sucesso e invalidar o token de acesso na blacklist")
    void shouldLogoutAndBlacklistToken() throws Exception {
        String adminToken = obtainAdminAccessToken();

        // Perform logout
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout realizado com sucesso"));

        // Now using that blacklisted token should fail with 401
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
    }
}