package com.freya02.bot.docs.metadata

import com.freya02.bot.docs.metadata.data.FieldMetadata
import com.freya02.bot.docs.metadata.data.MethodMetadata
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType

typealias PackageName = String
typealias ClassName = String
typealias FullSimpleClassName = String
typealias ResolvedClassesList = MutableMap<FullSimpleClassName, FullSimpleClassName>
typealias MethodMetadataMap = MutableMap<String, MutableList<MethodMetadata>>
typealias FieldMetadataMap = MutableMap<String, FieldMetadata>

typealias ResolvedClass = ResolvedReferenceType
typealias ResolvedMethod = ResolvedMethodDeclaration