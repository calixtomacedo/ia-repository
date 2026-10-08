package com.cmdev.atom.authentication.service;

import com.cmdev.atom.authentication.domain.dto.*;
import com.cmdev.atom.authentication.domain.model.RefreshToken;
import com.cmdev.atom.authentication.domain.model.User;
import com.cmdev.atom.authentication.exception.BadCredentialsException;
import com.cmdev.atom.authentication.exception.InvalidTokenException;
import com.cmdev.atom.authentication.exception.UserAlreadyExistsException;
import com.cmdev.atom.authentication.repository.LoginAuditDao;
import com.cmdev.atom.authentication.repository.RefreshTokenDao;
import com.cmdev.atom.authentication.repository.TokenBlacklistDao;
import com.cmdev.atom.authentication.repository.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserDao userDao;
    private final RefreshTokenDao refreshTokenDao;
    private final TokenBlacklistDao tokenBlacklistDao;
    private final LoginAuditDao loginAuditDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserDao userDao, RefreshTokenDao refreshTokenDao, TokenBlacklistDao tokenBlacklistDao, LoginAuditDao loginAuditDao, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userDao = userDao;
        this.refreshTokenDao = refreshTokenDao;
        this.tokenBlacklistDao = tokenBlacklistDao;
        this.loginAuditDao = loginAuditDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userDao.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("O email informado já está em uso");
        }

        Instant now = Instant.now();
        String encodedPassword = passwordEncoder.encode(request.password());
        String uuid = UUID.randomUUID().toString();

        Long newId = userDao.insert(uuid, request.name().trim(), normalizedEmail, encodedPassword, now, now);

        userDao.assignRole(newId, "ROLE_USER");

        User createdUser = new User(newId, uuid, request.name().trim(), normalizedEmail, encodedPassword, true, true, true, true, null, now, now, "ROLE_USER");

        return UserResponse.fromUser(createdUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Instant now = Instant.now();
        String ip = getClientIp();
        String userAgent = getClientUserAgent();

        Optional<User> userOpt = userDao.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            loginAuditDao.logLogin(null, normalizedEmail, ip, userAgent, false, now);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            loginAuditDao.logLogin(user.id(), normalizedEmail, ip, userAgent, false, now);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        if (!user.enabled() || !user.accountNonLocked()) {
            loginAuditDao.logLogin(user.id(), normalizedEmail, ip, userAgent, false, now);
            throw new BadCredentialsException("Conta desabilitada ou bloqueada");
        }

        // Login success: audit and update last_login
        loginAuditDao.logLogin(user.id(), normalizedEmail, ip, userAgent, true, now);
        userDao.updateLastLogin(user.id(), now);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Instant expiryDate = now.plusSeconds(jwtService.getRefreshTokenExpirationSeconds());

        refreshTokenDao.insert(user.id(), refreshToken, now, expiryDate, now);

        return AuthResponse.of(accessToken, refreshToken, jwtService.getAccessTokenExpirationSeconds());
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenStr = request.refreshToken();
        RefreshToken storedToken = refreshTokenDao.findByToken(tokenStr).orElseThrow(() -> new InvalidTokenException("Token de renovação inválido"));
        if (!storedToken.isValid()) {
            throw new InvalidTokenException("Token de renovação expirado ou revogado");
        }
        User user = userDao.findById(storedToken.userId()).orElseThrow(() -> new InvalidTokenException("Usuário associado ao token não encontrado"));
        Instant now = Instant.now();

        // Revoke the current refresh token
        refreshTokenDao.revokeByToken(tokenStr, now);

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        Instant expiryDate = now.plusSeconds(jwtService.getRefreshTokenExpirationSeconds());
        refreshTokenDao.insert(user.id(), newRefreshToken, now, expiryDate, now);
        return AuthResponse.of(newAccessToken, newRefreshToken, jwtService.getAccessTokenExpirationSeconds());
    }

    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return;
        }

        String token = authHeader.trim();
        while (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        if (token.isEmpty()) {
            return;
        }

        try {
            String tokenHash = jwtService.hashToken(token);
            String userEmail = jwtService.extractUsername(token);
            Instant now = Instant.now();

            Long userId = null;
            if (userEmail != null) {
                Optional<User> userOpt = userDao.findByEmail(userEmail);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    userId = user.id();
                    refreshTokenDao.revokeAllByUserId(user.id(), now);
                }
            }

            if (userId != null) {
                tokenBlacklistDao.insert(userId, now, tokenHash);
            }
        } catch (Exception e) {
            // Ignored if token parsing fails on logout
        }
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String xForwarded = req.getHeader("X-Forwarded-For");
            if (xForwarded != null && !xForwarded.isBlank()) {
                return xForwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        }
        return "127.0.0.1";
    }

    private String getClientUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String ua = req.getHeader("User-Agent");
            if (ua != null) {
                return ua.length() > 500 ? ua.substring(0, 500) : ua;
            }
        }
        return "Unknown";
    }
}
