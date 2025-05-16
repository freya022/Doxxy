# Doxxy - Bot module

Module for the main Discord bot.

## Features used
- Dependency injection (duh)
  - Conditions
- Application commands (slash, message context)
  - Declaration filters
  - Annotated and declarative commands
- Text commands
- Components
- Database
- Parameter (arguments) resolvers
- Pagination

## Notable libraries

- [JDA](https://github.com/discord-jda/JDA) — Java wrapper for the popular chat & VOIP service: Discord
  - [jda-ktx](https://github.com/MinnDevelopment/jda-ktx) — Kotlin extensions for JDA
  - [BotCommands](https://github.com/freya022/BotCommands) — Kotlin-first JDA framework to create Discord automations 
- [Ktor](https://ktor.io/) — Framework for building asynchronous web servers and clients
- [Flyway](https://github.com/flyway/flyway) — Database migration
- [JavaParser](https://github.com/javaparser/javaparser) — Java 1–21 parser as Abstract Syntax Trees with resolution capabilities
- [palantir-java-format](https://github.com/palantir/palantir-java-format) — 120-characters Java formatter
- [remark-java](https://github.com/freya022/remark-java) — Converts Javadoc's HTML to Markdown

## Contributing

See the [`Contributing` section of the project root](../README.md#contributing) first.

### Running

Run the main class at [`src/main/kotlin/dev/freya02/doxxy/bot/Main.kt`](src/main/kotlin/dev/freya02/doxxy/bot/Main.kt),
and add the following VM options in the run configuration:
`--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED`

### Making database changes
When changing the schema, please write a migration script in [`src/main/resources/doxxy_database_scripts`](./src/main/resources/doxxy_database_scripts),
with Flyway's naming scheme `V[Major].[Minor].[YYYY].[MM].[DD]__Extended_Description.sql`.