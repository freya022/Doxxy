package com.freya02.bot.commands.slash.versioning;

import com.freya02.bot.commands.slash.DeleteButtonListener;
import com.freya02.bot.utils.CryptoUtils;
import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.LibraryType;
import com.freya02.bot.versioning.github.*;
import com.freya02.bot.versioning.maven.MavenBranchProjectDependencyVersionChecker;
import com.freya02.bot.versioning.supplier.BuildToolType;
import com.freya02.bot.versioning.supplier.DependencySupplier;
import com.freya02.botcommands.api.BContext;
import com.freya02.botcommands.api.annotations.Optional;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandPath;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.DefaultValueSupplier;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.freya02.bot.Main.BRANCH_VERSIONS_FOLDER_PATH;
import static com.freya02.bot.utils.Utils.isBCGuild;

//TODO refactor
public class SlashJitpack extends ApplicationCommand {
	private static final String BRANCH_NUMBER_AUTOCOMPLETE_NAME = "SlashJitpack: branchNumber";
	private static final String BRANCH_NAME_AUTOCOMPLETE_NAME = "SlashJitpack: branchName";

	private final PullRequestCache bcPullRequestCache = new PullRequestCache("freya022", "BotCommands", null);
	private final PullRequestCache jdaPullRequestCache = new PullRequestCache("DV8FromTheWorld", "JDA", "master");

	private final Map<String, MavenBranchProjectDependencyVersionChecker> branchNameToJdaVersionChecker = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, UpdateCountdown> updateCountdownMap = new HashMap<>();

	private final Map<LibraryType, UpdateCountdown> updateMap = new HashMap<>();
	private final Map<LibraryType, GithubBranchMap> branchMap = new HashMap<>();

	public SlashJitpack() throws IOException {
		Files.createDirectories(BRANCH_VERSIONS_FOLDER_PATH);
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

	@Override
	@NotNull
	public List<Command.Choice> getOptionChoices(@Nullable Guild guild, @NotNull CommandPath commandPath, int optionIndex) {
		if (optionIndex == 0) {
			if (isBCGuild(guild)) {
				return List.of(
						new Command.Choice("BotCommands", LibraryType.BOT_COMMANDS.name()),
						new Command.Choice("JDA 5", LibraryType.JDA5.name())
				);
			}

			return List.of(
					new Command.Choice("JDA 5", LibraryType.JDA5.name())
			);
		}

		return super.getOptionChoices(guild, commandPath, optionIndex);
	}

	@Override
	@Nullable //Need to set JDA 5 as a default value if in a non-BC guild
	public DefaultValueSupplier getDefaultValueSupplier(@NotNull BContext context, @NotNull Guild guild,
	                                                    @Nullable String commandId, @NotNull CommandPath commandPath,
	                                                    @NotNull String optionName, @NotNull Class<?> parameterType) {
		if (optionName.equals("library_type")) {
			if (!isBCGuild(guild)) {
				return e -> LibraryType.JDA5;
			}
		}

		return super.getDefaultValueSupplier(context, guild, commandId, commandPath, optionName, parameterType);
	}

	@JDASlashCommand(
			name = "jitpack",
			subcommand = "maven",
			group = "pr",
			description = "Shows you how to use Pull Requests for your bot"
	)
	public void onSlashJitpackPRMaven(GuildSlashEvent event,
	                                  @NotNull @AppOption(description = "Type of library") LibraryType libraryType,
	                                  @AppOption(description = "The number of the issue", autocomplete = BRANCH_NUMBER_AUTOCOMPLETE_NAME) int issueNumber) throws IOException {
		onSlashJitpackPR(event, libraryType, BuildToolType.MAVEN, issueNumber);
	}

	@JDASlashCommand(
			name = "jitpack",
			group = "pr",
			subcommand = "gradle",
			description = "Shows you how to use Pull Requests for your bot"
	)
	public void onSlashJitpackPRGradle(GuildSlashEvent event,
	                                   @NotNull @AppOption(description = "Type of library") LibraryType libraryType,
	                                   @AppOption(description = "The number of the issue", autocomplete = BRANCH_NUMBER_AUTOCOMPLETE_NAME) int issueNumber) throws IOException {
		onSlashJitpackPR(event, libraryType, BuildToolType.GRADLE, issueNumber);
	}

	@JDASlashCommand(
			name = "jitpack",
			group = "pr",
			subcommand = "kotlin_gradle",
			description = "Shows you how to use Pull Requests for your bot"
	)
	public void onSlashJitpackPRKT(GuildSlashEvent event,
	                               @NotNull @AppOption(description = "Type of library") LibraryType libraryType,
	                               @AppOption(description = "The number of the issue", autocomplete = BRANCH_NUMBER_AUTOCOMPLETE_NAME) int issueNumber) throws IOException {
		onSlashJitpackPR(event, libraryType, BuildToolType.GRADLE_KTS, issueNumber);
	}

	private void onSlashJitpackPR(@NotNull GuildSlashEvent event,
	                              @NotNull LibraryType libraryType,
	                              @NotNull BuildToolType buildToolType,
	                              int issueNumber) throws IOException {
		final PullRequest pullRequest = switch (libraryType) {
			case BOT_COMMANDS -> bcPullRequestCache.getPullRequests().get(issueNumber);
			case JDA5 -> jdaPullRequestCache.getPullRequests().get(issueNumber);
			default -> throw new IllegalArgumentException();
		};

		if (pullRequest == null) {
			event.reply("Unknown Pull Request").setEphemeral(true).queue();

			return;
		}

		final EmbedBuilder builder = new EmbedBuilder();

		final String dependencyStr;
		if (libraryType == LibraryType.BOT_COMMANDS) {
			final GithubBranch branch = pullRequest.branch();

			final MavenBranchProjectDependencyVersionChecker jdaVersionChecker = branchNameToJdaVersionChecker.computeIfAbsent(branch.branchName(), x -> {
				try {
					return new MavenBranchProjectDependencyVersionChecker(getBranchFileName(branch),
							branch.ownerName(),
							branch.repoName(),
							"JDA",
							branch.branchName());
				} catch (IOException e) {
					throw new RuntimeException("Unable to create branch specific JDA version checker", e);
				}
			});

			checkGithubBranchUpdates(event, branch, jdaVersionChecker);

			final ArtifactInfo latestBotCommands = pullRequest.toJitpackArtifact();
			final ArtifactInfo jdaVersionFromBotCommands = jdaVersionChecker.getLatest();

			dependencyStr = DependencySupplier.formatBC(buildToolType, jdaVersionFromBotCommands, latestBotCommands);
		} else {
			final ArtifactInfo latestJDAVersion = pullRequest.toJitpackArtifact();

			dependencyStr = DependencySupplier.formatJDA5Jitpack(buildToolType, latestJDAVersion);
		}

		builder.setTitle(buildToolType.getHumanName() + " dependencies for " + libraryType.getDisplayString() + " @ PR #" + pullRequest.number(), pullRequest.pullUrl());
		builder.addField("PR Link", pullRequest.pullUrl(), false);

		switch (buildToolType) {
			case MAVEN -> builder.setDescription("```xml\n" + dependencyStr + "```");
			case GRADLE, GRADLE_KTS -> builder.setDescription("```gradle\n" + dependencyStr + "```");
		}

		event.replyEmbeds(builder.build())
				.addActionRow(DeleteButtonListener.getDeleteButton(event.getUser()))
				.queue();
	}

	@JDASlashCommand(
			name = "jitpack",
			subcommand = "maven",
			group = "branch",
			description = "Shows you how to use a branch for your bot"
	)
	public void onSlashJitpackBranchMaven(GuildSlashEvent event,
	                                      @NotNull @AppOption(description = "Type of library") LibraryType libraryType,
	                                      @Optional @AppOption(description = "The name of the branch", autocomplete = BRANCH_NAME_AUTOCOMPLETE_NAME) String branchName) throws IOException {
		onSlashJitpackBranch(event, libraryType, BuildToolType.MAVEN, branchName);
	}

	@JDASlashCommand(
			name = "jitpack",
			group = "branch",
			subcommand = "gradle",
			description = "Shows you how to use a branch for your bot"
	)
	public void onSlashJitpackBranchGradle(GuildSlashEvent event,
	                                       @NotNull @AppOption(description = "Type of library") LibraryType libraryType,
	                                       @Optional @AppOption(description = "The name of the branch", autocomplete = BRANCH_NAME_AUTOCOMPLETE_NAME) String branchName) throws IOException {
		onSlashJitpackBranch(event, libraryType, BuildToolType.GRADLE, branchName);
	}

	@JDASlashCommand(
			name = "jitpack",
			group = "branch",
			subcommand = "kotlin_gradle",
			description = "Shows you how to use a branch for your bot"
	)
	public void onSlashJitpackBranchKT(GuildSlashEvent event,
	                                   @NotNull @AppOption(description = "Type of library") LibraryType libraryType,
	                                   @Optional @AppOption(description = "The name of the branch", autocomplete = BRANCH_NAME_AUTOCOMPLETE_NAME) String branchName) throws IOException {
		onSlashJitpackBranch(event, libraryType, BuildToolType.GRADLE_KTS, branchName);
	}

	private void onSlashJitpackBranch(@NotNull GuildSlashEvent event,
	                                  @NotNull LibraryType libraryType,
	                                  @NotNull BuildToolType buildToolType,
	                                  @Nullable String branchName) throws IOException {

		GithubBranchMap githubBranchMap = getBranchMap(libraryType);

		final GithubBranch branch;
		if (branchName == null) {
			branch = githubBranchMap.defaultBranch();
		} else {
			branch = githubBranchMap.branches().get(branchName);
		}

		if (branch == null) {
			event.reply("Unknown branch '" + branchName + "'").setEphemeral(true).queue();

			return;
		}

		final EmbedBuilder builder = new EmbedBuilder();

		final String dependencyStr = switch (libraryType) {
			case JDA5 -> DependencySupplier.formatJDA5Jitpack(buildToolType, branch.toJitpackArtifact());
			case BOT_COMMANDS -> {
				final MavenBranchProjectDependencyVersionChecker jdaVersionChecker = branchNameToJdaVersionChecker.computeIfAbsent(branchName, x -> {
					try {
						return new MavenBranchProjectDependencyVersionChecker(getBranchFileName(branch),
								branch.ownerName(),
								branch.repoName(),
								"JDA",
								branch.branchName());
					} catch (IOException e) {
						throw new RuntimeException("Unable to create branch specific JDA version checker", e);
					}
				});

				checkGithubBranchUpdates(event, branch, jdaVersionChecker);

				yield DependencySupplier.formatBC(buildToolType,
						jdaVersionChecker.getLatest(),
						new ArtifactInfo(branch.ownerName(),
								branch.repoName(),
								branch.latestCommitSha().asSha10())
				);
			}
			default -> throw new IllegalArgumentException("Invalid lib type: " + libraryType);
		};

		builder.setTitle(buildToolType.getHumanName() + " dependencies for " + libraryType.getDisplayString() + " @ branch '" + branchName + "'", branch.toURL());
		builder.addField("Branch Link", branch.toURL(), false);

		switch (buildToolType) {
			case MAVEN -> builder.setDescription("```xml\n" + dependencyStr + "```");
			case GRADLE, GRADLE_KTS -> builder.setDescription("```gradle\n" + dependencyStr + "```");
		}

		event.replyEmbeds(builder.build())
				.addActionRow(DeleteButtonListener.getDeleteButton(event.getUser()))
				.queue();
	}

	private void checkGithubBranchUpdates(@NotNull GuildSlashEvent event, GithubBranch branch, MavenBranchProjectDependencyVersionChecker checker) throws IOException {
		final UpdateCountdown updateCountdown = updateCountdownMap.computeIfAbsent(branch.branchName(), x -> new UpdateCountdown(5, TimeUnit.MINUTES));

		if (updateCountdown.needsUpdate()) {
			checker.checkVersion();

			event.getContext().invalidateAutocompletionCache(BRANCH_NUMBER_AUTOCOMPLETE_NAME);

			checker.saveVersion();
		}
	}

	private GithubBranchMap getBranchMap(@NotNull LibraryType libraryType) throws IOException {
		final UpdateCountdown updateCountdown = updateMap.computeIfAbsent(libraryType, x -> new UpdateCountdown(5, TimeUnit.MINUTES));

		GithubBranchMap githubBranchMap;
		synchronized (branchMap) {
			githubBranchMap = branchMap.get(libraryType);

			if (githubBranchMap == null || updateCountdown.needsUpdate()) {
				githubBranchMap = retrieveBranchList(libraryType);

				branchMap.put(libraryType, githubBranchMap);
			}
		}

		return githubBranchMap;
	}

	private GithubBranchMap retrieveBranchList(LibraryType libraryType) throws IOException {
		final String ownerName, repoName;

		switch (libraryType) {
			case JDA5 -> {
				ownerName = "DV8FromTheWorld";
				repoName = "JDA";
			}
			case BOT_COMMANDS -> {
				ownerName = "freya022";
				repoName = "BotCommands";
			}
			default -> throw new IllegalArgumentException("No branches for " + libraryType);
		}

		final Map<String, GithubBranch> map = new HashMap<>();

		for (GithubBranch branch : GithubUtils.getBranches(ownerName, repoName)) {
			map.put(branch.branchName(), branch);
		}

		final String defaultBranchName = GithubUtils.getDefaultBranchName(ownerName, repoName);
		final GithubBranch defaultBranch = map.get(defaultBranchName);

		return new GithubBranchMap(defaultBranch, map);
	}

	@NotNull
	private Path getBranchFileName(GithubBranch branch) {
		return BRANCH_VERSIONS_FOLDER_PATH.resolve("%s-%s-%s.txt".formatted(branch.ownerName(),
				branch.repoName(),
				CryptoUtils.hash(branch.branchName())));
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

	@CacheAutocompletion
	@AutocompletionHandler(name = BRANCH_NAME_AUTOCOMPLETE_NAME)
	public Collection<String> onBranchNameAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                   @CompositeKey @AppOption LibraryType libraryType) throws IOException {
		GithubBranchMap githubBranchMap = getBranchMap(libraryType);

		return githubBranchMap.branches().keySet();
	}

	private String pullRequestToString(PullRequest referent) {
		return "%d - %s (%s)".formatted(referent.number(), referent.title(), referent.branch().ownerName());
	}
}