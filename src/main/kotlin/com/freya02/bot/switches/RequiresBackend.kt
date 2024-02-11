package com.freya02.bot.switches

import com.freya02.bot.config.BackendConfig
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.service.CustomConditionChecker
import io.github.freya022.botcommands.api.core.service.annotations.Condition
import io.github.freya022.botcommands.api.core.service.getService

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Condition(BackendChecker::class, fail = false)
annotation class RequiresBackend

object BackendChecker : CustomConditionChecker<RequiresBackend> {
    override val annotationType: Class<RequiresBackend>
        get() = RequiresBackend::class.java

    override fun checkServiceAvailability(
        context: BContext,
        checkedClass: Class<*>,
        annotation: RequiresBackend
    ): String? {
        if (!context.getService<BackendConfig>().enable) {
            return "Backend is disabled"
        }

        return null
    }
}