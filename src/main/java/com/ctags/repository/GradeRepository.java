package com.ctags.repository;

import com.ctags.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findByTaskId(Long taskId);
    boolean existsByTaskId(Long taskId);
}
