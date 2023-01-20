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
1. Have a `Doxxy` folder in a `Bots` folder, in your user directory. The corresponding `Path` would be: 
   ```kt
   Path(System.getProperty("user.home"), "Bots", "Doxxy")
   ```
2. Have a PostgreSQL database, a recent enough version (11+) should be enough.
3. Setup the tables with [this script](https://github.com/freya022/BotCommands/blob/UNSTABLE-3.0.0/sql/CreateDatabase.sql) and [this one](https://github.com/freya022/Doxxy/blob/master/sql/CreateDatabase.sql).
4. Have a valid `Config.json`, to which you can find a template [here](Config_template.json).
5. ???
6. Profit