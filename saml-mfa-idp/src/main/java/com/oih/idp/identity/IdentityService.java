package com.oih.idp.identity;

import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.domain.User;
import com.oih.idp.identity.repo.RoleRepository;
import com.oih.idp.identity.repo.UserRepository;
import com.oih.idp.policy.Argon2PasswordHasher;
import com.oih.idp.policy.PasswordPolicy;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final Argon2PasswordHasher hasher;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHistoryService passwordHistory;

    public IdentityService(
            UserRepository users,
            RoleRepository roles,
            Argon2PasswordHasher hasher,
            PasswordPolicy passwordPolicy,
            PasswordHistoryService passwordHistory) {
        this.users = users;
        this.roles = roles;
        this.hasher = hasher;
        this.passwordPolicy = passwordPolicy;
        this.passwordHistory = passwordHistory;
    }

    @Transactional
    public User createUser(
            String username,
            String email,
            String firstName,
            String lastName,
            String password,
            Set<String> roleNames) {
        validateUnique(username, email);
        List<String> errors = passwordPolicy.validate(password);
        if (!errors.isEmpty()) {
            throw new IdentityException(String.join(" ", errors));
        }
        User user = User.create(username, email, firstName, lastName, hasher.hash(password));
        for (String roleName : roleNames) {
            Role role = roles.findByName(roleName)
                    .orElseThrow(() -> new IdentityException("Unknown role: " + roleName));
            user.getRoles().add(role);
        }
        if (user.getRoles().isEmpty()) {
            user.getRoles().add(requireRole(Role.USER));
        }
        User saved = users.save(user);
        passwordHistory.record(saved, saved.getPasswordHash());
        return saved;
    }

    @Transactional
    public User updateProfile(UUID userId, String email, String firstName, String lastName) {
        User user = requireUser(userId);
        if (!User.normalize(email).equals(user.getEmail().toLowerCase(Locale.ROOT))) {
            if (users.existsByEmailNormalized(User.normalize(email))) {
                throw new IdentityException("Email is already in use.");
            }
        }
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return users.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = requireUser(userId);
        if (!hasher.matches(currentPassword, user.getPasswordHash())) {
            throw new IdentityException("Current password is incorrect.");
        }
        applyNewPassword(user, newPassword);
    }

    @Transactional
    public void adminSetPassword(UUID userId, String newPassword) {
        User user = requireUser(userId);
        applyNewPassword(user, newPassword);
    }

    @Transactional
    public void disable(UUID userId) {
        User user = requireUser(userId);
        user.setEnabled(false);
        users.save(user);
    }

    @Transactional
    public void enable(UUID userId) {
        User user = requireUser(userId);
        user.setEnabled(true);
        users.save(user);
    }

    @Transactional
    public void unlock(UUID userId) {
        User user = requireUser(userId);
        user.unlock();
        users.save(user);
    }

    @Transactional
    public void assignRoles(UUID userId, Set<String> roleNames) {
        User user = requireUser(userId);
        user.getRoles().clear();
        for (String roleName : roleNames) {
            user.getRoles().add(requireRole(roleName));
        }
        if (user.getRoles().isEmpty()) {
            user.getRoles().add(requireRole(Role.USER));
        }
        users.save(user);
    }

    public User requireUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new IdentityException("User not found."));
    }

    public List<User> listUsers() {
        return users.findAll();
    }

    public long userCount() {
        return users.count();
    }

    private void applyNewPassword(User user, String newPassword) {
        List<String> errors = passwordPolicy.validate(newPassword);
        if (!errors.isEmpty()) {
            throw new IdentityException(String.join(" ", errors));
        }
        if (passwordHistory.reused(user, newPassword)) {
            throw new IdentityException("Password was used recently.");
        }
        String hash = hasher.hash(newPassword);
        user.replacePasswordHash(hash);
        users.save(user);
        passwordHistory.record(user, hash);
    }

    private void validateUnique(String username, String email) {
        if (users.existsByUsernameNormalized(User.normalize(username))) {
            throw new IdentityException("Username is already in use.");
        }
        if (users.existsByEmailNormalized(User.normalize(email))) {
            throw new IdentityException("Email is already in use.");
        }
    }

    private Role requireRole(String name) {
        return roles.findByName(name).orElseThrow(() -> new IdentityException("Unknown role: " + name));
    }
}
