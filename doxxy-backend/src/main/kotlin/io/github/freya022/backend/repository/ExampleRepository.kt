package io.github.freya022.backend.repository

import io.github.freya022.backend.entity.Example
import io.github.freya022.backend.repository.dto.ExampleSearchResultDTO
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Repository
interface ExampleRepository : JpaRepository<Example, Int> {
    @Modifying
    @Query("truncate example cascade", nativeQuery = true)
    @Transactional(propagation = Propagation.MANDATORY)
    fun removeAll()

    // JDA#updateCommands() like JDA#updateCommand%
    // Guild#getRoleById(long) like Guild#getRoleById%
    // Guild#getRoleById(String) like Guild#getRoleById%
    // Guild#getRoleById(String) like Guild#getRoleById(String)%
    @Query("from Example example join example.targets target where :target like target.target || '%'")
    fun findByTarget(target: String): List<ExampleSearchResultDTO>

    @Query("select title, library from example where title % :title order by similarity(title, :title) desc", nativeQuery = true)
    fun searchByTitle(title: String): List<ExampleSearchResultDTO>

    @Query("select title, library from example order by title", nativeQuery = true)
    fun findAllAsSearchResults(): List<ExampleSearchResultDTO>

    fun findByTitle(title: String): Example?
}