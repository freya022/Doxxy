package com.freya02.bot.versioning

enum class ScriptType(val humanName: String, val folderName: String) {
    DEPENDENCIES("Dependencies", "dependencies_scripts"),
    FULL("Full","build_scripts")
}