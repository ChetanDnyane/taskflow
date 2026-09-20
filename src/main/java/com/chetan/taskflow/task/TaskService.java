package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="Task business logic and per-user access boundary">
/*
 * Lombok injects the task and user repositories. Every operation resolves the current principal to
 * a persisted user. Item operations query by both task ID and user ID before reading or modifying.
 * Creation fixes ownership server-side; update replaces editable fields, including null optional
 * fields. Results are mapped to DTOs. There is no service-level @Transactional annotation here;
 * repository operations use their own Spring Data transaction behavior.
 */
// </editor-fold>

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

    // <editor-fold defaultstate="collapsed" desc="Resolve the authenticated account">
    /*
     * The security principal name is the normalized email established by the authentication
     * layer. Resolve it to the database user to obtain a trusted owner ID. Absence is treated as
     * an inconsistent authenticated state, not a client-supplied owner. This service assumes
     * authentication was already enforced; calling it without a principal is unsupported.
     */
    // </editor-fold>
    private User getCurrentUser() {
        String email = getCurrentUserEmail();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated user not found"));
    }

    // <editor-fold defaultstate="collapsed" desc="Read request-local identity">
    /*
     * SecurityContextHolder exposes the authentication installed by the JWT filter for this
     * request. getName reads the principal username, which CustomUserDetailsService sets to email.
     */
    // </editor-fold>
    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    // <editor-fold defaultstate="collapsed" desc="Create with server-controlled ownership and defaults">
    /*
     * Use the current user rather than any request owner. Validation has guaranteed a title;
     * trim removes outer spaces. Optional description/date are copied as provided. A missing
     * priority becomes MEDIUM and all new tasks start TODO. Mapping the save result includes
     * the generated ID and persistence timestamps in the response.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="List only the current account's tasks">
    /*
     * Filtering happens in the repository query, before conversion to response objects.
     * Mapping avoids serializing the entity and its user association. Empty results become
     * an empty list. No explicit sort order or page limit is defined by this operation.
     */
    // </editor-fold>
    public List<TaskResponse> getMyTasks() {

        User currentUser = getCurrentUser();

        return taskRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // <editor-fold defaultstate="collapsed" desc="Read through an ownership-scoped query">
    /*
     * Combining ID and owner in one query makes a foreign task indistinguishable from a missing
     * task. Both produce TaskNotFoundException and the same 404 mapping. Only a matching task
     * can be converted into a public response.
     */
    // </editor-fold>
    public TaskResponse getTaskById(Long taskId) {

        User currentUser = getCurrentUser();

        Task task = taskRepository
                .findByIdAndUserId(taskId, currentUser.getId())
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        return toResponse(task);
    }

    // <editor-fold defaultstate="collapsed" desc="Replace editable fields after checking ownership">
    /*
     * Fetch by both task and owner ID before applying changes. PUT replaces title, description,
     * status, priority and dueDate; null optional fields clear saved values. It leaves owner,
     * ID and creation time intact. There is no transition restriction on valid status values.
     * Explicit save persists the changes; Hibernate supplies the update timestamp.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Delete only an owned, existing task">
    /*
     * The scoped lookup occurs before deletion, so knowing somebody else's task ID is insufficient.
     * Missing or foreign IDs throw rather than returning success. The controller returns an empty
     * 204 response only after the repository delete completes.
     */
    // </editor-fold>
    public void deleteTask(Long taskId) {

        User currentUser = getCurrentUser();

        Task task = taskRepository
                .findByIdAndUserId(taskId, currentUser.getId())
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        taskRepository.delete(task);
    }

    // <editor-fold defaultstate="collapsed" desc="Define the public task fields in one place">
    /*
     * All CRUD responses share this mapping to keep field selection consistent. It copies scalar
     * fields without touching task.user, so credentials and the lazy association stay out of JSON.
     */
    // </editor-fold>
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