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
    val language: String,
    val title: String,
    val content: String,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "example_id", nullable = false)
    val targets: List<ExampleTarget>
) : AbstractEntity()

@Entity
class ExampleTarget(
    val target: String
) : AbstractEntity()