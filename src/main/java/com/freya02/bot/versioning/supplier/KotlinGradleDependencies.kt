package com.freya02.bot.versioning.supplier

object KotlinGradleDependencies : BuildToolDependencies {
    override val bcDependencyFormatString: String = """
        repositories {
            mavenCentral()
            maven("https://jitpack.io")
        }
        
        dependencies {
            implementation("%s:%s:%s")
            implementation("%s:%s:%s")
        }
    """.trimIndent()

    override val jda4DependencyFormatString: String = """
        repositories {
            mavenCentral()
            maven("https://m2.dv8tion.net/releases")
        }
        
        dependencies {
            implementation("%s:%s:%s")
        }
    """.trimIndent()

    override val jda5DependencyFormatString: String = """
        dependencies {
            implementation("%s:%s:%s")
        }
    """.trimIndent()

    override val jdaJitpackDependencyFormatString: String = """
        repositories {
            mavenCentral()
            maven("https://jitpack.io")
        }
        
        dependencies {
            implementation("%s:%s:%s")
        }
    """.trimIndent()
}