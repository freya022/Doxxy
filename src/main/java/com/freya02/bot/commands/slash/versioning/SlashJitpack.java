package com.freya02.bot.commands.slash.versioning;

import com.freya02.bot.Main;
import com.freya02.bot.commands.slash.DeleteButtonListener;
import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.LibraryType;
import com.freya02.bot.versioning.github.PullRequest;
import com.freya02.bot.versioning.github.PullRequestCache;
import com.freya02.bot.versioning.github.UpdateCountdown;
import com.freya02.bot.versioning.maven.MavenBranchProjectDependencyVersionChecker;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandPath;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CacheAutocompletion;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CompositeKey;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.ToStringFunction;
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SlashJitpack extends ApplicationCommand {
	private static final String BRANCH_NUMBER_AUTOCOMPLETE_NAME = "SlashJitpack: branchNumber";

	private final PullRequestCache bcPullRequestCache = new PullRequestCache("freya022", "BotCommands", null);
	private final PullRequestCache jdaPullRequestCache = new PullRequestCache("DV8FromTheWorld", "JDA", "master");

	private final Map<String, MavenBranchProjectDependencyVersionChecker> branchNameToJdaVersionChecker = new HashMap<>();
	private final UpdateCountdown updateCountdown = new UpdateCountdown(5, TimeUnit.MINUTES);

	@Override
	@NotNull
	public List<Command.Choice> getOptionChoices(@Nullable Guild guild, @NotNull CommandPath commandPath, int optionIndex) {
		if (optionIndex == 0) {
			return List.of(
					new Command.Choice("BotCommands", LibraryType.BOT_COMMANDS.name()),
					new Command.Choice("JDA 5", LibraryType.JDA5.name())
			);
		}

		return super.getOptionChoices(guild, commandPath, optionIndex);
	}

	@JDASlashCommand(
			name = "jitpack",
			description = "Shows you how to use Pull Requests for your bot"
	)
	public void onSlashJitpack(GuildSlashEvent event,
	                           @NotNull @AppOption(description = "Type of library") LibraryType libraryType,
	                           @AppOption(description = "The number of the issue", autocomplete = BRANCH_NUMBER_AUTOCOMPLETE_NAME) long issueNumber) throws IOException {
		final PullRequest pullRequest = switch (libraryType) {
			case BOT_COMMANDS -> bcPullRequestCache.getPullRequests().get((int) issueNumber);
			case JDA5 -> jdaPullRequestCache.getPullRequests().get((int) issueNumber);
			default -> throw new IllegalArgumentException();
		};

		if (pullRequest == null) {
			event.reply("Unknown Pull Request").setEphemeral(true).queue();

			return;
		}

		final EmbedBuilder builder = new EmbedBuilder();

		final String xml;
		if (libraryType == LibraryType.BOT_COMMANDS) { //Default
			final MavenBranchProjectDependencyVersionChecker checker = branchNameToJdaVersionChecker.computeIfAbsent(pullRequest.headBranchName(), x -> {
				try {
					return new MavenBranchProjectDependencyVersionChecker(getPRFileName(pullRequest),
							pullRequest.headOwnerName(),
							pullRequest.headRepoName(),
							"JDA",
							pullRequest.headBranchName());
				} catch (IOException e) {
					throw new RuntimeException("Unable to create branch specific JDA version checker", e);
				}
			});

			if (updateCountdown.needsUpdate()) {
				checker.checkVersion();

				event.getContext().invalidateAutocompletionCache(BRANCH_NUMBER_AUTOCOMPLETE_NAME);

				checker.saveVersion();
			}

			final ArtifactInfo latestBotCommands = pullRequest.toJitpackArtifact();
			final ArtifactInfo jdaVersionFromBotCommands = checker.getLatest();

			builder.setTitle("Maven dependencies for BotCommands for PR #" + pullRequest.number());
			builder.addField("PR Link", pullRequest.pullUrl(), false);

			xml = VersioningCommons.formatBC(jdaVersionFromBotCommands, latestBotCommands);
		} else {
			final ArtifactInfo latestJDAVersion = pullRequest.toJitpackArtifact();

			builder.setTitle("Maven dependencies for JDA for PR #" + pullRequest.number());
			builder.addField("PR Link", pullRequest.pullUrl(), false);

			xml = VersioningCommons.formatJDA5(latestJDAVersion);
		}

		builder.setDescription("```xml\n" + xml + "```");

		event.replyEmbeds(builder.build())
				.addActionRow(DeleteButtonListener.getDeleteButton(event.getUser()))
				.queue();
	}

	@NotNull
	private Path getPRFileName(PullRequest pullRequest) {
		return Main.LAST_KNOWN_VERSIONS_FOLDER_PATH.resolve("%s-%s-%d.txt".formatted(pullRequest.headOwnerName(),
				pullRequest.headRepoName(),
				pullRequest.number()));
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = BRANCH_NUMBER_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<Command.Choice> onBranchNumberAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                             @CompositeKey @AppOption LibraryType libraryType) throws IOException {

		final PullRequest[] pullRequests = switch (libraryType) {
			case BOT_COMMANDS -> bcPullRequestCache.getPullRequests().values(new PullRequest[0]);
			case JDA5 -> jdaPullRequestCache.getPullRequests().values(new PullRequest[0]);
			default -> throw new IllegalArgumentException();
		};

		return fuzzyMatching(Arrays.asList(pullRequests), this::pullRequestToString, event)
				.stream()
				.map(r -> new Command.Choice(pullRequestToString(r.getReferent()), r.getReferent().number()))
				.toList();
	}

	private static List<BoundExtractedResult<PullRequest>> fuzzyMatching(Collection<PullRequest> items, ToStringFunction<PullRequest> toStringFunction, CommandAutoCompleteInteractionEvent event) {
		final List<PullRequest> list = items
				.stream()
				.sorted(Comparator.comparingInt(PullRequest::number).reversed())
				.toList();

		final AutoCompleteQuery autoCompleteQuery = event.getFocusedOption();

		if (autoCompleteQuery.getValue().isBlank()) {
			final List<BoundExtractedResult<PullRequest>> l = new ArrayList<>();

			for (int i = 0, listSize = list.size(); i < listSize; i++) {
				PullRequest request = list.get(i);

				l.add(new BoundExtractedResult<>(request, "", 100, i));
			}

			return l;
		}

		//First sort the results by similarities but by taking into account an incomplete input
		final List<BoundExtractedResult<PullRequest>> bigLengthDiffResults = FuzzySearch.extractTop(autoCompleteQuery.getValue(),
				list,
				toStringFunction,
				FuzzySearch::partialRatio,
				OptionData.MAX_CHOICES);

		//Then sort the results by similarities but don't take length into account
		return FuzzySearch.extractTop(autoCompleteQuery.getValue(),
				bigLengthDiffResults.stream().map(BoundExtractedResult::getReferent).toList(),
				toStringFunction,
				FuzzySearch::ratio,
				OptionData.MAX_CHOICES);
	}

	private String pullRequestToString(PullRequest referent) {
		return "%d - %s (%s)".formatted(referent.number(), referent.title(), referent.headOwnerName());
	}
}