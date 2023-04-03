package com.freya02.bot.docs.metadata.parser

import com.freya02.bot.docs.metadata.data.FieldMetadata
import com.freya02.bot.docs.metadata.data.MethodMetadata

typealias PackageName = String
typealias ClassName = String
typealias FullSimpleClassName = String
typealias ResolvedClassesList = MutableMap<FullSimpleClassName, FullSimpleClassName>
typealias MethodMetadataMap = MutableMap<String, MutableList<MethodMetadata>>
typealias FieldMetadataMap = MutableMap<String, FieldMetadata>