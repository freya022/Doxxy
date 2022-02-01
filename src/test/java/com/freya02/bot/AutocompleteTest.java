package com.freya02.bot;

import info.debatty.java.stringsimilarity.*;
import info.debatty.java.stringsimilarity.interfaces.StringSimilarity;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.ToStringFunction;
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutocompleteTest {
	private static final List<String> classNames = List.of("AllowedMentions", "GenericMessageReactionEvent", "GuildVoiceMuteEvent", "UserContextInteractionEvent", "WidgetUtil", "ICategorizableChannel", "GuildUpdateVanityCodeEvent", "VanityInvite", "GuildMemberUpdateEvent", "Button", "MessageBuilder.SplitPolicy.CharSequenceSplitPolicy", "SessionController", "UnifiedMemberCacheView", "GuildUpdateMaxMembersEvent", "WidgetUtil.BannerType", "ChannelManager", "Guild.VerificationLevel", "ChannelCreateEvent", "EmoteUpdateRolesEvent", "ThreadMemberLeaveEvent", "InterfacedEventManager", "SpeakingMode", "WidgetUtil.Widget.Member", "MiscUtil", "MessageHistory", "AuditLogEntry", "GuildUpdateAfkChannelEvent", "ActionComponent", "AccountManager", "GuildUpdateMaxPresencesEvent", "CategoryOrderAction", "IPermissionContainerManager", "GuildMemberUpdatePendingEvent", "GenericEmoteEvent", "ConcurrentSessionController", "ThreadChannelPaginationAction", "ThreadChannelManager", "WebhookMessageUpdateAction", "MessageEmbedEvent", "ShardCacheView", "ConnectionListener", "IPermissionHolder", "GuildUpdateBannerEvent", "ChannelUpdateAutoArchiveDurationEvent", "WebhookMessageAction", "CombinedAudio", "DataType", "HttpException", "ThreadChannel", "MessageContextInteraction", "GuildVoiceStreamEvent", "MessageActivity.ActivityType", "GuildUpdateFeaturesEvent", "GuildVoiceUpdateEvent", "IThreadContainer", "BaseGuildMessageChannelManager", "CommandListUpdateAction", "GenericStageInstanceUpdateEvent", "GenericGuildMemberUpdateEvent", "SerializableArray", "RoleOrderAction", "ErrorResponse", "MessageChannel", "GuildUpdateOwnerEvent", "ReplaceWith", "IPositionableChannel", "MessageContextInteractionEvent", "SelectMenu", "ChannelUpdateRegionEvent", "MessageEmbed", "RoleUpdateMentionableEvent", "TimeUtil", "CommandInteraction", "SelectMenuInteraction", "StageInstance", "DataArray", "GenericRoleEvent", "MessageBuilder", "ThreadLocalReason", "GuildMemberRemoveEvent", "UserActivityEndEvent", "Procedure", "ButtonStyle", "MessageType", "GenericGuildEvent", "AutoCompleteCallbackAction", "UserAudio", "Interaction", "GuildVoiceState", "GatewayPingEvent", "IInviteContainer", "MessageReference", "GuildUpdateRulesChannelEvent", "RoleUpdateColorEvent", "GenericGuildVoiceEvent", "Role", "ChannelUpdateBitrateEvent", "GuildChannel", "VoiceDispatchInterceptor", "DataObject", "SnowflakeCacheView", "RoleUpdatePermissionsEvent", "PermissionOverrideAction", "Icon", "IMemberContainer", "UserUpdateOnlineStatusEvent", "IDeferrableCallback", "JDA.Status", "PaginationAction", "PermissionOverride", "PrivateChannel", "EmoteManager", "CommandEditAction", "RoleUpdatePositionEvent", "MarkdownUtil", "StageChannelManager", "ShardManager", "UserActivityStartEvent", "GuildVoiceRequestToSpeakEvent", "GuildVoiceSelfDeafenEvent", "HierarchyException", "AudioReceiveHandler", "Guild.ExplicitContentLevel", "SelectOption", "MessageReactionRemoveAllEvent", "MessageBulkDeleteEvent", "GuildInviteCreateEvent", "SelfUpdateDiscriminatorEvent", "Component", "GuildUpdateCommunityUpdatesChannelEvent", "MessageBuilder.Formatting", "MessageEmbed.Footer", "SubcommandData", "SubcommandGroupData", "ChannelOrderAction", "AudioNatives", "RichPresence.Party", "ErrorResponseException.SchemaError", "ActionRow", "RoleManager", "RoleCreateEvent", "Invite.EmbeddedApplication", "GatewayIntent", "StageInstance.PrivacyLevel", "TemplateGuild", "GuildMessageChannel", "Command", "EmoteUpdateNameEvent", "StageChannel", "ChannelUpdateLockedEvent", "MessageBuilder.SplitPolicy", "CommandPrivilege", "DisconnectEvent", "UpdateEvent", "OptionMapping", "EmoteRemovedEvent", "UserUpdateNameEvent", "MessageEditCallbackAction", "GuildUpdateMFALevelEvent", "ThreadMemberJoinEvent", "PaginationAction.PaginationIterator", "MessageReceivedEvent", "MarkdownSanitizer.SanitizationStrategy", "StageInstanceCreateEvent", "ICopyableChannel", "TemplateChannel.PermissionOverride", "WidgetUtil.WidgetTheme", "GuildUpdateVerificationLevelEvent", "StageInstanceDeleteEvent", "ConnectionStatus", "MessageEmbed.Provider", "Message.Attachment", "GuildAction.RoleData", "ComponentInteraction", "StageInstanceUpdatePrivacyLevelEvent", "AttachmentOption", "CommandAutoCompleteInteraction", "GuildVoiceSuppressEvent", "Activity.ActivityType", "GuildUpdateBoostTierEvent", "SessionControllerAdapter", "MemberCachePolicy", "CommandData", "LayoutComponent", "PermissionException", "ClientType", "MessageReactionRemoveEvent", "GuildMemberUpdateAvatarEvent", "DefaultSendFactory", "MessageUpdateEvent", "RoleUpdateIconEvent", "MessageEmbed.AuthorInfo", "ReconnectedEvent", "CloseCode", "GenericMessageEvent", "UserUpdateAvatarEvent", "ButtonInteractionEvent", "ParsingException", "Commands", "ChannelAction", "BaseGuildMessageChannel", "Guild.NSFWLevel", "ExTermEncoder", "TemplateChannel", "ExTermTag", "ThreadHiddenEvent", "DelayedCompletableFuture", "RoleUpdateHoistedEvent", "DefaultShardManager", "WebhookAction", "NewsChannel", "OptionData", "ThreadPoolProvider", "GenericCommandInteractionEvent", "Guild.NotificationLevel", "GuildAction", "Message.MentionType", "ChannelType", "GenericUserPresenceEvent", "Compression", "Task", "MessageActivity.Application", "CommandCreateAction", "IPacketProvider", "CommandPrivilege.Type", "Result", "Template", "Role.RoleTags", "MessageReactionRemoveEmoteEvent", "ReadyEvent", "Message.MessageFlag", "MessageActivity", "ApplicationTeam", "GenericChannelEvent", "GenericChannelUpdateEvent", "ChannelUpdateUserLimitEvent", "TemplateManager", "WebhookType", "Invite.Guild", "Message.Interaction", "GenericGuildVoiceUpdateEvent", "ErrorHandler", "SelfUpdateAvatarEvent", "StoreChannelManager", "SelectMenu.Builder", "GuildBanEvent", "AudioSendHandler", "GuildMemberRoleRemoveEvent", "MessageDeleteEvent", "IPositionableChannelManager", "GuildUpdateAfkTimeoutEvent", "InteractionType", "GuildMemberJoinEvent", "WebhookManager", "RestFuture", "MarkdownSanitizer", "JDAInfo", "MessageEmbed.VideoInfo", "RateLimitedException", "OptionType", "RoleIcon", "ListedEmote", "VoiceDispatchInterceptor.VoiceStateUpdate", "ActivityFlag", "User.UserFlag", "GuildVoiceVideoEvent", "VoiceDispatchInterceptor.VoiceServerUpdate", "AuditLogChange", "GuildMemberRoleAddEvent", "LockIterator", "MessageSticker", "Invite.TargetType", "Invite.InviteType", "UserUpdateActivityOrderEvent", "CommandInteractionPayload", "AccountType", "TeamMember", "User.Profile", "GuildVoiceSelfMuteEvent", "ChannelUpdateNSFWEvent", "GuildUpdateDescriptionEvent", "InviteAction", "GuildAction.ChannelData", "RoleAction", "UserUpdateActivitiesEvent", "ContextInteraction.ContextTarget", "Guild.BoostTier", "GuildUpdateNotificationLevelEvent", "ReplyCallbackAction", "Invite", "StageInstanceAction", "ChannelUpdateTopicEvent", "RoleUpdateNameEvent", "Webhook.WebhookReference", "ChannelDeleteEvent", "Activity", "Invite.Group", "ContextException.ContextConsumer", "GenericUserEvent", "StageInstanceManager", "RawGatewayEvent", "WidgetUtil.Widget", "GenericSelfUpdateEvent", "AudioManager", "GuildUpdateSystemChannelEvent", "GuildUpdateNSFWLevelEvent", "UserUpdateFlagsEvent", "Permission", "GuildUnavailableEvent", "ChunkingFilter", "IOBiConsumer", "AuditLogKey", "CategoryManager", "Webhook.ChannelReference", "SelfUpdateMFAEvent", "GenericStageInstanceEvent", "GuildVoiceJoinEvent", "TeamMember.MembershipState", "ErrorResponseException.ErrorCode", "GenericEmoteUpdateEvent", "GatewayEncoding", "ErrorResponseException", "OpusPacket", "SessionController.SessionConnectNode", "GuildAvailableEvent", "HttpRequestEvent", "JDA", "ChannelField", "MissingAccessException", "Timestamp", "DefaultSendSystem", "VoiceChannelManager", "GenericGuildMemberEvent", "ItemComponent", "CacheView.SimpleCacheView", "GuildInviteDeleteEvent", "ReactionPaginationAction", "MessageEmbed.Field", "Guild.Ban", "MessageReactionAddEvent", "ChannelUpdateArchiveTimestampEvent", "MessageSticker.StickerFormat", "PermissionOverrideUpdateEvent", "GuildUpdateBoostCountEvent", "GuildUpdateNameEvent", "Event", "ExTermDecoder", "ICategorizableChannelManager", "Webhook", "MemberAction", "StatusChangeEvent", "WidgetUtil.Widget.VoiceState", "ChannelUpdateTypeEvent", "Incubating", "Manager", "GenericGuildUpdateEvent", "GenericRoleUpdateEvent", "MessageEmbed.ImageInfo", "UnavailableGuildLeaveEvent", "Emote", "StoreChannel", "OnlineStatus", "AnnotatedEventManager", "ContextInteraction", "GuildUnbanEvent", "Region", "ButtonInteraction", "RichPresence", "AutoCompleteQuery", "ChannelUpdateArchivedEvent", "PermissionOverrideCreateEvent", "ThreadRevealedEvent", "GuildVoiceGuildDeafenEvent", "ChannelUpdateParentEvent", "InsufficientPermissionException", "Invite.InviteTarget", "RichPresence.Image", "ThreadChannel.AutoArchiveDuration", "ThreadChannelAction", "GenericGuildInviteEvent", "SlashCommandData", "ContextException", "GuildVoiceMoveEvent", "SlashCommandInteraction", "Command.Subcommand", "IMentionable", "ChannelUpdateNameEvent", "Presence", "Icon.IconType", "IPermissionContainer", "RestAction", "GuildUpdateExplicitContentLevelEvent", "ResumedEvent", "TimeFormat", "EmbedType", "GenericContextInteractionEvent", "GuildManager", "SelfUpdateNameEvent", "Guild.MFALevel", "VoiceDispatchInterceptor.VoiceUpdate", "Request", "GuildUpdateLocaleEvent", "MessageReaction.ReactionEmote", "EmbedBuilder", "Component.Type", "ThreadManager", "MemberCacheView", "GuildJoinEvent", "WebhookClient", "MessageEmbed.Thumbnail", "SessionController.ShardedGateway", "CommandAutoCompleteInteractionEvent", "StageInstanceUpdateTopicEvent", "GuildMemberUpdateBoostTimeEvent", "UnavailableGuildJoinedEvent", "AccountTypeException", "SortedSnowflakeCacheView", "DefaultShardManagerBuilder", "VoiceChannel", "EmoteAddedEvent", "Invite.Channel", "AudioChannel", "IAudioSendSystem", "InteractionCallbackAction.ResponseType", "Command.Option", "IAutoCompleteCallback", "IOFunction", "AudioChannelManager", "Activity.Emoji", "ThreadMember", "DeprecatedSince", "AuditLogOption", "IOConsumer", "MessageHistory.MessageRetrieveAction", "InteractionCallbackAction", "SerializableData", "GuildVoiceGuildMuteEvent", "SelectMenuInteractionEvent", "ShutdownEvent", "Guild.Timeout", "ForRemoval", "CacheView", "Command.Choice", "GenericThreadMemberEvent", "TargetType", "GuildVoiceDeafenEvent", "EventListener", "GuildUpdateSplashEvent", "GenericComponentInteractionCreateEvent", "ClosableIterator", "GuildReadyEvent", "UserTypingEvent", "ListenerProxy", "GenericThreadEvent", "ExceptionEvent", "CacheFlag", "ActionType", "IEventManager", "GuildLeaveEvent", "ChannelUpdateSlowmodeEvent", "InteractionFailureException", "TextChannel", "SlashCommandInteractionEvent", "GenericEvent", "DirectAudioController", "Guild.MetaData", "Command.Type", "GenericUserUpdateEvent", "Member", "GuildTimeoutEvent", "ISnowflake", "AuditLogPaginationAction", "Channel", "SelfUpdateVerifiedEvent", "UserContextInteraction", "TemplateRole", "MessageReaction", "InteractionHook", "GuildMemberUpdateTimeOutEvent", "PermissionOverrideDeleteEvent", "NewsChannelManager", "UserUpdateDiscriminatorEvent", "SelfUser", "GuildVoiceLeaveEvent", "Category", "IReplyCallback", "JDA.ShardInfo", "User", "IMessageEditCallback", "Message", "PermOverrideManager", "WidgetUtil.Widget.VoiceChannel", "JDABuilder", "ThreadLocalReason.Closable", "ApplicationInfo", "Guild", "ChannelUpdateInvitableEvent", "TextChannelManager", "Webhook.GuildReference", "SubscribeEvent", "RoleDeleteEvent", "MessageAction", "Emoji", "IAudioSendFactory", "GuildUpdateIconEvent", "AuditableRestAction", "GuildMemberUpdateNicknameEvent", "GenericPermissionOverrideEvent", "GenericInteractionCreateEvent", "ChannelUpdatePositionEvent", "Activity.Timestamps", "Command.SubcommandGroup", "OrderAction", "Response", "ListenerAdapter", "GenericAutoCompleteInteractionEvent", "MessagePaginationAction");

	public static void main(String[] args) {
		tryEntries(new JaroWinkler());

		System.out.println("-".repeat(50));
		tryEntries(new SorensenDice());

		System.out.println("-".repeat(50));
		tryEntries(new RatcliffObershelp());

		System.out.println("-".repeat(50));
		tryEntries(new Cosine(1));

		tryWith("TextChannel", "TxtChannel");

		tryWith("TextChannel", "TChannel");

		tryWith("TextChannel", "AChannel");

		tryWith("TextChannel", "AudChannel");
	}

	private static void tryEntries(StringSimilarity similarityAlgo) {
		showEntries("TxtChannel", similarityAlgo);
		showEntries("TChannel", similarityAlgo);
		showEntries("AChannel", similarityAlgo);
		showEntries("AudChannel", similarityAlgo);
		showEntries("MessageBuilder.SplitPolicy.CharSequenceSplitPolicy", similarityAlgo);
		showEntries("Msg.SpliPol.CharSequenceSplitPolicy", similarityAlgo);
		showEntries("CharSequenceSplitPolicy", similarityAlgo);
		showEntries("GuildUpdateVerification", similarityAlgo);
		showEntries("GuildUpdateVe", similarityAlgo);
		showEntries("GuildUpdateVerificationLevelEvent", similarityAlgo);
		showEntries("ActivityType", similarityAlgo);
		showEntries("ActActivityType", similarityAlgo);
		showEntries("MesType", similarityAlgo);
	}

	private static void showEntries(String str, StringSimilarity similarityAlgo) {
		final Map<String, Double> similarities = new HashMap<>();

		for (String className : classNames) {
			final double similarity = similarityAlgo.similarity(className, str);

			similarities.put(className, similarity);
		}

		System.out.println(str + " : " + similarities.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.limit(8)
				.map(e -> "%s : %.2f".formatted(e.getKey(), e.getValue()))
				.toList());

		System.out.println(" ".repeat(str.length() + 3) + getOriginalResults(str).stream()
				.limit(8)
				.map(b -> b.getString() + " : " + b.getScore() / 100.0)
				.toList());
	}

	private static List<BoundExtractedResult<String>> getOriginalResults(String str) {
		ToStringFunction<String> toStringFunction = s -> s;
		final List<String> list = classNames
				.stream()
				.sorted(Comparator.comparing(toStringFunction::apply))
				.toList();

		//First sort the results by similarities but by taking into account an incomplete input
		final List<BoundExtractedResult<String>> bigLengthDiffResults = FuzzySearch.extractTop(str,
				list,
				toStringFunction,
				FuzzySearch::partialRatio,
				OptionData.MAX_CHOICES);

		//Then sort the results by similarities but don't take length into account
		return FuzzySearch.extractTop(str,
				bigLengthDiffResults.stream().map(BoundExtractedResult::getReferent).toList(),
				toStringFunction,
				FuzzySearch::ratio,
				OptionData.MAX_CHOICES);
	}

	private static void tryWith(String str1, String str2) {
		System.out.println("-".repeat(50) + "%s vs %s".formatted(str1, str2) + "-".repeat(50));

		final Levenshtein levenshtein = new Levenshtein();
		final NormalizedLevenshtein normalizedLevenshtein = new NormalizedLevenshtein();
		final JaroWinkler winkler = new JaroWinkler();

		System.out.println("FuzzySearch.ratio(str1, str2) = " + FuzzySearch.ratio(str1, str2));
		System.out.println("FuzzySearch.partialRatio(str1, str2) = " + FuzzySearch.partialRatio(str1, str2));
		System.out.println("levenshtein.distance(str1, str2) = " + levenshtein.distance(str1, str2));
		System.out.println("normalizedLevenshtein.similarity(str1, str2) = " + normalizedLevenshtein.similarity(str1, str2));
		System.out.println("winkler.similarity(str1, str2) = " + winkler.similarity(str1, str2));
	}
}
