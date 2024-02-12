# Doxxy bot module
Maven module of the Discord bot, it may optionally use the backend.

## Running
### Requirements
* JDK 17+
* A PostgreSQL database, a recent enough version (11+) should be enough.

### Setting up
1. Copy the `config-template` as `dev-config`.
2. Delete the "logback.xml" as it is only for production.
3. Set the values in `config.json`.
4. Run the bot **in the `doxxy-bot` working directory**:
   * Using `mvn compile exec:exec`, or,
   * Make a run configuration with the VM options found in `exec-maven-plugin`.
5. ???
6. Profit

### Database changes
If you change something to the database, 
please write a migration script [here](src/main/resources/doxxy_database_scripts),
with [Flyway's naming scheme](https://documentation.red-gate.com/fd/migrations-184127470.html):

`V[Major].[Minor].[YYYY].[MM].[DD]__Extended_Description.sql`.