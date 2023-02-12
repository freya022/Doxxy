package com.freya02.bot.pagination

import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.data.InteractionConstraints
import com.freya02.botcommands.api.pagination.PaginatorSupplier
import com.freya02.botcommands.api.pagination.TimeoutInfo
import com.freya02.botcommands.api.pagination.paginator.BasicPaginator
import com.freya02.botcommands.api.pagination.paginator.BasicPaginatorBuilder
import com.freya02.botcommands.api.utils.ButtonContent

class CodePaginatorBuilder(
    componentsService: Components
) : BasicPaginatorBuilder<CodePaginatorBuilder, CodePaginator>(componentsService) {
    override fun build() =
        CodePaginator(
            componentsService,
            constraints,
            timeout,
            _maxPages = 0,
            paginatorSupplier,
            hasDeleteButton,
            firstContent,
            previousContent,
            nextContent,
            lastContent,
            deleteContent
        )
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
    public override fun setMaxPages(maxPages: Int) {
        super.setMaxPages(maxPages)
    }

    override fun onPostGet() {
        super.onPostGet()
        messageBuilder.setEmbeds()
    }
}