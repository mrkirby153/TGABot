package com.mrkirby153.tgabot.listener

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.db.models.PollCategory
import com.mrkirby153.tgabot.db.models.PollOption
import com.mrkirby153.tgabot.polls.PollManager
import com.mrkirby153.tgabot.removeReaction
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class PollListener : ListenerAdapter() {

    companion object {
        val reactionManager = ReactionManager()

        val optionCache = CacheBuilder.newBuilder().expireAfterWrite(10,
                TimeUnit.MINUTES).build(object : CacheLoader<String, List<PollOption>>() {
            override fun load(mid: String): List<PollOption> {
                return Model.query(PollOption::class.java).where("message_id", mid).get()
            }
        })

        val categoryCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(
                object : CacheLoader<Long, PollCategory>() {
                    override fun load(category: Long): PollCategory {
                        return Model.query(PollCategory::class.java).where("id", category).first()
                    }

                }
        )

        val msgCache = CacheBuilder.newBuilder().expireAfterWrite(10,
                TimeUnit.MINUTES).build<String, Message>()
    }

    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        if (event.member.user.id == event.guild.selfMember.user.id)
            return
        val options = optionCache[event.messageId]
        val option = options.firstOrNull {
            if (event.reactionEmote.isEmote) {
                it.custom && it.reaction == event.reactionEmote.id
            } else {
                it.reaction == event.reactionEmote.name
            }
        } ?: return

        val category = categoryCache[option.category]

        val r = PollManager.registerVote(event.user, category, option)

        if (r != null)
            Bot.logger.debug("Registered vote with id ${r.id}")

        val cachedMsg = msgCache.getIfPresent(event.messageId)
        val msg = cachedMsg ?: event.channel.getMessageById(event.messageId).complete()
        val future = if (event.reactionEmote.isEmote) {
            msg.removeReaction(event.user, event.reactionEmote.emote)?.submitAfter(250,
                    TimeUnit.MILLISECONDS)
        } else {
            msg.removeReaction(event.user, event.reactionEmote.name)?.submitAfter(250,
                    TimeUnit.MILLISECONDS)
        }
        if (future != null)
            reactionManager.enqueue(msg, future)

        if (reactionManager.getToClear().contains(msg.id)) {
            Bot.logger.debug("Hit threshold for message ${msg.id} clearing and re-adding reactions")
            reactionManager.cancelAll(msg)
            msg.clearReactions().queueAfter(150, TimeUnit.MILLISECONDS) {
                PollManager.addOptionReactions(category)
            }
        }
    }
}