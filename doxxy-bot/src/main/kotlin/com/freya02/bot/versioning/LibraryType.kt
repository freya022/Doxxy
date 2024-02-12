package com.freya02.bot.versioning

import com.freya02.bot.utils.Utils.isBCGuild
import com.freya02.bot.versioning.maven.RepoType
import net.dv8tion.jda.api.entities.Guild

enum class LibraryType(
    val displayString: String,
    val githubOwnerName: String, val githubRepoName: String,
    val repoType: RepoType = RepoType.MAVEN, val mavenGroupId: String, val mavenArtifactId: String
) {
    JDA(
        displayString = "JDA",
        githubOwnerName = "discord-jda", githubRepoName = "JDA",
        mavenGroupId = "net.dv8tion", mavenArtifactId = "JDA"
    ),
    JDA_KTX(
        displayString = "JDA-KTX",
        githubOwnerName = "MinnDevelopment", githubRepoName = "jda-ktx",
        mavenGroupId = "club.minnced", mavenArtifactId = "jda-ktx"
    ),
    BOT_COMMANDS(
        displayString = "BotCommands",
        githubOwnerName = "freya022", githubRepoName = "BotCommands",
        mavenGroupId = "io.github.freya022", mavenArtifactId = "BotCommands"
    ),
    LAVA_PLAYER(
        displayString = "LavaPlayer",
        githubOwnerName = "lavalink-devs", githubRepoName = "lavaplayer",
        mavenGroupId = "dev.arbjerg", mavenArtifactId = "lavaplayer"
    ),
    ;

    companion object {
        fun getDefaultLibrary(guild: Guild): LibraryType {
            return if (guild.isBCGuild()) BOT_COMMANDS else JDA
        }
    }
}
