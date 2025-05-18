package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.SerializableJavadocClass.SerializableSeeAlso
import dev.freya02.doxxy.docs.declarations.*
import dev.freya02.doxxy.docs.sections.DocDetail
import dev.freya02.doxxy.docs.sections.SeeAlso
import dev.freya02.doxxy.docs.sections.SeeAlso.TargetType
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.jsoup.nodes.Element
import org.junit.jupiter.api.BeforeAll
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.fail

private object ElementSerializer : KSerializer<Element> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Element) {
        encoder.encodeString(value.outerHtml())
    }

    override fun deserialize(decoder: Decoder) = error("Should not be deserialized")
}

private object JavadocElementSerializer : KSerializer<JavadocElement> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: JavadocElement) {
        encoder.encodeString(value.targetElement.outerHtml())
    }

    override fun deserialize(decoder: Decoder) = error("Should not be deserialized")
}

private typealias SerializableElement = @Serializable(with = ElementSerializer::class) Element
private typealias SerializableJavadocElement = @Serializable(with = JavadocElementSerializer::class) JavadocElement

@Serializable
private data class SerializableJavadocClass(
    val classNameFqcn: String,
    val descriptionElements: List<SerializableJavadocElement>,
    val deprecationElement: SerializableElement?,
    val details: Map<DocDetail.Type, List<SerializableJavadocElement>>,
    val seeAlso: SerializableSeeAlso?,
    val methods: Map<String, SerializableJavadocMethod>,
    val fields: Map<String, SerializableJavadocField>,
) {

    @Serializable
    data class SerializableSeeAlso(
        val references: List<SerializableSeeAlsoReferences>,
    ) {

        @Serializable
        data class SerializableSeeAlsoReferences(
            val text: String,
            val link: String,
            val targetType: TargetType,
            val fullSignature: String?,
        )
    }
}

@Serializable
private data class SerializableJavadocMethod(
    val elementId: String,
    val methodAnnotations: String?,
    val methodName: String,
    val methodSignature: String,
    val methodParameters: SerializableParameters?,
    val methodReturnType: String,
    val isStatic: Boolean,
    val descriptionElements: List<SerializableJavadocElement>,
    val deprecationElement: SerializableJavadocElement?,
    val details: Map<DocDetail.Type, List<SerializableJavadocElement>>,
    val seeAlso: SerializableSeeAlso?,
) {

    @Serializable
    data class SerializableParameters(
        val originalText: String,
        val parameters: List<SerializableParameter>,
    ) {

        @Serializable
        data class SerializableParameter(
            val annotations: Set<String>,
            val type: String,
            val simpleType: String,
            val name: String
        )
    }
}

@Serializable
private data class SerializableJavadocField(
    val elementId: String,
    val fieldName: String,
    val fieldType: String,
    val fieldValue: String?,
    val descriptionElements: List<SerializableJavadocElement>,
    val deprecationElement: SerializableJavadocElement?,
    val modifiers: String,
)

object IntegrationTests {

    private val sources = JavadocSources(listOf(JDA_SOURCE, JDK_SOURCE))

    @BeforeAll
    @JvmStatic
    fun setup() {
        PageCache[JDA_SOURCE].clearCache()

        embeddedServer(Netty, host = JDA_HOST, port = JDA_PORT) {
            routing {
                staticZip(JDA_PATH, "", getResourcePath("/JDA-javadoc.zip"), index = null)
            }
        }.start()
    }

    @Test
    @OptIn(ExperimentalSerializationApi::class)
    fun `Check regressions from snapshots`() {
        val globalSession = GlobalJavadocSession(sources)
        val moduleSession = globalSession.retrieveSession(JDA_SOURCE)

        val classes = runBlocking {
            moduleSession
                .classesAsFlow()
                .buffer()
                .toList()
        }

        val tmpZip = createTempFile(suffix = ".zip")
        tmpZip.outputStream().buffered().let(::ZipOutputStream).use { zos ->
            val json = Json {
                prettyPrint = true
            }

            classes
                .sortedBy { it.classNameFqcn }
                .forEach { javadocClass ->
                    val entry = ZipEntry("${javadocClass.packageName.replace('.', '/')}/${javadocClass.className}.json")
                    entry.time = 0 // Reproducible entries
                    zos.putNextEntry(entry)

                    json.encodeToStream(javadocClass.toSerializable(), zos)
                    zos.closeEntry()
                }
        }

        val expectedSnapshotPath = Path("snapshots", "JDA.zip")
        if (expectedSnapshotPath.exists()) {
            fun failTest(): Nothing {
                tmpZip.moveTo(expectedSnapshotPath, overwrite = true)
                fail("Snapshots differ and has been overwritten, check the diff using your editor")
            }

            if (expectedSnapshotPath.fileSize() != tmpZip.fileSize()) failTest()

            val expectedSnapshotBytes = expectedSnapshotPath.readBytes()
            val actualSnapshotBytes = tmpZip.readBytes()

            if (!(expectedSnapshotBytes contentEquals actualSnapshotBytes)) failTest()
        } else {
            tmpZip.moveTo(expectedSnapshotPath)
        }
    }

    private fun JavadocClass.toSerializable() = SerializableJavadocClass(
        classNameFqcn,
        descriptionElements,
        deprecationElement?.targetElement,
        detailToElementsMap.map,
        seeAlso?.toSerializable(),
        methods.mapValues { (_, method) -> method.toSerializable() },
        fields.mapValues { (_, field) -> field.toSerializable() },
    )

    private fun SeeAlso.toSerializable() = SerializableSeeAlso(
        references.map { it.toSerializable() },
    )

    private fun SeeAlso.SeeAlsoReference.toSerializable() = SerializableSeeAlso.SerializableSeeAlsoReferences(
        text,
        link,
        targetType,
        fullSignature,
    )

    private fun JavadocMethod.toSerializable() = SerializableJavadocMethod(
        elementId,
        methodAnnotations,
        methodName,
        methodSignature,
        methodParameters?.toSerializable(),
        methodReturnType,
        isStatic,
        descriptionElements,
        deprecationElement,
        detailToElementsMap.map,
        seeAlso?.toSerializable(),
    )

    private fun MethodDocParameters.toSerializable() = SerializableJavadocMethod.SerializableParameters(
        asString,
        parameters.map { it.toSerializable() },
    )

    private fun MethodDocParameter.toSerializable() = SerializableJavadocMethod.SerializableParameters.SerializableParameter(
        annotations,
        type,
        simpleType,
        name,
    )

    private fun JavadocField.toSerializable() = SerializableJavadocField(
        elementId,
        fieldName,
        fieldType,
        fieldValue,
        descriptionElements,
        deprecationElement,
        modifiers,
    )
}