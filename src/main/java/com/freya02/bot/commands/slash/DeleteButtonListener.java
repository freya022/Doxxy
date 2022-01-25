package com.freya02.bot.commands.slash;

import com.freya02.botcommands.api.components.Components;
import com.freya02.botcommands.api.components.InteractionConstraints;
import com.freya02.botcommands.api.components.annotations.JDAButtonListener;
import com.freya02.botcommands.api.components.event.ButtonEvent;
import com.freya02.botcommands.api.utils.EmojiUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class DeleteButtonListener {
	private static final String DELETE_MESSAGE_BUTTON_LISTENER_NAME = "DeleteButtonListener: deleteMessage";
	private static final Emoji WASTEBASKET = EmojiUtils.resolveJDAEmoji("wastebasket");

	@JDAButtonListener(name = DELETE_MESSAGE_BUTTON_LISTENER_NAME)
	public void onDeleteMessageClick(ButtonEvent event) {
		event.deferEdit().queue();

		event.getMessage().delete().queue();
	}

	public static Button getDeleteButton(User allowedUser) {
		return Components.dangerButton(DELETE_MESSAGE_BUTTON_LISTENER_NAME)
				.setConstraints(InteractionConstraints.ofUsers(allowedUser).addPermissions(Permission.MESSAGE_MANAGE))
				.oneUse()
				.build(WASTEBASKET);
	}
}
