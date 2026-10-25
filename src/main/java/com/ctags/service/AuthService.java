package com.ctags.service;

import com.ctags.dto.Dtos.LoginRequest;
import com.ctags.dto.Dtos.RegisterRequest;
import com.ctags.dto.Dtos.UserDto;
import com.ctags.model.Role;
import com.ctags.model.User;
import com.ctags.repository.DepartmentRepository;
import com.ctags.repository.UserRepository;
import com.ctags.security.JwtService;
import com.ctags.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    public AuthService(UserRepository users, DepartmentRepository departments,
                       PasswordEncoder encoder, JwtService jwt, AuditService audit) {
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
    }

    @Transactional
    public Map<String, Object> login(LoginRequest req) {
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        User user = users.findByEmail(email)
                .filter(u -> req.password() != null && encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!user.isActive()) {
            throw ApiException.forbidden("Account disabled");
        }
        audit.log(user.getId(), "login", "user", user.getId(), null);
        return Map.of("token", jwt.createToken(user), "user", UserDto.of(user));
    }

    /** Admin creates accounts; there is no public sign-up in a corporate app. */
    @Transactional
    public UserDto register(RegisterRequest req, User admin) {
        String name = req.name() == null ? "" : req.name().trim();
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (name.isEmpty() || email.isEmpty() || req.password() == null || req.password().length() < 8) {
            throw ApiException.badRequest("Name, email and a password of 8+ characters are required");
        }
        Role role;
        try {
            role = Role.valueOf((req.role() == null ? "employee" : req.role()).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Role must be one of admin, manager, employee");
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User(name, email, encoder.encode(req.password()), role);
        if (req.departmentId() != null) {
            user.setDepartment(departments.findById(req.departmentId())
                    .orElseThrow(() -> ApiException.badRequest("Department not found")));
        }
        if (req.managerId() != null) {
            User manager = users.findById(req.managerId())
                    .filter(m -> m.getRole() == Role.MANAGER)
                    .orElseThrow(() -> ApiException.badRequest("Manager not found"));
            user.setManager(manager);
        }
        user = users.save(user);
        audit.log(admin.getId(), "create", "user", user.getId(), "role=" + role.name().toLowerCase());
        return UserDto.of(user);
    }
}
