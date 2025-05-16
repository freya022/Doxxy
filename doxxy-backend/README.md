# Doxxy - Backend module

Module for the backend.

## Notable libraries

- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) — Data serialization library for many formats
- Spring Boot Starter - Data JPA — Opinionated configuration of Spring Framework, for the JPA module
- Spring Boot Starter - Web — Opinionated configuration of Spring Framework, for the Web MVC/REST module
- [Flyway](https://github.com/flyway/flyway) — Database migration

## Contributing

See the [`Contributing` section of the project root](../README.md#contributing) first.

### Running

Run the main class at [`src/main/kotlin/dev/freya02/doxxy/backend/BackendApplication.kt`](./src/main/kotlin/dev/freya02/doxxy/backend/BackendApplication.kt)

### Making database changes
When changing the schema, please write a migration script in [`src/main/resources/db/migration`](./src/main/resources/db/migration),
with Flyway's naming scheme `V[Major].[Minor].[YYYY].[MM].[DD]__Extended_Description.sql`.