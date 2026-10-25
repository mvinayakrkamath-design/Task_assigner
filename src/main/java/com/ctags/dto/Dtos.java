package com.ctags.dto;

import com.ctags.model.Task;
import com.ctags.model.User;

import java.time.LocalDateTime;

/**
 * Request and response bodies. JSON keys are snake_case
 * (spring.jackson.property-naming-strategy=SNAKE_CASE), and enum values
 * are sent as lowercase strings, e.g. "in_progress".
 */
public final class Dtos {

    private Dtos() {
    }

    // ---------- requests ----------

    public record LoginRequest(String email, String password) {
    }

    public record RegisterRequest(String name, String email, String password, String role,
                                  Long departmentId, Long managerId) {
    }

    public record TaskRequest(String title, String description, Long assignedTo,
                              String priority, Integer weightage, LocalDateTime deadline) {
    }

    public record StatusRequest(String status) {
    }

    // ---------- responses ----------

    public record UserDto(Long id, String name, String email, String role,
                          Long departmentId, Long managerId, boolean isActive) {

        public static UserDto of(User u) {
            return new UserDto(u.getId(), u.getName(), u.getEmail(),
                    u.getRole().name().toLowerCase(),
                    u.getDepartment() == null ? null : u.getDepartment().getId(),
                    u.getManager() == null ? null : u.getManager().getId(),
                    u.isActive());
        }
    }

    public record TaskDto(Long id, String title, String description, String priority,
                          int weightage, String status, LocalDateTime deadline,
                          Long assignedBy, String assignedByName,
                          Long assignedTo, String assignedToName,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {

        public static TaskDto of(Task t) {
            return new TaskDto(t.getId(), t.getTitle(), t.getDescription(),
                    t.getPriority().name().toLowerCase(), t.getWeightage(),
                    t.getStatus().name().toLowerCase(), t.getDeadline(),
                    t.getAssignedBy().getId(), t.getAssignedBy().getName(),
                    t.getAssignedTo().getId(), t.getAssignedTo().getName(),
                    t.getCreatedAt(), t.getUpdatedAt());
        }
    }
}
