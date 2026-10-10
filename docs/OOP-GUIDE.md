# OOP guide: where each concept appears

| Concept | Where in the code | One-line explanation |
|---|---|---|
| **Class and object** | `User`, `Item`, `Claim` in `model/` | A class is a blueprint; each database row becomes an object. |
| **Encapsulation** | Private fields with getters in every entity; `private final` fields in services | Data is hidden and changed only through controlled methods. |
| **Constructor overloading** | `Item` (with/without report date), `User` (with/without googleId) | Same class, several ways to create it (compile-time polymorphism). |
| **Composition (has-a)** | `Item` has a `User` reporter; `Claim` has an `Item` and a claimant `User`; services have repositories | Objects are built from other objects. |
| **Enums with behaviour** | `ItemStatus.isActive()`, `Role`, `ItemType`, `ClaimStatus` | A fixed set of values, which can carry data and methods. |
| **Inheritance** | `ResourceNotFoundException`, `ForbiddenException`, `ConflictException`, `UnauthorizedException`, `BadRequestException` extend `RuntimeException`; repositories extend `JpaRepository` | A child class reuses and extends a parent. |
| **Abstraction / interfaces** | `PasswordEncoder`, `JwtEncoder`, `JwtDecoder`, `JpaRepository`, `CommandLineRunner` | Code depends on what something does, not how. |
| **Polymorphism (runtime)** | `PasswordEncoder` is a BCrypt object at runtime; `JsonSecurityErrorHandler` implements two interfaces; `@Override` in `AdminBootstrap.run` | The same interface call runs different implementations. |
| **Dependency injection** | Every controller/service constructor | Spring creates objects and passes them in; tests pass fakes. |
| **Immutable data objects** | DTO `record`s in `dto/` | Short, immutable classes for request/response data. |
| **Exception handling** | `exception/` package, `GlobalExceptionHandler` | Errors are thrown as objects and turned into JSON in one place. |
| **Separation of concerns** | controller -> service -> repository | HTTP, rules, and data access each live in one layer. |
| **Generics** | `JpaRepository<Item, Long>`, `List<ItemResponse>`, `Specification<Item>` | Type-safe reusable code. |
| **Static factory method** | `ItemResponse.from(item)`, `ClaimResponse.from(claim)` | Converts entity to DTO in one named place. |

## Design choices to mention
- **DTOs instead of entities in the API:** a client cannot set the reporter, status, role or password hash.
- **No Lombok:** getters and constructors are written out so every line is visible.
- **Constructor injection** (not field `@Autowired`): dependencies are `final` and tests are easy.
- **Transactions + row lock:** claim and item status change together; two admins cannot approve two claims for one item.
