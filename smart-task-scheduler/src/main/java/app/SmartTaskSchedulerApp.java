package app;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class SmartTaskSchedulerApp {
    public static void main(String[] args) {
        TaskScheduler scheduler = new TaskScheduler();
        List<Task> tasks = TaskStorage.loadTasks();
        tasks.forEach(scheduler::addTask);

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- Smart Task Scheduler ---");
            System.out.println("1. Add Task");
            System.out.println("2. Show Tasks");
            System.out.println("3. Get Next Task");
            System.out.println("4. Exit");
            System.out.print("Choose: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter title: ");
                    String title = sc.nextLine();
                    System.out.print("Enter priority (1=High, 2=Medium, 3=Low): ");
                    int priority = sc.nextInt();
                    sc.nextLine();
                    System.out.print("Enter deadline (YYYY-MM-DDTHH:MM): ");
                    String deadlineStr = sc.nextLine();
                    LocalDateTime deadline = LocalDateTime.parse(deadlineStr);
                    Task task = new Task(title, priority, deadline);
                    scheduler.addTask(task);
                    tasks.add(task);
                    TaskStorage.saveTasks(tasks);
                }
                case 2 -> scheduler.getAllTasks().forEach(System.out::println);
                case 3 -> {
                    Task next = scheduler.getNextTask();
                    System.out.println(next != null ? "Next Task: " + next : "No tasks available");
                }
                case 4 -> {
                    TaskStorage.saveTasks(tasks);
                    System.out.println("Goodbye!");
                    return;
                }
            }
        }
    }
}
