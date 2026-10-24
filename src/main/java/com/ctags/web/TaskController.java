package com.ctags.web;

import com.ctags.dto.Dtos.StatusRequest;
import com.ctags.dto.Dtos.TaskRequest;
import com.ctags.model.User;
import com.ctags.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody TaskRequest req, @AuthenticationPrincipal User user) {
        return Map.of("task", taskService.create(req, user));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String status,
                                    @AuthenticationPrincipal User user) {
        return Map.of("tasks", taskService.list(user, status));
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return Map.of("task", taskService.get(id, user));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestBody StatusRequest req,
                                            @AuthenticationPrincipal User user) {
        return Map.of("task", taskService.updateStatus(id, req.status(), user));
    }
}
