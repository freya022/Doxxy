package dev.freya02.doxxy.bot.docs.metadata.parser

import dev.freya02.doxxy.bot.docs.metadata.data.FieldMetadata
import dev.freya02.doxxy.bot.docs.metadata.data.MethodMetadata

typealias PackageName = String
typealias ClassName = String
typealias Signature = String
typealias FullSimpleClassName = String
typealias TopSimpleClassName = String
typealias MethodMetadataMap = MutableMap<String, MutableList<MethodMetadata>>
typealias FieldMetadataMap = MutableMap<String, FieldMetadata>