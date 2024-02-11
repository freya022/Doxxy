package com.freya02.bot.versioning.jitpack.pullupdater

import java.io.IOException

class ProcessException(val exitCode: Int, val errorOutput: String, message: String) : IOException("(exit code: $exitCode) $message")