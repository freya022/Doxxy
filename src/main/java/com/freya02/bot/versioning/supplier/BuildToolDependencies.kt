package com.freya02.bot.versioning.supplier

interface BuildToolDependencies {
    val bcDependencyFormatString: String
    val jda4DependencyFormatString: String
    val jda5DependencyFormatString: String
    val jdaJitpackDependencyFormatString: String
}