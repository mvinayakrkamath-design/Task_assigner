package com.ctags.dto;

import com.ctags.model.Grade;

import java.time.LocalDateTime;

/** Request and response DTOs for manager task grading. */
public final class GradeDtos {
    private GradeDtos() {}

    /** Each rubric score must be an integer from 0 through 10. */
    public record GradeRequest(Integer quality, Integer timeliness,
                               Integer completeness, Integer communication,
                               String comments) {}

    public record GradeDto(Long id, Long taskId, String taskTitle,
                           int quality, int timeliness, int completeness,
                           int communication, double finalScore,
                           String comments, Long gradedBy,
                           String gradedByName, LocalDateTime gradedAt) {
        public static GradeDto of(Grade grade) {
            return new GradeDto(
                    grade.getId(),
                    grade.getTask().getId(),
                    grade.getTask().getTitle(),
                    grade.getQuality(),
                    grade.getTimeliness(),
                    grade.getCompleteness(),
                    grade.getCommunication(),
                    grade.getFinalScore(),
                    grade.getComments(),
                    grade.getGradedBy().getId(),
                    grade.getGradedBy().getName(),
                    grade.getGradedAt()
            );
        }
    }
}
