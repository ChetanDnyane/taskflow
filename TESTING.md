# TaskFlow workflow tests

<details>
<summary>How to read this file and the test comments</summary>

This document maps API behavior to the test suites and explains how to run them.
The Java files now contain collapsible explanation blocks beside each workflow
and helper. Each block explains the setup, the behavior under test and what the
assertions establish. Expand a block in IntelliJ using its gutter folding control;
the comments are marked `defaultstate="collapsed"` so supporting editors initially
hide the explanation while keeping the executable test visible.

For the overall request flow and folding support in other file formats, see
[READING_GUIDE.md](READING_GUIDE.md). The verification count later in this file
records the earlier September 20 run; rerunning Maven produces fresh Surefire
reports under `target/surefire-reports/`.

</details>

Start with `src/test/java/com/chetan/taskflow/WorkflowTest.java`. It exercises every currently implemented API workflow through MockMvc using the real Spring application, security filter chain, password encoder, services and JPA repositories. Method names describe the expected behavior. Parameterized tests repeat a scenario for each input shown in `@ValueSource` or `@MethodSource`.

## Workflow map

| Workflow | What the tests check |
| --- | --- |
| Register (`POST /api/auth/register`) | 201 response, identity normalization, default USER role, persisted password hash, public response fields, duplicate email conflict, missing/blank/invalid fields, length limits and password boundary acceptance |
| Login (`POST /api/auth/login`) | Correct credentials, uppercase email, usable signed token, unknown account, incorrect password, invalid request fields |
| Create (`POST /api/tasks`) | Persisted task, trimmed title, TODO/MEDIUM defaults, all priorities, optional description/date, maximum field lengths, invalid inputs |
| List (`GET /api/tasks`) | Empty list, populated list, only the authenticated user's tasks |
| Read (`GET /api/tasks/{id}`) | Saved fields and timestamps, missing task, another user's task, deleted task |
| Update (`PUT /api/tasks/{id}`) | Persisted title/description/priority/date edits, each supported status, clearing optional fields, required status/priority, invalid fields, missing/foreign tasks |
| Delete (`DELETE /api/tasks/{id}`) | 204 and empty body, removal from persistence, subsequent read returns 404, missing/foreign tasks cannot be deleted |
| Authentication and authorization | Task endpoints reject anonymous requests; malformed, expired, incorrectly signed, empty and deleted-user tokens return 401; ordinary users receive 403 for the configured admin-only path; Basic authentication is rejected; authentication does not carry into a later tokenless request |
| Token generation | Verified signature, email subject, issue time and one-hour expiry, no password claims |
| Application startup | Spring context starts with isolated test configuration |

`completeTaskLifecyclePersistsEditsAndDeletion` is a good first test to study: it registers a user, logs in, creates a task, retrieves it, changes its status and fields, lists it, deletes it and verifies it is gone. The small helpers at the bottom handle repeated HTTP setup. Assertions against repositories confirm that HTTP results correspond to saved data.

`TaskServiceTest` is a focused Mockito unit test illustrating arrange/act/assert without starting Spring. `JwtServiceTest` directly verifies generated token claims. The workflow tests additionally cover exception response mappings and repository ownership queries.

## Run the tests

Use JDK 21. From the project directory in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\mvnw.cmd test
# Run only the workflow suite:
.\mvnw.cmd '-Dtest=WorkflowTest' test
# Run one scenario:
.\mvnw.cmd '-Dtest=WorkflowTest#completeTaskLifecyclePersistsEditsAndDeletion' test
```

If the wrapper cannot launch in your terminal, use an installed Maven 3.9 executable (`mvn test`) with the same JDK. In IntelliJ IDEA, reload the Maven project after adding H2, select JDK 21, then right-click `src/test/java` and choose **Run All Tests**. The gutter icon next to a test runs just that scenario.

Maven writes detailed reports to `target/surefire-reports/`. Each parameterized invocation counts as a separate test.

Verified on September 20, 2026 with Java 21 and Maven 3.9.16: **49 tests, 0 failures, 0 errors, 0 skipped** (46 workflow cases and 3 focused/startup tests).

## Regression found by the tests

The first run exposed unhandled malformed, expired and empty bearer tokens. `JwtAuthenticationFilter` imported Spring Security's `JwtException`, but `JwtService` uses JJWT, which throws `io.jsonwebtoken.JwtException`. The filter now catches JJWT exceptions and the `IllegalArgumentException` raised for empty tokens. The invalid-token scenarios verify that these requests return the existing `401 INVALID_TOKEN` JSON response.

## Isolation and scope

All Spring tests explicitly activate `test`, using `src/test/resources/application-test.properties`. H2 runs in memory with PostgreSQL compatibility mode; tables are created from the entities. The workflow suite deletes test tasks and users before each scenario. No PostgreSQL server, `DB_PASSWORD`, or `JWT_SECRET` is needed. The checked-in signing key is exclusively for tests.

This is API integration coverage through MockMvc, not a browser or live-server test. H2 does not prove PostgreSQL-specific SQL behavior or migrations; Flyway is disabled for these tests, and this project currently contains no migration scripts. The admin path exists only as a security rule, so its test verifies access denial rather than an admin business workflow. There are no implemented logout, password reset or UI workflows to exercise.
