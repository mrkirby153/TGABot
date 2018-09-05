package com.mrkirby153.tgabot

import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.requests.RestAction

const val RED_CROSS = "❌"
const val GREEN_CHECK = "✅"

fun Message.removeReaction(user: User, reaction: String): RestAction<Void> {
    val r = this.reactions.first { it.reactionEmote.name == reaction }
    return r.removeReaction(user)
}

fun Message.removeReaction(user: User, reaction: Emote): RestAction<Void> {
    val r = this.reactions.first { it.reactionEmote.isEmote && it.reactionEmote.emote == reaction}
    return r.removeReaction(user)
}

fun findEmoteById(emote: String): Emote? {
    Bot.jda.guilds.forEach {
        val emote = it.getEmoteById(emote)
        if(emote != null)
            return emote
    }
    return null
}