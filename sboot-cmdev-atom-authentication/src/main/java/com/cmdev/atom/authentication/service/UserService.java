package com.cmdev.atom.authentication.service;

import com.cmdev.atom.authentication.domain.dto.UpdateUserRequest;
import com.cmdev.atom.authentication.domain.dto.UserResponse;
import com.cmdev.atom.authentication.domain.model.User;
import com.cmdev.atom.authentication.exception.ResourceNotFoundException;
import com.cmdev.atom.authentication.exception.UserAlreadyExistsException;
import com.cmdev.atom.authentication.repository.UserDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userDao.findAll().stream().map(UserResponse::fromUser).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return UserResponse.fromUser(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User existingUser = userDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        String newName = (request.name() != null && !request.name().isBlank()) ? request.name().trim() : existingUser.name();
        String newEmail = (request.email() != null && !request.email().isBlank()) ? request.email().trim().toLowerCase() : existingUser.email();

        if (!newEmail.equalsIgnoreCase(existingUser.email()) && userDao.existsByEmailAndIdNot(newEmail, id)) {
            throw new UserAlreadyExistsException("O email informado já está em uso por outro usuário");
        }

        Instant now = Instant.now();
        userDao.update(id, newName, newEmail, now);

        User updatedUser = new User(
                id,
                existingUser.uuid(),
                newName,
                newEmail,
                existingUser.passwordHash(),
                existingUser.enabled(),
                existingUser.accountNonExpired(),
                existingUser.accountNonLocked(),
                existingUser.credentialsNonExpired(),
                existingUser.lastLogin(),
                existingUser.createdAt(),
                now,
                existingUser.role()
        );

        return UserResponse.fromUser(updatedUser);
    }

    @Transactional
    public void delete(Long id) {
        User user = userDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        userDao.deleteById(user.id());
    }
}
