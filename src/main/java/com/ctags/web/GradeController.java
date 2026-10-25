package com.ctags.web;

import com.ctags.dto.GradeDtos.GradeRequest;
import com.ctags.model.User;
import com.ctags.service.GradeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks/{taskId}/grade")
public class GradeController {
    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> grade(@PathVariable Long taskId,
                                     @RequestBody GradeRequest request,
                                     @AuthenticationPrincipal User user) {
        return Map.of("grade", gradeService.gradeTask(taskId, request, user));
    }

    @GetMapping
    public Map<String, Object> getGrade(@PathVariable Long taskId,
                                        @AuthenticationPrincipal User user) {
        return Map.of("grade", gradeService.getTaskGrade(taskId, user));
    }
}
