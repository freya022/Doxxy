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
* [doxxy-backend](doxxy-backend): Backend optionally used by the bot
* [doxxy-docs](doxxy-docs): Parses Javadoc pages

## Contributing
### Requirements
* JDK 24+
  * [Configure your IDE](https://docs.gradle.org/current/userguide/toolchains.html#sec:ide-settings-toolchain) to run Gradle with it
* A PostgreSQL 13 or higher database

### Set up
1. Clone repo
2. Copy `config-template` as `config`
3. Delete `logback.xml` as it's configured for production
4. Copy `.env.example` as `.env` and set the values, values are required unless commented otherwise

Then follow the instructions of the module you want to contribute to.

## Deployment
1. Clone repo
2. Copy `config-template` as `config`
3. Change profile from `dev` to `prod` in `config/application.yaml`
4. Copy `.env.example` as `.env` and set the values, values are required unless commented otherwise
5. Generate the base docker image with `docker build --tag doxxy-bot-eclipse-temurin:jdk-24 ./doxxy-bot`
6. Run `mvn clean package jib:dockerBuild` to create the docker image
7. Run `docker compose up -d` to start the application stack