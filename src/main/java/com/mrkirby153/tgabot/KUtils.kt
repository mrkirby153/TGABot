package com.mrkirby153.tgabot

import com.google.common.cache.CacheBuilder
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.requests.RestAction
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

const val RED_CROSS = "❌"
const val GREEN_CHECK = "✅"
val JUMP_LINK_PATTERN: Pattern = Pattern.compile(
        "https?://(canary\\.|ptb\\.)?discordapp.com/channels/(\\d{17,18})/(\\d{17,18})/(\\d{17,18})")

fun Message.removeReaction(user: User, reaction: String): RestAction<Void> {
    val r = this.reactions.first { it.reactionEmote.name == reaction }
    return r.removeReaction(user)
}

fun Message.removeReaction(user: User, reaction: Emote): RestAction<Void> {
    val r = this.reactions.first { it.reactionEmote.isEmote && it.reactionEmote.emote == reaction }
    return r.removeReaction(user)
}

fun findEmoteById(emote: String): Emote? {
    Bot.jda.guilds.forEach {
        val emote = it.getEmoteById(emote)
        if (emote != null)
            return emote
    }
    return null
}


private val msgCache = CacheBuilder.newBuilder().expireAfterWrite(30,
        TimeUnit.SECONDS).build<String, String>()

private fun Guild.getMessage(channel: String, id: String): Message? {
    return try {
        this.getTextChannelById(channel)?.getMessageById(id)?.complete()
    } catch (e: Exception) {
        null
    }
}

fun Guild.findMessageById(id: String): Message? {
    Bot.logger.debug("Finding message with ID of $id in $this")

    if (id.matches(Regex("\\d{17,18}-\\d{17,18}"))) {
        Bot.logger.debug("Matches channelid-messageid. Parsing...")
        val split = id.split("-")
        val channel = split[0]
        val msg = split[1]

        val m = getMessage(channel, msg)
        if (m != null) {
            msgCache.put(msg, channel)
            return m
        }
    }

    val matcher = JUMP_LINK_PATTERN.matcher(id)
    if (matcher.find()) {
        Bot.logger.debug("Matches jump link... Parsing")
        val channel = matcher.group(3)
        val msg = matcher.group(4)
        try {
            return getTextChannelById(channel)?.getMessageById(msg)?.complete()
        } catch (e: Exception) {
            // Ignore
        }
    }


    val cached = msgCache.getIfPresent(id)
    if (cached != null) {
        val m = getMessage(cached, id)
        if (m != null) {
            msgCache.put(id, m.channel.id)
            return m
        }
    }
    // All other methods have failed. Lets try iterating
    if (!id.matches(Regex("\\d{17,18}"))) {
        Bot.logger.debug("ID passed is not a snowflake.")
        return null
    }
    this.textChannels.forEach {
        try {
            val m = it.getMessageById(id).complete()
            msgCache.put(id, m.channel.id)
            return m
        } catch (e: Exception) {
            // Ignore
        }
    }
    return null
}