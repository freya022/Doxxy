package com.freya02.bot.docs.metadata

import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import io.github.classgraph.MethodInfo
import mu.KotlinLogging
import java.nio.file.Path

class ImplementationMetadata private constructor(classpath: List<Path>) {
    val subclassesMap: Map<ClassInfo, List<ClassInfo>>
    val classToMethodImplementations: MutableMap<ClassInfo, MutableSet<MethodInfo>> = hashMapOf()

    init {
        ClassGraph()
            .overrideClasspath(classpath)
            .acceptPackages("net.dv8tion.jda")
            .enableClassInfo()
            .enableMethodInfo()
            .ignoreClassVisibility()
            .disableRuntimeInvisibleAnnotations()
            .disableNestedJarScanning()
            .scan()
            .use { res ->
                subclassesMap = res.allClasses.associateWith { (it.subclasses + it.classesImplementing) }.filterValues { it.isNotEmpty() }

                res.allClasses.forEach { superClass ->
                    val overrides = superClass.declaredMethodInfo.flatMap { superMethod ->
                        (superClass.subclasses + superClass.classesImplementing).mapNotNull { subClass ->
                            subClass.declaredMethodInfo.find { it.isSameMethodSignature(superMethod) }
                        }
                    }

                    if (overrides.isNotEmpty()) {
                        classToMethodImplementations
                            .computeIfAbsent(superClass) { hashSetOf() }
                            .addAll(overrides)
                    }
                }
            }
    }

    private fun MethodInfo.isSameMethodSignature(superMethod: MethodInfo): Boolean {
        return typeDescriptorStr == superMethod.typeDescriptorStr && name == superMethod.name
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        fun fromClasspath(classpath: List<Path>): ImplementationMetadata {
            return ImplementationMetadata(classpath)
        }
    }
}