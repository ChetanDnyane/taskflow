package com.chetan.taskflow;

// <editor-fold defaultstate="collapsed" desc="Integration tests of complete API workflows">
/*
 * MockMvc sends simulated HTTP requests through real MVC controllers and security filters without
 * opening a network port. Real services, BCrypt and JPA use the in-memory H2 test profile.
 * Each scenario starts with an empty database. Parameterized tests repeat for every supplied input.
 * HTTP assertions verify the client contract; repository assertions independently verify saved data.
 * Helpers at the bottom remove repeated JSON/login setup. Start with the complete lifecycle test.
 * H2 compatibility mode does not verify PostgreSQL-specific behavior, and Flyway is disabled.
 */
// </editor-fold>

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

    // <editor-fold defaultstate="collapsed" desc="Reset isolation before every case">
    /*
     * Delete tasks before users because tasks reference users. Repository cleanup commits independently
     * of the requests; tests can inspect the actual persisted results after each operation.
     */
    // </editor-fold>
    @BeforeEach
    void resetDatabase() {
        // Delete children first to respect the foreign key. Tests never use the development database.
        tasks.deleteAll();
        users.deleteAll();
    }

    // <editor-fold defaultstate="collapsed" desc="Verify identity, credentials and response privacy">
    /*
     * Send padded, uppercase identity fields, then assert normalized JSON and the default role.
     * Read the saved account and verify BCrypt matches the original password while the stored
     * hash differs from plaintext. Password fields must not appear in the response.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Prove normalization participates in uniqueness">
    /*
     * Create one account, then retry with a case/whitespace variant. Expect 409 and verify the
     * database still contains exactly one row, not merely an error-looking response.
     */
    // </editor-fold>
    @Test
    void duplicateEmailIsRejectedAfterNormalization() throws Exception {
        register("Alice", EMAIL, PASSWORD).andExpect(status().isCreated());
        register("Another", " ALICE@EXAMPLE.COM ", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_REGISTERED"));
        assertThat(users.count()).isEqualTo(1);
    }

    // <editor-fold defaultstate="collapsed" desc="Supply one invalid registration field at a time">
    /*
     * Each pair names a field and its replacement value. Inputs cover missing/blank values,
     * bad email format and sizes outside allowed bounds. A mutable map later permits nulls.
     */
    // </editor-fold>
    static Stream<Object[]> invalidRegistrations() {
        return Stream.of(new Object[]{"name", ""}, new Object[]{"name", " "},
                new Object[]{"name", "n".repeat(101)}, new Object[]{"name", null},
                new Object[]{"email", ""}, new Object[]{"email", "invalid"},
                new Object[]{"email", "a".repeat(244) + "@example.com"}, new Object[]{"email", null},
                new Object[]{"password", ""}, new Object[]{"password", "1234567"},
                new Object[]{"password", "p".repeat(73)}, new Object[]{"password", null});
    }

    // <editor-fold defaultstate="collapsed" desc="Check validation prevents persistence">
    /*
     * Start with an otherwise valid body and replace only the supplied field to isolate its rule.
     * Assert HTTP 400 and a field-specific error, then confirm no account was inserted.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Exercise inclusive size boundaries">
    /*
     * Run once at 8 and once at 72 password characters, with a maximum-length name. Successful
     * registration proves the limits themselves are accepted rather than treated as out of range.
     */
    // </editor-fold>
    @ParameterizedTest
    @ValueSource(ints = {8, 72})
    void registrationAcceptsPasswordLengthBoundaries(int length) throws Exception {
        register("n".repeat(100), EMAIL, "p".repeat(length)).andExpect(status().isCreated());
    }

    // <editor-fold defaultstate="collapsed" desc="Follow a token from login into a protected request">
    /*
     * Register normally, log in using uppercase email, and verify the issued token subject.
     * Use the same token against task listing to exercise the filter, principal and empty list.
     */
    // </editor-fold>
    @Test
    void loginAcceptsUppercaseEmailAndTokenAuthenticatesSubsequentRequest() throws Exception {
        register("Alice", EMAIL, PASSWORD).andExpect(status().isCreated());
        String token = tokenFrom(send(post("/api/auth/login"), Map.of("email", "ALICE@EXAMPLE.COM", "password", PASSWORD))
                .andExpect(status().isOk()));
        assertThat(jwt.extractEmail(token)).isEqualTo(EMAIL);
        mvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    // <editor-fold defaultstate="collapsed" desc="Compare both credential failure paths">
    /*
     * Try a known account with a bad password and an unknown account with the normal password.
     * Both must produce the same error code and omit a token; neither reaches token issuance.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Reject invalid login payloads before authentication">
    /*
     * Parameterized raw JSON covers absent fields, invalid email syntax and a blank password.
     * Expect the validation envelope rather than an invalid-credentials result.
     */
    // </editor-fold>
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"email\":\"invalid\",\"password\":\"x\"}",
            "{\"email\":\"alice@example.com\",\"password\":\" \"}"})
    void loginValidatesRequiredCredentials(String body) throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    // <editor-fold defaultstate="collapsed" desc="Walk through the complete task lifecycle">
    /*
     * Register/login, create with defaults, and retrieve the trimmed title and timestamps.
     * Update through every supported status, checking both response values and saved state.
     * List the task, delete it, then prove both physical removal and a subsequent 404.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Verify valid extremes and PUT clearing semantics">
    /*
     * Create at the title/description limits for each priority and inspect saved optional values.
     * Update without description/date, then reload to prove those fields are cleared, not retained.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Test the ownership boundary with two real users">
    /*
     * Create Alice's task and use Bob's token for listing and all item operations. Compare Alice's
     * ID with a nonexistent ID: both must give 404 for Bob. Finally verify Alice's task and the
     * total row count remain unchanged, proving failed mutations have no persistence side effects.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Supply shared create/update validation failures">
    /*
     * Return null/blank/overlong titles and an overlong description. These constraints apply to
     * both request records, so each pair is exercised against POST and PUT.
     */
    // </editor-fold>
    static Stream<Object[]> invalidTasks() {
        return Stream.of(new Object[]{"title", null}, new Object[]{"title", " "},
                new Object[]{"title", "t".repeat(201)}, new Object[]{"description", "d".repeat(2001)});
    }

    // <editor-fold defaultstate="collapsed" desc="Validate both writes without losing saved state">
    /*
     * Create a baseline task, mutate one field in an otherwise valid payload, and send it to
     * both write endpoints. Check field-specific 400 responses, row count and the original title.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Distinguish update requirements from creation defaults">
    /*
     * Remove status or priority from a valid PUT request. Expect a field error and confirm the
     * stored title did not change, even though the submitted title was different.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Check failures during JSON conversion">
    /*
     * Send incomplete/empty JSON, an unknown enum name or an invalid date. These fail request
     * deserialization before Bean Validation; assert HTTP 400 without assuming the custom
     * validation envelope, and confirm nothing was saved.
     */
    // </editor-fold>
    @ParameterizedTest
    @ValueSource(strings = {"{", "", "{\"title\":\"Task\",\"priority\":\"URGENT\"}",
            "{\"title\":\"Task\",\"dueDate\":\"not-a-date\"}"})
    void malformedTaskRequestsAreBadRequests(String body) throws Exception {
        String token = registerAndLogin(EMAIL);
        mvc.perform(post("/api/tasks").header("Authorization", bearer(token)).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        assertThat(tasks.count()).isZero();
    }

    // <editor-fold defaultstate="collapsed" desc="Check security before business logic">
    /*
     * Call each HTTP operation without a bearer token and expect the UNAUTHORIZED envelope.
     * The body need not satisfy every business rule because security rejects the request first.
     * An individual task GET is also checked explicitly.
     */
    // </editor-fold>
    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE"})
    void taskEndpointsRequireAuthentication(String method) throws Exception {
        String path = method.equals("PUT") || method.equals("DELETE") ? "/api/tasks/1" : "/api/tasks";
        send(request(HttpMethod.valueOf(method), path), Map.of("title", "Task"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mvc.perform(get("/api/tasks/1")).andExpect(status().isUnauthorized());
    }

    // <editor-fold defaultstate="collapsed" desc="Exercise distinct token rejection paths">
    /*
     * Construct an expired signed token, a token signed with another key, a subject with no
     * database account, an empty token and malformed text. Each must be rejected by the filter
     * with the same 401 INVALID_TOKEN envelope. Keys here are public test fixtures only.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Separate missing identity from insufficient permission">
    /*
     * A valid USER token is authenticated but lacks ADMIN, giving 403 on the configured route.
     * A Basic header does not authenticate, giving 401. A later tokenless request also gives 401,
     * proving authentication from the earlier request is not retained in a session.
     */
    // </editor-fold>
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

    // <editor-fold defaultstate="collapsed" desc="Reusable registration request">
    /*
     * Build the normal JSON fields and return ResultActions so each caller selects its own
     * expected outcome. Invalid-null cases use a HashMap elsewhere because Map.of rejects nulls.
     */
    // </editor-fold>
    private ResultActions register(String name, String email, String password) throws Exception {
        return send(post("/api/auth/register"), Map.of("name", name, "email", email, "password", password));
    }

    // <editor-fold defaultstate="collapsed" desc="Obtain a real bearer credential for a test user">
    /*
     * Require registration and login success before returning the token. Tests using this helper
     * exercise the production hashing/authentication path rather than installing mock principals.
     */
    // </editor-fold>
    private String registerAndLogin(String email) throws Exception {
        register("Test user", email, PASSWORD).andExpect(status().isCreated());
        return tokenFrom(send(post("/api/auth/login"), Map.of("email", email, "password", PASSWORD))
                .andExpect(status().isOk()));
    }

    // <editor-fold defaultstate="collapsed" desc="Extract the token from an asserted login response">
    /*
     * JsonPath reads the token field from response JSON. Callers assert the expected status first
     * so setup failures do not silently masquerade as the later scenario under test.
     */
    // </editor-fold>
    private String tokenFrom(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.token");
    }

    // <editor-fold defaultstate="collapsed" desc="Create a task and return its generated ID">
    /*
     * Send authenticated JSON, require HTTP 201 and read the ID from the response. Converting
     * through Number handles the numeric representation returned by JsonPath before using long.
     */
    // </editor-fold>
    private long create(String token, Map<String, ?> body) throws Exception {
        String response = send(post("/api/tasks").header("Authorization", bearer(token)), body)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    // <editor-fold defaultstate="collapsed" desc="Build the Authorization header value">
    /*
     * Use the exact case-sensitive Bearer prefix followed by one space. Empty-token test cases
     * deliberately still include this prefix to reach the invalid-token path.
     */
    // </editor-fold>
    private String bearer(String token) { return "Bearer " + token; }

    // <editor-fold defaultstate="collapsed" desc="Serialize request bodies consistently">
    /*
     * Use the application ObjectMapper and application/json so tests exercise the configured
     * JSON binding. Return ResultActions to allow status, header and payload assertions inline.
     */
    // </editor-fold>
    private ResultActions send(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return mvc.perform(request.contentType("application/json").content(json.writeValueAsString(body)));
    }
}
