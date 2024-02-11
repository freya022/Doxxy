# Doxxy
A Discord bot to display the javadocs of [BotCommands](https://github.com/freya022/BotCommands), [JDA](https://github.com/discord-jda/JDA) and Java 17

## Features
* Searching docs
  * By class name (+ identifier)
  * By method/field name
* Sending Maven/Gradle/Gradle KTS builds scripts
* Sending Logback configurations
* Getting the latest versions on Jitpack
* Formatting code
* Tags
* Reactions on classes recognized in messages

## Contributing
Contributions are welcome, here are the prerequisites to run the bot, so you can test your stuff.

### Requirements
* JDK 17+
* A PostgreSQL database, a recent enough version (11+) should be enough.

### Setting up
1. Copy the `config-template` as `dev-config`.
2. Delete the "logback.xml" as it is only for production.
3. Set the values in `config.json`.
4. Running the bot can be done by running `mvn compile exec:exec`, 
you can also make a run configuration with the VM options found in `exec-maven-plugin`, 
and with `doxxy-bot` as the working directory.
5. ???
6. Profit

### Database changes
If you change something to the database, 
please write a migration script [here](src/main/resources/doxxy_database_scripts),
with [Flyway's naming scheme](https://documentation.red-gate.com/fd/migrations-184127470.html):

`V[Major].[Minor].[YYYY].[MM].[DD]__Extended_Description.sql`.