package com.freya02.bot.versioning.jitpack.jdafork

import java.io.IOException

class ProcessException(val exitCode: Int, val errorOutput: String, message: String) : IOException("(exit code: $exitCode) $message")