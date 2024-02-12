# Doxxy
A Discord bot to display the javadocs of [BotCommands](https://github.com/freya022/BotCommands), 
[JDA](https://github.com/discord-jda/JDA), and JDK 17.

## Features
* Searching docs
  * By class name (+ identifier)
  * By method/field name
* Sending Maven/Gradle/Gradle KTS builds scripts
* Sending Logback configurations
* Getting the latest versions on Jitpack
* Formatting code
* Tags
* Sending code examples

## Modules
* [doxxy-commons](doxxy-commons): Common source code used by both the bot and the backend
* [doxxy-bot](doxxy-bot): Main Discord bot
* [doxxy-backend](doxxy-backend): Backend library optionally used by the bot, cannot be run
* [doxxy-backend-app](doxxy-backend-app): Application which runs the backend library