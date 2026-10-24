package com.ctags.repository;

import com.ctags.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedToIdOrderByDeadline(Long employeeId);

    List<Task> findByAssignedByIdOrderByDeadline(Long managerId);

    List<Task> findAllByOrderByDeadline();
}
