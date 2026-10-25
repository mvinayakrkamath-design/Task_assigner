package com.ctags.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Table defined now; the grading engine is built in Phase 4. Each rubric score is 0-10. */
@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false, unique = true)
    private Task task;

    @ManyToOne(optional = false)
    @JoinColumn(name = "graded_by", nullable = false)
    private User gradedBy;

    private int quality;
    private int timeliness;
    private int completeness;
    private int communication;

    /** (0.4Q + 0.3T + 0.2C + 0.1M) / 10 x 100 */
    private double finalScore;

    @Column(length = 4000)
    private String comments;

    @Column(nullable = false, updatable = false)
    private LocalDateTime gradedAt;

    @PrePersist
    void onCreate() {
        gradedAt = LocalDateTime.now();
    }

    public static double computeFinalScore(int q, int t, int c, int m) {
        return (0.4 * q + 0.3 * t + 0.2 * c + 0.1 * m) / 10 * 100;
    }

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public User getGradedBy() { return gradedBy; }
    public void setGradedBy(User gradedBy) { this.gradedBy = gradedBy; }
    public int getQuality() { return quality; }
    public void setQuality(int quality) { this.quality = quality; }
    public int getTimeliness() { return timeliness; }
    public void setTimeliness(int timeliness) { this.timeliness = timeliness; }
    public int getCompleteness() { return completeness; }
    public void setCompleteness(int completeness) { this.completeness = completeness; }
    public int getCommunication() { return communication; }
    public void setCommunication(int communication) { this.communication = communication; }
    public double getFinalScore() { return finalScore; }
    public void setFinalScore(double finalScore) { this.finalScore = finalScore; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public LocalDateTime getGradedAt() { return gradedAt; }
}
