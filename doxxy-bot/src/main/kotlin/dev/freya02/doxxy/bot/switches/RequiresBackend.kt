package dev.freya02.doxxy.bot.switches

import dev.freya02.doxxy.bot.config.BackendConfig
import io.github.freya022.botcommands.api.core.service.CustomConditionChecker
import io.github.freya022.botcommands.api.core.service.ServiceContainer
import io.github.freya022.botcommands.api.core.service.annotations.Condition
import io.github.freya022.botcommands.api.core.service.getService

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Condition(BackendChecker::class, fail = false)
annotation class RequiresBackend

object BackendChecker : CustomConditionChecker<RequiresBackend> {
    override val annotationType: Class<RequiresBackend>
        get() = RequiresBackend::class.java

    override fun checkServiceAvailability(
        serviceContainer: ServiceContainer,
        checkedClass: Class<*>,
        annotation: RequiresBackend
    ): String? {
        if (!serviceContainer.getService<BackendConfig>().enable) {
            return "Backend is disabled"
        }

        return null
    }
}