package com.mrkirby153.tgabot

import com.google.common.cache.CacheBuilder
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.requests.RestAction
import java.util.concurrent.TimeUnit

const val RED_CROSS = "❌"
const val GREEN_CHECK = "✅"

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

fun Guild.findMessageById(id: String): Message? {
    Bot.logger.debug("Finding message with ID of $id in $this")

    val cached = msgCache.getIfPresent(id)
    if (cached != null) {
        val m = this.getTextChannelById(cached)?.getMessageById(id)?.complete() ?: return null
        msgCache.put(id, m.channel.id)
        return m
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