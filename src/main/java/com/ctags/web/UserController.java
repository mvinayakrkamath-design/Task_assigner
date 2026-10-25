package com.ctags.web;

import com.ctags.dto.Dtos.UserDto;
import com.ctags.model.Role;
import com.ctags.model.User;
import com.ctags.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** User listing (full user management comes in Phase 2). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    /** Admin sees everyone; a manager sees only their team. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    public Map<String, Object> list(@AuthenticationPrincipal User current) {
        List<User> result = current.getRole() == Role.ADMIN
                ? users.findAllByOrderByRoleAscNameAsc()
                : users.findByManagerIdOrderByName(current.getId());
        return Map.of("users", result.stream().map(UserDto::of).toList());
    }
}
