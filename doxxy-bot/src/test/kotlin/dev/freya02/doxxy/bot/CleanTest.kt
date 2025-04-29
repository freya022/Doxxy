package dev.freya02.doxxy.bot

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

object CleanTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val str = """
				<div class="description">
				<ul class="blockList">
				<li class="blockList">
				<dl>
				<dt>All Superinterfaces:</dt>
				<dd><code><a href="../Component.html" title="interface in net.dv8tion.jda.api.interactions.components">Component</a></code>, <code><a href="../../../utils/data/SerializableData.html" title="interface in net.dv8tion.jda.api.utils.data">SerializableData</a></code></dd>
				</dl>
				<hr>
				<pre>public interface <span class="typeNameLabel">SelectionMenu</span>
				extends <a href="../Component.html" title="interface in net.dv8tion.jda.api.interactions.components">Component</a></pre>
				<div class="block">Represents a selection menu in a message.
				 <br>This is an interactive component and usually located within an <a href="../ActionRow.html" title="class in net.dv8tion.jda.api.interactions.components"><code>ActionRow</code></a>.
				 One selection menu fills up an entire action row by itself. You cannot have an action row with other components if a selection menu is present in the same row.

				 <p>The selections a user makes are only visible within their current client session.
				 Other users cannot see the choices selected and they will disappear when the client restarts or the message is reloaded.

				 </p><h2>Examples</h2>
				 <pre><code>
				 public void onSlashCommand(SlashCommandEvent event) {
				   if (!event.getName().equals("class")) return;

				   SelectionMenu menu = SelectionMenu.create("menu:class")
				     .setPlaceholder("Choose your class") // shows the placeholder indicating what this menu is for
				     .setRequireRange(1, 1) // only one can be selected
				     .addOption("Arcane Mage", "mage-arcane")
				     .addOption("Fire Mage", "mage-fire")
				     .addOption("Frost Mage", "mage-frost")
				     .build();

				   event.reply("Please pick your class below")
				     .setEphemeral(true)
				     .addActionRow(menu)
				     .queue();
				 }
				 </code></pre></div>
				</li>
				</ul>
				</div>
				""".trimIndent()
        val clean = Jsoup.clean(
            str,
            "https://docs.jda.wiki/net/dv8tion/jda/api/JDABuilder.html",
            Safelist.relaxed()
                .removeAttributes("a", "title")
        )
            .replace("<pre><code>(\\X*?)</code></pre>".toRegex(), "```java\n$1```")
        println(clean)
    }
}