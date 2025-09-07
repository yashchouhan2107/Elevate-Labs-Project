package app;

import java.util.PriorityQueue;

public class TaskScheduler {
    private PriorityQueue<Task> taskQueue;

    public TaskScheduler() {
        taskQueue = new PriorityQueue<>((t1, t2) -> {
            if (t1.getPriority() != t2.getPriority())
                return Integer.compare(t1.getPriority(), t2.getPriority());
            return t1.getDeadline().compareTo(t2.getDeadline());
        });
    }

    public void addTask(Task task) {
        taskQueue.offer(task);
    }

    public Task getNextTask() {
        return taskQueue.poll();
    }

    public PriorityQueue<Task> getAllTasks() {
        return new PriorityQueue<>(taskQueue);
    }
}
