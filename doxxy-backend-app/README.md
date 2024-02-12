# Doxxy backend module
Maven module of the backend application, it isn't required for deployment.

## Contributing
Sources files are in [doxxy-backend](../doxxy-backend), 
this module is only an application which runs the backend library.

## Running
### Requirements
* JDK 17+
* A PostgreSQL database, a recent enough version (11+) should be enough.

### Setting up
1. Copy the `application.properties.example` as `application.properties` and set the values.
2. Run the backend **in the `doxxy-backend-app` working directory**:
   * Using `mvn spring-boot:run`, or,
   * Make a Spring Boot run configuration on IntelliJ.
3. ???
4. Profit

### Database changes
If you change something to the database, 
please write a migration script [here](../doxxy-backend/src/main/resources/db/migration),
with [Flyway's naming scheme](https://documentation.red-gate.com/fd/migrations-184127470.html):

`V[Major].[Minor].[YYYY].[MM].[DD]__Extended_Description.sql`.