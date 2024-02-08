package io.github.freya022.backend.entity

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
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "example_id", nullable = false)
    val contents: List<ExampleContent>,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "example_id", nullable = false)
    val targets: List<ExampleTarget>
) : AbstractEntity()

@Entity
class ExampleContent(
    val language: String,
    val content: String
) : AbstractEntity()

@Entity
class ExampleTarget(
    val target: String
) : AbstractEntity()