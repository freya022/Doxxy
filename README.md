# Doxxy
A Discord bot to display the javadocs of [BotCommands](https://github.com/freya022/BotCommands), [JDA](https://github.com/DV8FromTheWorld/JDA) and Java 17

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
Contributions are welcome, here are the prerequisites in order to run the bot, so you can test your stuff.

### Requirements
* Java 17
* A PostgreSQL database, a recent enough version (11+) should be enough.

### Setting up
1. Have a `Doxxy` folder in a `Bots` folder, in your user directory, this is where the bot will put its files. The corresponding `Path` would be: 
   ```kt
   Path(System.getProperty("user.home"), "Bots", "Doxxy")
   ```
2. Set up the tables with [this script](https://github.com/freya022/BotCommands/blob/UNSTABLE-3.0.0/sql/CreateDatabase.sql) and [this one](https://github.com/freya022/Doxxy/blob/master/sql/CreateDatabase.sql).
3. Have a valid `Test_Config.json` in the project root, to which you can find a template [here](Config_template.json).
4. When running the bot, add this to the VM options: `--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED`
5. ???
6. Profit

### Database changes
If you change something to the database, please write a migration script in the [sql folder](sql).
* You must first bump the version of the bot in the [`pom.xml`](pom.xml), just change the minor version. 
* You can then copy the `pom.xml`'s version into `DatabaseSource.kt` and [`CreateDatabase.sql`](sql/CreateDatabase.sql)
* The name of the migration script must be: `vMajor.Minor__Short_Description.sql`, you can take [this template](sql/vMajor.Minor__Short_Description.sql).
  * For example, `v2.0__Doc_Mentions.sql`
* Make the modifications to the [`CreateDatabase.sql`](sql/CreateDatabase.sql) script as well
  * This means that, in theory, applying the very first creation script + all the migration scripts should give the same tables.

### Updating the database
If the bot says the database is outdated, you can find the migration scripts in the [sql folder](sql)
