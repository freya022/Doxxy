# Doxxy
A Discord bot to help guide users of the JDA library.

## Features
* Search docs (JDA & JDK 17) using patterns:
  * `JDA` -> Class
  * `#createDefault` -> Method from any class
  * `#MAX_FILE_SIZE` -> Constant field from any class
  * `JDA#openPrivateChannelById` -> Method from the `JDA` class
* Sending Maven/Gradle/Gradle KTS build scripts
* Sending Logback configurations
* Getting the latest versions and snapshots
* Sending user-created code examples
* Formatting code
* Tags

## Modules
* [doxxy-commons](doxxy-commons): Common source code used by both the bot and the backend
* [doxxy-bot](doxxy-bot): Main Discord bot
* [doxxy-backend](doxxy-backend): Backend library optionally used by the bot, cannot be run
* [doxxy-backend-app](doxxy-backend-app): Application which runs the backend library

## Contributing
### Requirements
* JDK 17+
* A PostgreSQL 13 or higher database

### Set up
1. Clone repo
2. Copy `config-template` as `config`
3. Delete `logback.xml` as it's configured for production
4. Set values in `config/.env`, values are required unless commented otherwise

### Running
#### Backend
You can run it by:
- Running the main class at [`doxxy-backend-app/src/main/kotlin/io/github/freya022/backend/BackendApplication.kt`](./doxxy-backend-app/src/main/kotlin/io/github/freya022/backend/BackendApplication.kt)
- Or, running `mvn -pl doxxy-backend-app -am spring-boot:run`

#### Bot
You can run it by:
- Running the main class at [`doxxy-bot/src/main/kotlin/com/freya02/bot/Main.kt`](./doxxy-bot/src/main/kotlin/com/freya02/bot/Main.kt)
- Or, running `mvn -pl doxxy-bot -am compile exec:exec`

### Making database changes
When changing the schema, please write a migration script,
with Flyway's naming scheme `V[Major].[Minor].[YYYY].[MM].[DD]__Extended_Description.sql`.

Backend migrations can be found in [`doxxy-backend/src/main/resources/db/migration`](./doxxy-backend/src/main/resources/db/migration).
Bot migrations can be found in [`doxxy-bot/src/main/resources/doxxy_database_scripts`](./doxxy-bot/src/main/resources/doxxy_database_scripts).

## Deployment
1. Clone repo
2. Copy `config-template` as `config`
3. Change profile from `dev` to `prod` in `config/application.yaml`
4. Set values in `config/.env`, values are required unless commented otherwise
5. Run `mvn clean package jib:dockerBuild` to create the docker image
6. Run `docker compose up -d` to start the application stack