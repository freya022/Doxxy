package com.freya02.docs2

import com.freya02.bot.Config.Companion.getConfig
import com.freya02.bot.db.Database
import com.freya02.bot.docs.DocIndexMap
import com.freya02.bot.versioning.Versions

fun main() {
    val config = getConfig()
    val database = Database(config)

    val map = DocIndexMap(database)
    Versions(map).checkLatestBCVersion(null)

    println()
}