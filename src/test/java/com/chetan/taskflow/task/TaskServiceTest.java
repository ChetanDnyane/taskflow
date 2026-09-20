package com.chetan.taskflow.task;

import com.chetan.taskflow.user.User;
import com.chetan.taskflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getTaskById_shouldReturnTask_whenTaskBelongsToCurrentUser() {

        // Arrange
        String email = "user@example.com";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null)
        );

        User user = new User();
        user.setId(1L);

        Task task = new Task();
        task.setId(10L);
        task.setTitle("Learn Kafka");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.HIGH);
        task.setUser(user);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(taskRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(task));

        // Act
        TaskResponse response = taskService.getTaskById(10L);

        // Assert
        assertEquals(10L, response.id());
        assertEquals("Learn Kafka", response.title());
        assertEquals(TaskStatus.TODO, response.status());
        assertEquals(TaskPriority.HIGH, response.priority());
    }
}
