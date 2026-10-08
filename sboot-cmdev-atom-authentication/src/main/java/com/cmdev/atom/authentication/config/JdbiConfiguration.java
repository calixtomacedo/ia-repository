package com.cmdev.atom.authentication.config;

import com.cmdev.atom.authentication.domain.model.RefreshToken;
import com.cmdev.atom.authentication.domain.model.User;
import com.cmdev.atom.authentication.repository.LoginAuditDao;
import com.cmdev.atom.authentication.repository.LogoutAuditDao;
import com.cmdev.atom.authentication.repository.RefreshTokenDao;
import com.cmdev.atom.authentication.repository.TokenBlacklistDao;
import com.cmdev.atom.authentication.repository.UserDao;
import com.cmdev.atom.authentication.repository.mapper.RefreshTokenRowMapper;
import com.cmdev.atom.authentication.repository.mapper.UserRowMapper;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
public class JdbiConfiguration {

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        TransactionAwareDataSourceProxy proxy = new TransactionAwareDataSourceProxy(dataSource);
        Jdbi jdbi = Jdbi.create(proxy);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.registerRowMapper(User.class, new UserRowMapper());
        jdbi.registerRowMapper(RefreshToken.class, new RefreshTokenRowMapper());
        return jdbi;
    }

    @Bean
    public UserDao userDao(Jdbi jdbi) {
        return jdbi.onDemand(UserDao.class);
    }

    @Bean
    public RefreshTokenDao refreshTokenDao(Jdbi jdbi) {
        return jdbi.onDemand(RefreshTokenDao.class);
    }

    @Bean
    public TokenBlacklistDao tokenBlacklistDao(Jdbi jdbi) {
        return jdbi.onDemand(TokenBlacklistDao.class);
    }

    @Bean
    public LoginAuditDao loginAuditDao(Jdbi jdbi) {
        return jdbi.onDemand(LoginAuditDao.class);
    }

    @Bean
    public LogoutAuditDao logoutAuditDao(Jdbi jdbi) {
        return jdbi.onDemand(LogoutAuditDao.class);
    }
}
