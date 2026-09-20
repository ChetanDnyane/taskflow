package com.chetan.taskflow;

import com.chetan.taskflow.auth.JwtService;
import com.chetan.taskflow.task.TaskRepository;
import com.chetan.taskflow.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real controllers, security, services and JPA; only the database is replaced by H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowTest {
    private static final String PASSWORD = "StudyPassword123!";
    private static final String EMAIL = "alice@example.com";
    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired TaskRepository tasks;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService jwt;

    @BeforeEach
    void resetDatabase() {
        // Delete children first to respect the foreign key. Tests never use the development database.
        tasks.deleteAll();
        users.deleteAll();
    }

    @Test
    void registrationNormalizesIdentityHashesPasswordAndReturnsOnlyPublicFields() throws Exception {
        register("  Alice  ", "  ALICE@EXAMPLE.COM  ", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        var saved = users.findByEmail(EMAIL).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, saved.getPasswordHash())).isTrue();
    }

    @Test
    void duplicateEmailIsRejectedAfterNormalization() throws Exception {
        register("Alice", EMAIL, PASSWORD).andExpect(status().isCreated());
        register("Another", " ALICE@EXAMPLE.COM ", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_REGISTERED"));
        assertThat(users.count()).isEqualTo(1);
    }

    static Stream<Object[]> invalidRegistrations() {
        return Stream.of(new Object[]{"name", ""}, new Object[]{"name", " "},
                new Object[]{"name", "n".repeat(101)}, new Object[]{"name", null},
                new Object[]{"email", ""}, new Object[]{"email", "invalid"},
                new Object[]{"email", "a".repeat(244) + "@example.com"}, new Object[]{"email", null},
                new Object[]{"password", ""}, new Object[]{"password", "1234567"},
                new Object[]{"password", "p".repeat(73)}, new Object[]{"password", null});
    }

    @ParameterizedTest(name = "registration rejects invalid {0}: {1}")
    @MethodSource("invalidRegistrations")
    void invalidRegistrationDoesNotSaveUser(String field, String value) throws Exception {
        var body = new HashMap<String, Object>(Map.of("name", "Alice", "email", EMAIL, "password", PASSWORD));
        body.put(field, value);
        send(post("/api/auth/register"), body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors." + field).exists());
        assertThat(users.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 72})
    void registrationAcceptsPasswordLengthBoundaries(int length) throws Exception {
        register("n".repeat(100), EMAIL, "p".repeat(length)).andExpect(status().isCreated());
    }

    @Test
    void loginAcceptsUppercaseEmailAndTokenAuthenticatesSubsequentRequest() throws Exception {
        register("Alice", EMAIL, PASSWORD).andExpect(status().isCreated());
        String token = tokenFrom(send(post("/api/auth/login"), Map.of("email", "ALICE@EXAMPLE.COM", "password", PASSWORD))
                .andExpect(status().isOk()));
        assertThat(jwt.extractEmail(token)).isEqualTo(EMAIL);
        mvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownUserWithoutLeakingWhichOneFailed() throws Exception {
        register("Alice", EMAIL, PASSWORD).andExpect(status().isCreated());
        for (var credentials : new String[][]{{EMAIL, "wrong"}, {"nobody@example.com", PASSWORD}}) {
            send(post("/api/auth/login"), Map.of("email", credentials[0], "password", credentials[1]))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.token").doesNotExist());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"email\":\"invalid\",\"password\":\"x\"}",
            "{\"email\":\"alice@example.com\",\"password\":\" \"}"})
    void loginValidatesRequiredCredentials(String body) throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void completeTaskLifecyclePersistsEditsAndDeletion() throws Exception {
        String token = registerAndLogin(EMAIL);
        long id = create(token, Map.of("title", "  Study tests  "));
        mvc.perform(get("/api/tasks/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Study tests"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andExpect(jsonPath("$.updatedAt").isNotEmpty());
        for (String taskStatus : new String[]{"IN_PROGRESS", "COMPLETED", "TODO"}) {
            send(put("/api/tasks/{id}", id).header("Authorization", bearer(token)),
                    Map.of("title", "  Revised  ", "description", "Read assertions", "status", taskStatus,
                            "priority", "HIGH", "dueDate", "2030-01-15"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Revised"))
                    .andExpect(jsonPath("$.description").value("Read assertions"))
                    .andExpect(jsonPath("$.status").value(taskStatus)).andExpect(jsonPath("$.priority").value("HIGH"))
                    .andExpect(jsonPath("$.dueDate").value("2030-01-15"));
            var saved = tasks.findById(id).orElseThrow();
            assertThat(saved.getStatus().name()).isEqualTo(taskStatus);
            assertThat(saved.getTitle()).isEqualTo("Revised");
        }
        mvc.perform(get("/api/tasks").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(delete("/api/tasks/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(tasks.existsById(id)).isFalse();
        mvc.perform(get("/api/tasks/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOW", "MEDIUM", "HIGH"})
    void createAcceptsAllPrioritiesAndMaximumLengthsAndUpdateClearsOptionalFields(String priority) throws Exception {
        String token = registerAndLogin(EMAIL);
        long id = create(token, Map.of("title", "t".repeat(200), "description", "d".repeat(2000),
                "priority", priority, "dueDate", "2030-01-15"));
        var saved = tasks.findById(id).orElseThrow();
        assertThat(saved.getPriority().name()).isEqualTo(priority);
        assertThat(saved.getDescription()).hasSize(2000);
        assertThat(saved.getDueDate().toString()).isEqualTo("2030-01-15");
        send(put("/api/tasks/{id}", id).header("Authorization", bearer(token)),
                Map.of("title", "Cleared", "status", "TODO", "priority", priority)).andExpect(status().isOk());
        saved = tasks.findById(id).orElseThrow();
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getDueDate()).isNull();
    }

    @Test
    void usersCannotListReadUpdateOrDeleteAnotherUsersTasks() throws Exception {
        String alice = registerAndLogin(EMAIL);
        long aliceId = create(alice, Map.of("title", "Private Alice task"));
        String bob = registerAndLogin("bob@example.com");
        mvc.perform(get("/api/tasks").header("Authorization", bearer(bob)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        long bobId = create(bob, Map.of("title", "Bob task"));
        mvc.perform(get("/api/tasks").header("Authorization", bearer(bob)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(bobId));
        for (long id : new long[]{aliceId, Long.MAX_VALUE}) {
            for (String method : new String[]{"GET", "PUT", "DELETE"}) {
                send(request(HttpMethod.valueOf(method), "/api/tasks/" + id).header("Authorization", bearer(bob)),
                        Map.of("title", "Intrusion", "status", "COMPLETED", "priority", "LOW"))
                        .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"));
            }
        }
        assertThat(tasks.findById(aliceId).orElseThrow().getTitle()).isEqualTo("Private Alice task");
        assertThat(tasks.count()).isEqualTo(2);
    }

    static Stream<Object[]> invalidTasks() {
        return Stream.of(new Object[]{"title", null}, new Object[]{"title", " "},
                new Object[]{"title", "t".repeat(201)}, new Object[]{"description", "d".repeat(2001)});
    }

    @ParameterizedTest(name = "task create/update reject invalid {0}")
    @MethodSource("invalidTasks")
    void taskValidationRejectsInvalidCreateAndUpdateWithoutChangingStoredTask(String field, String value) throws Exception {
        String token = registerAndLogin(EMAIL);
        long id = create(token, Map.of("title", "Original"));
        var body = new HashMap<String, Object>(Map.of("title", "Valid", "status", "TODO", "priority", "LOW"));
        body.put(field, value);
        for (var endpoint : new MockHttpServletRequestBuilder[]{post("/api/tasks"), put("/api/tasks/{id}", id)}) {
            send(endpoint.header("Authorization", bearer(token)), body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors." + field).exists());
        }
        assertThat(tasks.count()).isEqualTo(1);
        assertThat(tasks.findById(id).orElseThrow().getTitle()).isEqualTo("Original");
    }

    @ParameterizedTest
    @ValueSource(strings = {"status", "priority"})
    void updateRequiresStatusAndPriority(String field) throws Exception {
        String token = registerAndLogin(EMAIL);
        long id = create(token, Map.of("title", "Original"));
        var body = new HashMap<String, Object>(Map.of("title", "Changed", "status", "TODO", "priority", "LOW"));
        body.remove(field);
        send(put("/api/tasks/{id}", id).header("Authorization", bearer(token)), body)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors." + field).exists());
        assertThat(tasks.findById(id).orElseThrow().getTitle()).isEqualTo("Original");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "", "{\"title\":\"Task\",\"priority\":\"URGENT\"}",
            "{\"title\":\"Task\",\"dueDate\":\"not-a-date\"}"})
    void malformedTaskRequestsAreBadRequests(String body) throws Exception {
        String token = registerAndLogin(EMAIL);
        mvc.perform(post("/api/tasks").header("Authorization", bearer(token)).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        assertThat(tasks.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE"})
    void taskEndpointsRequireAuthentication(String method) throws Exception {
        String path = method.equals("PUT") || method.equals("DELETE") ? "/api/tasks/1" : "/api/tasks";
        send(request(HttpMethod.valueOf(method), path), Map.of("title", "Task"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mvc.perform(get("/api/tasks/1")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"malformed", "expired", "wrong-signature", "deleted-user", "empty"})
    void invalidBearerTokensReturnStructuredUnauthorizedResponse(String kind) throws Exception {
        String token = switch (kind) {
            case "expired" -> Jwts.builder().subject(EMAIL).expiration(Date.from(Instant.now().minusSeconds(60)))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_KEY))).compact();
            case "wrong-signature" -> new JwtService("YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=").generateToken(EMAIL);
            case "deleted-user" -> jwt.generateToken("deleted@example.com");
            case "empty" -> "";
            default -> "not-a-jwt";
        };
        mvc.perform(get("/api/tasks").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void ordinaryUserIsForbiddenFromAdminRouteAndBasicAuthenticationIsNotAccepted() throws Exception {
        String token = registerAndLogin(EMAIL);
        mvc.perform(get("/api/auth/admin-test").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(get("/api/tasks").header("Authorization", "Basic ignored"))
                .andExpect(status().isUnauthorized());
        // A previous bearer request must not authenticate a later request without a token.
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    private ResultActions register(String name, String email, String password) throws Exception {
        return send(post("/api/auth/register"), Map.of("name", name, "email", email, "password", password));
    }

    private String registerAndLogin(String email) throws Exception {
        register("Test user", email, PASSWORD).andExpect(status().isCreated());
        return tokenFrom(send(post("/api/auth/login"), Map.of("email", email, "password", PASSWORD))
                .andExpect(status().isOk()));
    }

    private String tokenFrom(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.token");
    }

    private long create(String token, Map<String, ?> body) throws Exception {
        String response = send(post("/api/tasks").header("Authorization", bearer(token)), body)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String bearer(String token) { return "Bearer " + token; }

    private ResultActions send(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return mvc.perform(request.contentType("application/json").content(json.writeValueAsString(body)));
    }
}
