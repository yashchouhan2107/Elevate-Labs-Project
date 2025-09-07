package app;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Task implements Serializable {
    private String title;
    private int priority; // 1 = High, 2 = Medium, 3 = Low
    private LocalDateTime deadline;

    public Task(String title, int priority, LocalDateTime deadline) {
        this.title = title;
        this.priority = priority;
        this.deadline = deadline;
    }

    public String getTitle() { return title; }
    public int getPriority() { return priority; }
    public LocalDateTime getDeadline() { return deadline; }

    @Override
    public String toString() {
        return "[Priority: " + priority + "] " + title + " (Deadline: " + deadline + ")";
    }
}
