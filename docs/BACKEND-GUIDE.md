# Backend guide: Java, Maven and Hibernate

What to know about the tools and files of this project, and short answers to likely questions.

## The project in 30 seconds
A campus lost and found. The backend is a **Java 21** program built with **Maven** on **Spring Boot**. It stores data in **PostgreSQL** through **Hibernate** and answers a **React** website with JSON. Students post and claim items, and an admin decides.

## 1. Maven

**What it is:** a build tool. It downloads the libraries the project needs, compiles the code, runs the tests and packages the result. Everything it needs to know is in `pom.xml` (Project Object Model).

### `pom.xml` section by section
| Section | What it says here |
|---|---|
| `<parent>` | `spring-boot-starter-parent 3.5.16`. It picks tested, matching versions of all Spring libraries, so dependencies need no version number. |
| `groupId` / `artifactId` / `version` | The project's identity: `com.lostfound` / `lostandfound` / `0.0.1-SNAPSHOT`. |
| `<properties>` | `java.version` is `21`. |
| `<dependencies>` | The libraries used (table below). |
| `<build>` | The Spring Boot plugin. It adds `mvn spring-boot:run` and builds a runnable jar. |

### The dependencies
Most are Spring Boot **starters**: one line that pulls in a whole group of libraries.

| Dependency | Gives the project |
|---|---|
| `spring-boot-starter-web` | REST controllers, JSON, and the built-in Tomcat web server |
| `spring-boot-starter-data-jpa` | JPA and **Hibernate**, to work with the database using Java objects |
| `spring-boot-starter-security` | Login, roles and URL protection |
| `spring-boot-starter-oauth2-resource-server` | Reading and checking login tokens |
| `spring-boot-starter-validation` | Rules like `@NotBlank` and `@Size` on request data |
| `postgresql` (runtime) | The driver that connects to PostgreSQL. Needed only when the app runs. |
| `spring-boot-starter-test` (test) | JUnit, Mockito and MockMvc. Needed only for tests. |
| `spring-security-test` (test) | Helpers for testing protected URLs |

`scope` decides when a library is available: the default is always, `runtime` is only when running, and `test` is only when testing.

### Standard Maven folders
| Folder | Holds |
|---|---|
| `src/main/java` | Application code |
| `src/main/resources` | Settings and other files (`application.properties`) |
| `src/test/java` | Test code |
| `target/` | Generated output: compiled classes and test reports. Safe to delete. |

### Commands
| Command | What it does |
|---|---|
| `mvn clean` | Deletes `target/` |
| `mvn compile` | Compiles the code |
| `mvn test` | Compiles and runs all tests |
| `mvn package` | Tests, then builds the jar in `target/` |
| `mvn spring-boot:run` | Starts the server |
| `mvn clean test` | Fresh build, then all tests (what we use) |

A Maven build runs in order: **clean, compile, test, package**. Each command also runs the steps before it.

### Maven compared with Gradle
**Gradle** is the other widely used Java build tool, and the default for Android. It does the same job as Maven: it downloads libraries, compiles, tests and packages. This project could be built with either, because Spring Boot supports both.

| | Maven (used here) | Gradle |
|---|---|---|
| Build file | `pom.xml`, written in XML | `build.gradle` or `build.gradle.kts`, written in Groovy or Kotlin code |
| Style | Fixed structure: you describe the project and Maven follows its standard steps | Flexible: the build file is a script you can program |
| Build order | Fixed lifecycle: clean, compile, test, package | A graph of tasks that only run when needed |
| Speed | Rebuilds more each time | Faster on large projects (it skips work that has not changed and caches results) |
| Output folder | `target/` | `build/` |
| Libraries | Downloaded from Maven Central | The same Maven Central |
| Folder layout | `src/main/java`, `src/main/resources`, `src/test/java` | The same layout |
| Learning curve | Easy to read, and every Maven project looks alike | More powerful, but harder to read |

The same dependency in both:

```xml
<!-- Maven: pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
```groovy
// Gradle: build.gradle
implementation 'org.springframework.boot:spring-boot-starter-web'
```

| Idea | Maven | Gradle |
|---|---|---|
| Normal dependency | default scope | `implementation` |
| Only needed when running | `runtime` | `runtimeOnly` |
| Only needed for tests | `test` | `testImplementation` |
| Run the tests | `mvn test` | `./gradlew test` |
| Build the jar | `mvn package` | `./gradlew build` |
| Start Spring Boot | `mvn spring-boot:run` | `./gradlew bootRun` |

**Why Maven here:** its structure is standard and simple, which makes the project easy to explain and any Maven project easy to read. **Ant** is an older tool that you may also hear about. It needs every build step written by hand and does not manage libraries itself, so it is rarely used for new projects.

## 2. Java as used in this project

### Packages and imports
Code is grouped into packages that match folders: `com.lostfound.controller` lives in `src/main/java/com/lostfound/controller`. `import` brings in a class from another package. The base package is the reverse of a domain name, by convention.

### The kinds of types used
| Kind | Meaning | Example here |
|---|---|---|
| **Class** | A blueprint for objects | `Item`, `ItemService` |
| **Interface** | A list of methods with no code. Others supply the code. | `ItemRepository` (Spring writes the code) |
| **Enum** | A fixed set of values | `ItemStatus`, `Role` |
| **Record** | A short, read-only data class | `ItemRequest`, `ItemResponse` |
| **Abstract / inherited class** | A class that extends another | `BadRequestException` extends `RuntimeException` |

### Core ideas, with where to point
| Idea | Where it appears |
|---|---|
| Encapsulation (private fields, public getters) | `User`, `Item`, `Claim` |
| Constructors and overloading | `Item` has two constructors (with and without a date) |
| Composition (an object holding another) | `Item` has a reporter (`User`); `Claim` has an `Item` and a claimant |
| Inheritance | The custom exceptions extend `RuntimeException` |
| Interfaces and polymorphism | Repositories are interfaces; `JsonSecurityErrorHandler` implements two interfaces |
| Enums with behaviour | `ItemStatus.isActive()` |
| Dependency injection | Every controller and service receives what it needs through its constructor |

### Exceptions
Exceptions that extend `RuntimeException` are **unchecked**: the compiler does not force you to catch them. The app throws them (for example `ConflictException`) and one class, `GlobalExceptionHandler`, turns each into a clean reply.

### Annotations used
An annotation is a label that tells Spring or Hibernate how to treat a class or method.

| Annotation | Meaning |
|---|---|
| `@SpringBootApplication` | The starting class. Spring scans this package and its sub-packages. |
| `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` | A class that handles web requests, and which URL and method each function answers |
| `@Service` | A class that holds the app's rules |
| `@Component`, `@Configuration`, `@Bean` | Classes and objects Spring should create and manage |
| `@Entity`, `@Table`, `@Id`, `@Column` | A class that is a database table, its key and its columns |
| `@ManyToOne`, `@JoinColumn` | A link between two tables (a foreign key) |
| `@Enumerated` | Store an enum as readable text |
| `@Transactional` | Do several database changes as one all-or-nothing step |
| `@NotBlank`, `@Size`, `@PastOrPresent` | Rules for incoming data |
| `@RestControllerAdvice`, `@ExceptionHandler` | Turn exceptions into replies |
| `@Scheduled` | Run a method on a timer |
| `@Override` | This method replaces one from a parent class or interface |

### Newer Java features used
- **Records** for the DTOs (Data Transfer Objects).
- **Pattern matching for `instanceof`** in `GlobalExceptionHandler`.
- **Text blocks** (`"""`) for JSON in tests.
- `List.of(...)` and `Map.of(...)` for short fixed lists.

## 3. Hibernate and JPA

- **JPA** (Jakarta Persistence) is the standard for mapping Java classes to database tables. **Hibernate** is the library that does the work. Spring Data JPA sits on top and makes it shorter.
- An **entity** (`@Entity`) is a class mapped to a table. `User` is table `users`, `Item` is `items`, `Claim` is `claims`. One object is one row, and each field is a column.
- **Relationships:** `@ManyToOne` on `Item.reporter` creates the `reporter_id` foreign key. `LAZY` means the related user is loaded only when needed.
- **Tables are created automatically.** `spring.jpa.hibernate.ddl-auto=update` makes Hibernate create missing tables and columns at start. It never drops data, but real projects use migration tools instead.
- **Repositories:** an interface that extends `JpaRepository` gets save, find, delete and count for free. Methods such as `findByEmail` need no code, because Spring builds the SQL from the method name.
- **Custom queries:** `@Query` for one hand-written query, and `ItemSpecifications` to build the search from whichever filters were used.
- **Seeing the SQL:** `spring.jpa.show-sql=true` prints every query Hibernate runs.
- **No SQL in the services:** services call repositories, and Hibernate turns that into SQL for PostgreSQL.

## 4. What each kind of file is for
| File kind | Job | Example |
|---|---|---|
| Entity (`model/`) | One table | `Item` |
| DTO, Data Transfer Object (`dto/`) | The data shape that goes in or out of the API | `ItemRequest` |
| Repository (`repository/`) | Reads and saves data | `ItemRepository` |
| Service (`service/`) | The rules of the app | `ClaimService` |
| Controller (`controller/`) | Receives web requests, calls a service | `ClaimController` |
| Config (`config/`) | Setup done at start | `SecurityConfig` |
| Exception (`exception/`) | Errors and how they become replies | `GlobalExceptionHandler` |
| Test (`src/test/`) | Checks that the code works | `ClaimServiceTest` |

The folder-by-folder map is in [ARCHITECTURE.md](ARCHITECTURE.md).

## 5. Spring Boot basics
- **Spring Boot** sets up a web server and wires the project together with little configuration.
- **Dependency injection:** a class asks for what it needs in its constructor, and Spring creates and passes it in. We write no `new` for services or repositories.
- **Component scan:** Spring finds every `@Service`, `@RestController` and similar class under the starting package.
- **Settings:** `application.properties` holds them, and `.env` supplies the private values (`${DB_URL}` and so on).

## 6. Testing basics
- **JUnit 5** runs the tests, and `@Test` marks a test method.
- **Mockito** creates fake repositories so service tests need no database.
- **MockMvc** calls controllers without starting a server.
- Database and end-to-end tests use the real PostgreSQL and roll back, so no data is left behind.
- `src/test/java` mirrors the main folders, and each test is named after its class.
- Run everything with `mvn clean test`.

## 7. Likely questions
| Question | Short answer |
|---|---|
| What is Maven and why use it? | A build tool. It fetches libraries, compiles, tests and packages the project, all driven by `pom.xml`. |
| What is `pom.xml`? | The file that describes the project: its identity, Java version, dependencies and build plugin. |
| What is a Spring Boot starter? Why no version numbers? | A starter is one dependency that brings a whole group of libraries. The parent POM picks matching versions for all of them. |
| What are `src/main`, `src/test` and `target/`? | Maven's standard places for application code, test code and generated build output. `target/` can be deleted and is not committed. |
| What does `mvn clean test` do? | Deletes `target/`, compiles, and runs every test. |
| How is Gradle different from Maven? | Another build tool for the same job. Its build file is code instead of XML, it is faster on big projects and more flexible. Maven is stricter and simpler. This project could use either. |
| What is a DTO and why use one? | DTO means Data Transfer Object: a small object (here a Java record) for data going in or out of the API. It controls which fields a client can send or see. |
| Why are repositories interfaces? | An interface lists methods only. Spring writes the working code, so we do not write SQL. |
| JPA or Hibernate? | JPA is the standard for mapping objects to tables. Hibernate is the library we use that implements it and creates the SQL. |
| What does `ddl-auto=update` do? | Creates missing tables and columns when the app starts. A real system would use migrations. |
| Why layers (controller, service, repository)? | Each has one job, so the rules can be tested without the web or the database. |
| What is dependency injection? | A class asks for what it needs in its constructor, and Spring supplies it. |
