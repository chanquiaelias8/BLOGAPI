# 01 — Modelo de dominio

Estado: **aprobado e implementado**. Build verde (`mvnw test`, Boot 3.5.16 / Java 17).

## Decisiones confirmadas

| # | Decisión | Resolución |
|---|---|---|
| 1 | Stack | Spring Boot 3.5.16, Java 17, Spring Security 6.5 |
| 2 | Identificadores | `UUID` como PK de todas las entidades (sin `Long` interno) |
| 3a | Alcance de `POST_UPDATE` para AUTHOR | Solo sus propios posts, vía ownership check en `@PreAuthorize` |
| 3b | Visibilidad de borradores | Anónimo/USER: solo `PUBLISHED`. AUTHOR: + sus `DRAFT`. ADMIN: todo |
| 3c | Borrado para AUTHOR | No tiene `POST_DELETE`; retira contenido pasándolo a `ARCHIVED` |
| 3d | Registro | `/auth/register` público, ignora roles del body. ADMIN se bootstrapea aparte |
| 3e | Roles al registrarse | Todo registrado recibe **USER + AUTHOR** y su perfil `Author` automáticamente |

## Diagrama de clases

```mermaid
classDiagram
    direction LR

    class User {
        +UUID id
        +String username
        +String email
        +String password
        +boolean enabled
        +boolean accountLocked
        +AuthProvider provider
        +String providerId
        +Long version
        +Set~Role~ roles
    }
    class Role {
        +UUID id
        +String name
        +String description
        +Set~Permission~ permissions
    }
    class Permission {
        +UUID id
        +String name
        +String description
    }
    class Author {
        +UUID id
        +String nombre
        +String bio
        +User user
        +List~Post~ posts
    }
    class Post {
        +UUID id
        +String titulo
        +String slug
        +String contenido
        +PostStatus status
        +Instant publishedAt
        +Long version
        +Author author
    }
    class Auditable {
        <<abstract>>
        +Instant createdAt
        +Instant updatedAt
    }
    class AuthProvider {
        <<enumeration>>
        LOCAL
        GOOGLE
    }
    class PostStatus {
        <<enumeration>>
        DRAFT
        PUBLISHED
        ARCHIVED
    }

    Auditable <|-- User
    Auditable <|-- Author
    Auditable <|-- Post
    User "1..*" -- "1..*" Role : user_roles
    Role "1..*" -- "1..*" Permission : role_permissions
    Author "0..1" --> "1" User : perfil de
    Author "1" -- "0..*" Post : escribe
    User ..> AuthProvider
    Post ..> PostStatus
```

## Catálogo de autorización (sembrado por `AuthorizationDataSeeder`)

| Permiso | ADMIN | AUTHOR | USER |
|---|:--:|:--:|:--:|
| `POST_CREATE` | X | X | |
| `POST_READ` | X | X | X |
| `POST_UPDATE` | X | X (propios) | |
| `POST_DELETE` | X | | |
| `AUTHOR_CREATE` | X | | |
| `AUTHOR_READ` | X | X | X |
| `AUTHOR_UPDATE` | X | X (propio) | |
| `AUTHOR_DELETE` | X | | |
| `USER_CREATE` | X | | |
| `USER_READ` | X | | |
| `USER_UPDATE` | X | | |
| `USER_DELETE` | X | | |
| `ROLE_MANAGE` | X | | |
| `PERMISSION_MANAGE` | X | | |

Authorities efectivas = `ROLE_<rol>` por cada rol + la unión de los permisos de todos sus roles.
Como toda cuenta registrada es USER+AUTHOR, sus authorities son:
`ROLE_USER, ROLE_AUTHOR, POST_CREATE, POST_READ, POST_UPDATE, AUTHOR_READ, AUTHOR_UPDATE`.

## Pendiente explícito

- Migrar de `ddl-auto: update` a **Flyway** con `ddl-auto: validate` antes del paso de Docker.
- `git init` + repo en GitHub (bonus 11).
