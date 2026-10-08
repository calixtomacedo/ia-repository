package com.cmdev.atom.authentication.config;

import com.cmdev.atom.authentication.domain.model.User;
import com.cmdev.atom.authentication.repository.TokenBlacklistDao;
import com.cmdev.atom.authentication.repository.UserDao;
import com.cmdev.atom.authentication.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDao userDao;
    private final TokenBlacklistDao tokenBlacklistDao;

    public JwtAuthenticationFilter(JwtService jwtService, UserDao userDao, TokenBlacklistDao tokenBlacklistDao) {
        this.jwtService = jwtService;
        this.userDao = userDao;
        this.tokenBlacklistDao = tokenBlacklistDao;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.trim();

        while (jwt.regionMatches(true, 0, "Bearer ", 0, 7)) {
            jwt = jwt.substring(7).trim();
        }

        if (jwt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String tokenHash = jwtService.hashToken(jwt);
            if (tokenBlacklistDao.isBlacklisted(tokenHash)) {
                log.warn("Token JWT está revogado no logout_audit");
                filterChain.doFilter(request, response);
                return;
            }

            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<User> userOptional = userDao.findByEmail(userEmail);

                if (userOptional.isPresent() && jwtService.isTokenValid(jwt, userEmail)) {
                    User user = userOptional.get();

                    if (!user.enabled() || !user.accountNonLocked()) {
                        log.warn("Usuário '{}' está desabilitado ou bloqueado", userEmail);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    String roleName = user.role();
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }

                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuário '{}' autenticado com sucesso via JWT", userEmail);
                } else {
                    log.warn("Token JWT inválido ou usuário '{}' não encontrado", userEmail);
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao processar token JWT no request '{} {}': {}",
                    request.getMethod(), request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
