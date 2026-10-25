package com.ctags.service;
import com.ctags.model.Task;
import com.ctags.repository.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class DeadlineReminderService {
    private final TaskRepository tasks;
    public DeadlineReminderService(TaskRepository tasks) {
        this.tasks = tasks;
    }
    @Scheduled(fixedRate = 60000)
    public void checkDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime upcoming = now.plusHours(24);
        List<Task> allTasks = tasks.findAll();
        for (Task task : allTasks) {
            if (task.getDeadline() != null
                    && task.getDeadline().isAfter(now)
                    && !task.getDeadline().isAfter(upcoming)) {
                System.out.println(
                    "Deadline reminder: " + task.getTitle()
                    + " is due at " + task.getDeadline()
                );
            }
        }
    }
}