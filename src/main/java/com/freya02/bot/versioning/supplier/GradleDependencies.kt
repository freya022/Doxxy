package com.freya02.bot.versioning.supplier

object GradleDependencies : BuildToolDependencies {
    override val bcDependencyFormatString: String = """
        repositories {
            mavenCentral()
            maven { url 'https://jitpack.io' }
        }
        
        dependencies {
            implementation '%s:%s:%s'
            implementation '%s:%s:%s'
        }
    """.trimIndent()

    override val jda4DependencyFormatString: String = """
        repositories {
            mavenCentral()
            maven {
                name 'm2-dv8tion'
                url 'https://m2.dv8tion.net/releases'
            }
        }
        
        dependencies {
            implementation '%s:%s:%s'
        }
    """.trimIndent()

    override val jda5DependencyFormatString: String = """
        dependencies {
            implementation '%s:%s:%s'
        }
    """.trimIndent()

    override val jdaJitpackDependencyFormatString: String = """
        repositories {
            mavenCentral()
            maven { url 'https://jitpack.io' }
        }
        
        dependencies {
            implementation '%s:%s:%s'
        }
    """.trimIndent()
}