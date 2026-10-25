package com.ctags.model;

import java.util.Map;
import java.util.Set;

public enum TaskStatus {
    ASSIGNED, IN_PROGRESS, SUBMITTED, UNDER_REVIEW, GRADED, OVERDUE;

    /** Status changes an employee may make in Phase 1. Review and grading come later. */
    private static final Map<TaskStatus, Set<TaskStatus>> EMPLOYEE_TRANSITIONS = Map.of(
            ASSIGNED, Set.of(IN_PROGRESS),
            IN_PROGRESS, Set.of(SUBMITTED));

    public boolean employeeCanMoveTo(TaskStatus next) {
        return EMPLOYEE_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}
