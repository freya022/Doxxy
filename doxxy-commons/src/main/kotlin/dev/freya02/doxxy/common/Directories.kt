package dev.freya02.doxxy.common

import java.nio.file.Path
import kotlin.io.path.Path

object Directories {

    val javadocs = Path(Env["JAVADOCS_DIRECTORY"])
    val jdaFork: Path = Path(Env["JDA-FORK_DIRECTORY"])
    val pageCache: Path = Path(Env["PAGE_CACHE_DIRECTORY"])
}