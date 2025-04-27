package dev.freya02.doxxy.bot.pagination

import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.pagination.paginator.AbstractPaginator
import io.github.freya022.botcommands.api.pagination.paginator.AbstractPaginatorBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

class CodePaginatorBuilder(
    context: BContext,
    val messageWriter: CodePaginator.(MessageCreateBuilder) -> Unit
) : AbstractPaginatorBuilder<CodePaginatorBuilder, CodePaginator>(context) {
    override fun build() = CodePaginator(context, this)
}

class CodePaginator(
    context: BContext,
    builder: CodePaginatorBuilder
) : AbstractPaginator<CodePaginator>(context, builder) {
    public override var maxPages: Int = 0 // This is set when the code is formatted

    private val messageWriter = builder.messageWriter

    override fun writeMessage(builder: MessageCreateBuilder) {
        super.writeMessage(builder)
        messageWriter(builder)
    }
}