package com.mrkirby153.tgabot.polls

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.db.models.PollCategory
import com.mrkirby153.tgabot.db.models.PollVote
import me.mrkirby153.kcutils.timing.Debouncer
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object PollDisplayManager {

    private val debouncer = Debouncer<PollCategory>(Consumer {
        update(it)
    }, mode = Debouncer.Mode.BOTH)


    fun update(category: PollCategory) {
        Bot.logger.debug("Updating poll message for category ${category.id}")
        val msg = if (category.messageId == null) {
            val m = Bot.jda.getGuildById(category.guild).getTextChannelById(
                    category.channel).sendMessage(
                    "_ _").complete()
            category.messageId = m.id
            category.save()
            m
        } else {
            Bot.jda.getGuildById(category.guild).getTextChannelById(
                    category.channel).getMessageById(category.messageId).complete()
        }
        val before = msg.contentRaw
        val after = buildMessage(category)
        if (before == after) {
            Bot.logger.debug("Before and after messages are the same, skipping")
            return
        }
        msg.editMessage(buildMessage(category)).complete()
    }

    fun updateDebounced(category: PollCategory) {
        debouncer.debounce(category, 1, TimeUnit.MINUTES)
    }


    private fun buildMessage(category: PollCategory): String {
        val votes = Model.where(PollVote::class.java, "category", category.id).get()

        return buildString {
            appendln("─".repeat(30))
            appendln(
                    "<:tgalogo:367753930463117313> **${category.name.toUpperCase()}** <:tgalogo:367753930463117313>")
            appendln()
            category.options.forEach { option ->
                appendln("${option.asMention} - ${option.name}")
            }
            appendln()
            appendln("**${votes.size} votes**")
            appendln()
            appendln("Vote by clicking the reactions below!")
        }
    }

    fun shutdown() {
        this.debouncer.shutdown()
    }
}