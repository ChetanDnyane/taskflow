package com.chetan.taskflow.task;

import com.chetan.taskflow.user.User;
import com.chetan.taskflow.user.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = getCurrentUserEmail();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated user not found"));
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    public TaskResponse createTask(CreateTaskRequest request) {

        User currentUser = getCurrentUser();

        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setPriority(
                request.priority() != null
                        ? request.priority()
                        : TaskPriority.MEDIUM
        );
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(request.dueDate());
        task.setUser(currentUser);

        Task savedTask = taskRepository.save(task);

        return toResponse(savedTask);
    }

    public List<TaskResponse> getMyTasks() {

        User currentUser = getCurrentUser();

        return taskRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getTaskById(Long taskId) {

        User currentUser = getCurrentUser();

        Task task = taskRepository
                .findByIdAndUserId(taskId, currentUser.getId())
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        return toResponse(task);
    }

    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request) {

        User currentUser = getCurrentUser();

        Task task = taskRepository
                .findByIdAndUserId(taskId, currentUser.getId())
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());

        Task savedTask = taskRepository.save(task);

        return toResponse(savedTask);
    }

    public void deleteTask(Long taskId) {

        User currentUser = getCurrentUser();

        Task task = taskRepository
                .findByIdAndUserId(taskId, currentUser.getId())
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        taskRepository.delete(task);
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}