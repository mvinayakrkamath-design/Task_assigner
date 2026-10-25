package com.ctags.repository;

import com.ctags.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByManagerIdOrderByName(Long managerId);

    List<User> findAllByOrderByRoleAscNameAsc();
}
