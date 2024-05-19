package com.freya02.bot.versioning

//see https://central.sonatype.org/search/rest-api-guide/
private const val MAVEN_URL_FORMAT = "https://search.maven.org/remotecontent?filepath=%s/%s/%s/%s-%s%s"
private const val JITPACK_URL_FORMAT = "https://jitpack.io/%s/%s/%s/%s-%s%s"

data class ArtifactInfo(val groupId: String, val artifactId: String, val version: String) {
    fun toMavenUrl(fileType: FileType): String {
        return MAVEN_URL_FORMAT.format(
            groupId.replace('.', '/'),
            artifactId,
            version,
            artifactId,
            version,
            fileType.fileSuffix
        )
    }

    fun toJitpackUrl(fileType: FileType): String {
        return JITPACK_URL_FORMAT.format(
            groupId.replace('.', '/'),
            artifactId,
            version,
            artifactId,
            version,
            fileType.fileSuffix
        )
    }

    override fun toString(): String {
        return "ArtifactInfo[" +
                "groupId=" + groupId + ", " +
                "artifactId=" + artifactId + ", " +
                "version=" + version + ']'
    }
}