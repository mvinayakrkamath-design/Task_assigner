package com.ctags.service;

import com.ctags.dto.Dtos.TaskDto;
import com.ctags.dto.Dtos.TaskRequest;
import com.ctags.model.Role;
import com.ctags.model.Task;
import com.ctags.model.TaskPriority;
import com.ctags.model.TaskStatus;
import com.ctags.model.User;
import com.ctags.repository.TaskRepository;
import com.ctags.repository.UserRepository;
import com.ctags.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final UserRepository users;
    private final AuditService audit;

    public TaskService(TaskRepository tasks, UserRepository users, AuditService audit) {
        this.tasks = tasks;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public TaskDto create(TaskRequest req, User current) {
        String title = req.title() == null ? "" : req.title().trim();
        if (title.isEmpty()) {
            throw ApiException.badRequest("Title is required");
        }
        TaskPriority priority;
        try {
            priority = TaskPriority.valueOf((req.priority() == null ? "medium" : req.priority()).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Priority must be one of low, medium, high, critical");
        }
        int weightage = req.weightage() == null ? 1 : req.weightage();
        if (weightage < 1 || weightage > 5) {
            throw ApiException.badRequest("Weightage must be a whole number from 1 to 5");
        }
        if (req.deadline() == null) {
            throw ApiException.badRequest("Deadline is required, e.g. 2026-11-15T17:00");
        }
        if (!req.deadline().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Deadline must be in the future");
        }

        User assignee = req.assignedTo() == null ? null : users.findById(req.assignedTo()).orElse(null);
        if (assignee == null || !assignee.isActive() || assignee.getRole() != Role.EMPLOYEE) {
            throw ApiException.badRequest("Assignee must be an active employee");
        }
        if (current.getRole() == Role.MANAGER
                && (assignee.getManager() == null || !Objects.equals(assignee.getManager().getId(), current.getId()))) {
            throw ApiException.forbidden("You can only assign tasks to your own team");
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(req.description() == null ? "" : req.description());
        task.setPriority(priority);
        task.setWeightage(weightage);
        task.setDeadline(req.deadline().withSecond(0).withNano(0));
        task.setAssignedBy(users.getReferenceById(current.getId()));
        task.setAssignedTo(assignee);
        task = tasks.save(task);
        audit.log(current.getId(), "assign", "task", task.getId(), "to user " + assignee.getId());
        return TaskDto.of(tasks.findById(task.getId()).orElseThrow());
    }

    /** Employees see their tasks, managers the tasks they assigned, admins all tasks. */
    @Transactional(readOnly = true)
    public List<TaskDto> list(User current, String status) {
        List<Task> result = switch (current.getRole()) {
            case EMPLOYEE -> tasks.findByAssignedToIdOrderByDeadline(current.getId());
            case MANAGER -> tasks.findByAssignedByIdOrderByDeadline(current.getId());
            case ADMIN -> tasks.findAllByOrderByDeadline();
        };
        return result.stream()
                .filter(t -> status == null || t.getStatus().name().equalsIgnoreCase(status))
                .map(TaskDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskDto get(Long id, User current) {
        Task task = tasks.findById(id).orElseThrow(() -> ApiException.notFound("Task not found"));
        boolean involved = Objects.equals(task.getAssignedTo().getId(), current.getId())
                || Objects.equals(task.getAssignedBy().getId(), current.getId());
        if (current.getRole() != Role.ADMIN && !involved) {
            throw ApiException.forbidden("Permission denied");
        }
        return TaskDto.of(task);
    }

    @Transactional
    public TaskDto updateStatus(Long id, String newStatus, User current) {
        Task task = tasks.findById(id).orElseThrow(() -> ApiException.notFound("Task not found"));
        if (!Objects.equals(task.getAssignedTo().getId(), current.getId())) {
            throw ApiException.forbidden("This task is not assigned to you");
        }
        TaskStatus next;
        try {
            next = TaskStatus.valueOf(String.valueOf(newStatus).toUpperCase());
        } catch (IllegalArgumentException e) {
            next = null;
        }
        TaskStatus old = task.getStatus();
        if (next == null || !old.employeeCanMoveTo(next)) {
            throw ApiException.badRequest("Cannot move task from '" + old.name().toLowerCase()
                    + "' to '" + newStatus + "'");
        }
        task.setStatus(next);
        tasks.saveAndFlush(task);
        audit.log(current.getId(), "status_change", "task", id,
                old.name().toLowerCase() + " -> " + next.name().toLowerCase());
        return TaskDto.of(task);
    }
}
