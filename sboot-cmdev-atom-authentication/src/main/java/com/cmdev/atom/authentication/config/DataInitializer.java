package com.cmdev.atom.authentication.config;

import com.cmdev.atom.authentication.domain.RolesEnum;
import com.cmdev.atom.authentication.repository.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@cmdev.com.br";
        try {
            if (!userDao.existsByEmail(adminEmail)) {
                Instant now = Instant.now();
                String encodedPassword = passwordEncoder.encode("Admin@123");
                Long adminId = userDao.insert(
                        UUID.randomUUID().toString(),
                        "CMDEV - Administrador",
                        adminEmail,
                        encodedPassword,
                        now,
                        now
                );
                userDao.assignRole(adminId, RolesEnum.ROLE_ADMIN.name());
                log.info("Usuário inicial criado com sucesso. ID: {}, Email: {}", adminId, adminEmail);
            }
        } catch (Exception e) {
            log.info("DataInitializer: tabela users ou admin já inicializados ({})", e.getMessage());
        }
    }
}
