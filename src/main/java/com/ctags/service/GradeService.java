package com.ctags.service;

import com.ctags.dto.GradeDtos.GradeDto;
import com.ctags.dto.GradeDtos.GradeRequest;
import com.ctags.model.Grade;
import com.ctags.model.Role;
import com.ctags.model.Task;
import com.ctags.model.TaskStatus;
import com.ctags.model.User;
import com.ctags.repository.GradeRepository;
import com.ctags.repository.TaskRepository;
import com.ctags.repository.UserRepository;
import com.ctags.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class GradeService {
    private final GradeRepository grades;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final AuditService audit;

    public GradeService(GradeRepository grades, TaskRepository tasks,
                        UserRepository users, AuditService audit) {
        this.grades = grades;
        this.tasks = tasks;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public GradeDto gradeTask(Long taskId, GradeRequest request, User current) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> ApiException.notFound("Task not found"));

        if (current.getRole() != Role.ADMIN
                && !Objects.equals(task.getAssignedBy().getId(), current.getId())) {
            throw ApiException.forbidden("You can only grade tasks assigned by you");
        }

        if (task.getStatus() != TaskStatus.SUBMITTED
                && task.getStatus() != TaskStatus.UNDER_REVIEW) {
            throw ApiException.badRequest("Only submitted tasks can be graded");
        }
        if (grades.existsByTaskId(taskId)) {
            throw ApiException.badRequest("This task has already been graded");
        }
        if (request == null) {
            throw ApiException.badRequest("Grade details are required");
        }

        validateScore("Quality", request.quality());
        validateScore("Timeliness", request.timeliness());
        validateScore("Completeness", request.completeness());
        validateScore("Communication", request.communication());

        Grade grade = new Grade();
        grade.setTask(task);
        grade.setGradedBy(users.getReferenceById(current.getId()));
        grade.setQuality(request.quality());
        grade.setTimeliness(request.timeliness());
        grade.setCompleteness(request.completeness());
        grade.setCommunication(request.communication());
        grade.setComments(request.comments() == null ? "" : request.comments().trim());
        grade.setFinalScore(Grade.computeFinalScore(request.quality(), request.timeliness(),
                request.completeness(), request.communication()));

        Grade saved = grades.save(grade);
        task.setStatus(TaskStatus.GRADED);
        tasks.save(task);
        audit.log(current.getId(), "grade", "task", taskId,
                "Final score: " + saved.getFinalScore());
        return GradeDto.of(saved);
    }

    @Transactional(readOnly = true)
    public GradeDto getTaskGrade(Long taskId, User current) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> ApiException.notFound("Task not found"));
        boolean involved = Objects.equals(task.getAssignedBy().getId(), current.getId())
                || Objects.equals(task.getAssignedTo().getId(), current.getId());
        if (current.getRole() != Role.ADMIN && !involved) {
            throw ApiException.forbidden("Permission denied");
        }
        Grade grade = grades.findByTaskId(taskId)
                .orElseThrow(() -> ApiException.notFound("This task has not been graded yet"));
        return GradeDto.of(grade);
    }

    private void validateScore(String name, Integer score) {
        if (score == null || score < 0 || score > 10) {
            throw ApiException.badRequest(name + " score must be a whole number from 0 to 10");
        }
    }
}
