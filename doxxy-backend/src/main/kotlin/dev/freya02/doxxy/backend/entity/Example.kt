package dev.freya02.doxxy.backend.entity

import jakarta.persistence.*

@MappedSuperclass
abstract class AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
}

@Entity
class Example(
    val title: String,
    val library: String,
    @OneToMany(mappedBy = "example", cascade = [CascadeType.ALL], orphanRemoval = true)
    val contents: List<ExampleContent>,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "example_id", nullable = false)
    val targets: List<ExampleTarget>
) : AbstractEntity()

@Entity
class ExampleContent(
    val language: String,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "example_content_id", nullable = false)
    val parts: List<ExampleContentPart>
) : AbstractEntity() {
    @ManyToOne
    lateinit var example: Example
}

@Entity
class ExampleContentPart(
    val label: String,
    val emoji: String?,
    val description: String?,
    val content: String
) : AbstractEntity()

@Entity
class ExampleTarget(
    val target: String
) : AbstractEntity()