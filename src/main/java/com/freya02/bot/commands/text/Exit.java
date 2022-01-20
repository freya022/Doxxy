package com.freya02.bot.commands.text;

import com.freya02.botcommands.api.Logging;
import com.freya02.botcommands.api.annotations.RequireOwner;
import com.freya02.botcommands.api.prefixed.BaseCommandEvent;
import com.freya02.botcommands.api.prefixed.TextCommand;
import com.freya02.botcommands.api.prefixed.annotations.JDATextCommand;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;

public class Exit extends TextCommand {
	private static final Logger LOGGER = Logging.getLogger();

	@RequireOwner
	@JDATextCommand(name = "exit")
	public void say(BaseCommandEvent event) throws InterruptedException {
		LOGGER.warn("Shutdown initiated by {} ({})", event.getAuthor().getAsTag(), event.getAuthor().getId());

		event.reactSuccess().mapToResult().complete();

		event.getJDA().shutdown();

		while (event.getJDA().getStatus() != JDA.Status.SHUTDOWN) {
			Thread.sleep(15);

			Thread.onSpinWait();
		}

		System.exit(0);
	}
}
