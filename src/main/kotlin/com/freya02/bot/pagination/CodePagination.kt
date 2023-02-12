package com.freya02.bot.pagination

import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.data.InteractionConstraints
import com.freya02.botcommands.api.pagination.PaginatorSupplier
import com.freya02.botcommands.api.pagination.TimeoutInfo
import com.freya02.botcommands.api.pagination.paginator.BasicPaginator
import com.freya02.botcommands.api.pagination.paginator.BasicPaginatorBuilder
import com.freya02.botcommands.api.utils.ButtonContent
import dev.minn.jda.ktx.messages.Embed

class CodePaginatorBuilder(
    componentsService: Components,
    private val blocks: List<String>
) : BasicPaginatorBuilder<CodePaginatorBuilder, CodePaginator>(componentsService) {
    override fun build() =
        CodePaginator(
            componentsService,
            constraints,
            timeout,
            _maxPages = blocks.size,
            supplier = { _, editBuilder, _, page ->
                emptyEmbed.also { editBuilder.setContent("```java\n${blocks[page]}```") }
            },
            hasDeleteButton,
            firstContent,
            previousContent,
            nextContent,
            lastContent,
            deleteContent
        )

    @Deprecated("Already handled", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith(""))
    override fun setPaginatorSupplier(paginatorSupplier: PaginatorSupplier<CodePaginator>): CodePaginatorBuilder {
        return super.setPaginatorSupplier(paginatorSupplier)
    }

    private companion object {
        private val emptyEmbed = Embed { description = "dummy" }
    }
}

class CodePaginator(
    componentsService: Components,
    constraints: InteractionConstraints?,
    timeout: TimeoutInfo<CodePaginator>?,
    _maxPages: Int,
    supplier: PaginatorSupplier<CodePaginator>?,
    hasDeleteButton: Boolean,
    firstContent: ButtonContent?,
    previousContent: ButtonContent?,
    nextContent: ButtonContent?,
    lastContent: ButtonContent?,
    deleteContent: ButtonContent?
) : BasicPaginator<CodePaginator>(
    componentsService,
    constraints,
    timeout,
    _maxPages,
    supplier,
    hasDeleteButton,
    firstContent,
    previousContent,
    nextContent,
    lastContent,
    deleteContent
) {
    override fun onPostGet() {
        super.onPostGet()
        messageBuilder.setEmbeds()
    }
}