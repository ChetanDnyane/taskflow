# Reading TaskFlow

TaskFlow is a Java 21 Spring Boot REST API for registering users, signing in and
managing each user's own tasks. Expand the explanations below as needed.

<details>
<summary>How to expand and collapse source comments</summary>

Java explanations use IntelliJ-style `// <editor-fold defaultstate="collapsed"
desc="...">` and `// </editor-fold>` markers. Each region encloses **only the
explanation**, so collapsing it leaves the code visible. Use the editor gutter
fold control to open or close a note. IntelliJ recognizes these custom regions;
saved editor folding state or editor preferences may override the initial state.
Other Java editors can fold the multiline `/* ... */` comment even if they do
not recognize the custom markers. Folding support depends on the editor.

XML explanations in `pom.xml` use multiline XML comments. Properties, Git
configuration and shell scripts use native comment syntax with region labels;
not every editor supports folding those formats. Windows wrapper notes use
`@REM` within its batch portion. Where folding is unavailable, the explanations
below provide a collapsible companion. Markdown `<details>` sections are closed
initially in renderers that support HTML details; source editors show the markup.

Comments document project source, tests, configuration and launchers. Generated
`target/` files, `.git/` internals and personal `.idea/` metadata are not source
documentation: tools regenerate them. Existing upstream wrapper comments remain
available for the platform-specific implementation details.

</details>

<details>
<summary>Follow a request through the application</summary>

1. `TaskflowApplication` starts Spring and discovers components in its subpackages.
2. `SecurityConfig` installs a stateless filter chain. `JwtAuthenticationFilter`
   verifies a supplied bearer token using `JwtService`, loads the current account
   through `CustomUserDetailsService`, and installs the authenticated principal.
3. MVC routes the request to a controller, converts JSON into a request record,
   and applies its Bean Validation constraints where `@Valid` is present.
4. Controllers delegate business logic to services. `UserService` hashes passwords
   for registration. `TaskService` resolves the current user and uses owner-scoped
   repository queries before returning or changing tasks.
5. Spring Data supplies repository implementations. JPA entities describe tables,
   relationships, generated IDs and timestamp behavior.
6. Services return response records containing selected public fields. Controller
   advice converts supported failures into JSON errors. Filter-stage failures are
   written by the security handlers instead of controller advice.

For login, `AuthController` authenticates the email/password using Spring Security
before issuing a signed JWT. A later bearer request verifies that token and reloads
the user; it does not recheck the original password. No login session is retained.

</details>

<details>
<summary>Suggested source reading order</summary>

Start with `WorkflowTest.completeTaskLifecyclePersistsEditsAndDeletion` to see the
API from a client's perspective. Then read `TaskController`, `TaskService`,
`TaskRepository`, `Task` and the request/response records together. Each layer
answers a different question: routing, business rules, data access, persistence
shape and public JSON shape.

Next read `AuthController`, `UserService`, `RegisterRequest`, `User` and
`UserRepository` for account creation. Follow `SecurityConfig`,
`CustomUserDetailsService`, `PasswordEncoderConfig`, `JwtService` and
`JwtAuthenticationFilter` for authentication. `GlobalExceptionHandler` explains
the controller error contract. Enum and exception files explain their role in
the database, API and access-control behavior.

Tests include a context-startup smoke test, a Mockito service unit test, a direct
JWT contract test and the real-component MockMvc workflow suite. The workflow
comments explain every scenario and helper; [TESTING.md](TESTING.md) supplies
commands and the coverage map.

</details>

<details>
<summary>Configuration and build files</summary>

| File | Purpose and underlying behavior |
| --- | --- |
| `pom.xml` | Declares Java 21, Spring Boot dependency management, application/test dependencies and the Boot packaging plugin. Runtime scope supplies implementations needed when running; test scope stays in test execution. |
| `src/main/resources/application.properties` | Default PostgreSQL connection, Hibernate schema updates and SQL logging. Reads `DB_PASSWORD` and the Base64-encoded `JWT_SECRET` from external configuration. |
| `src/test/resources/application-test.properties` | Overrides database settings with H2, rebuilds tables, disables Flyway and supplies a public test signing key. Activated explicitly by Spring tests. |
| `.mvn/wrapper/maven-wrapper.properties` | Pins the Maven distribution downloaded by the launchers; wrapper and Maven versions are separate values. |
| `mvnw` | Unix shell bootstrapper that locates or downloads Maven, caches it and forwards command arguments. |
| `mvnw.cmd` | Hybrid CMD/PowerShell bootstrapper performing the equivalent process on Windows. Its initial hybrid marker must stay first. |
| `.gitattributes` | Keeps the Unix launcher in LF and Windows scripts in CRLF form through Git normalization. |
| `.gitignore` | Excludes generated builds, wrapper binaries and local IDE metadata; exceptions preserve source folders with matching names. |
| `TESTING.md` | Explains runnable scenarios, test commands, isolation and coverage limits. |
| `HELP.md` | Ignored, generated starter documentation links and an explanation of Maven metadata overrides. |
| `READING_GUIDE.md` | This overview and the folding instructions; it has no runtime effect. |

</details>

<details>
<summary>Important behavior to keep in mind while reading</summary>

- Missing and foreign-owned task IDs both yield 404. Queries enforce ownership
  using the current user's persisted ID, never an owner supplied in JSON.
- Task creation defaults to TODO/MEDIUM. Updates require status and priority and
  replace all editable fields; omitted description/date clear saved values.
- Any declared status can replace any other status. No transition rules, list
  sorting or pagination are currently implemented.
- Registration normalizes identity and hashes the unchanged password. Its response
  omits credentials. Database uniqueness remains the final duplicate-email guard.
- JWTs are signed, not encrypted. They contain email and timing claims; current
  roles come from the user table, and expiry is one hour after issuance.
- H2 integration tests exercise real application components but do not prove
  PostgreSQL-specific behavior or Flyway migrations. No migration scripts are
  currently present in this project.

</details>
