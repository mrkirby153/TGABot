package com.mrkirby153.tgabot.listener

import com.google.common.cache.CacheBuilder
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.db.models.PollCategory
import com.mrkirby153.tgabot.db.models.PollOption
import com.mrkirby153.tgabot.polls.PollManager
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class PollListener : ListenerAdapter() {

    companion object {
        val reactionManager = ReactionManager()

        val msgCache = CacheBuilder.newBuilder().expireAfterWrite(10,
                TimeUnit.MINUTES).build<String, Message>()
    }

    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        Bot.executor.submit {
            if (event.member.user.id == event.guild.selfMember.user.id)
                return@submit
            val options = Model.where(PollOption::class.java, "message_id", event.messageId).get()
            val option = options.firstOrNull {
                if(event.reactionEmote.isEmote){
                    it.custom && it.reaction == event.reactionEmote.id
                } else {
                    it.reaction == event.reactionEmote.name
                }
            }
            if(option == null) {
                Bot.logger.info("No suitable poll option found for ${event.reactionEmote} in ${event.channel}")
                return@submit
            }

            val category = Model.where(PollCategory::class.java, "id", option.category).first()

            val r = PollManager.registerVote(event.user, category, option)

            if (r != null)
                Bot.logger.debug("Registered vote with id ${r.id}")

            val cachedMsg = msgCache.getIfPresent(event.messageId)
            val msg = cachedMsg ?: event.channel.getMessageById(event.messageId).complete()
            if (event.reactionEmote.isEmote) {
                reactionManager.removeReaction(msg, event.user, event.reactionEmote.emote)
            } else {
                reactionManager.removeReaction(msg, event.user, event.reactionEmote.name)
            }

            val pending = reactionManager.pendingReactions(msg)
            Bot.logger.info("Remaining on ${msg.id}: $pending")
            if (pending >= ReactionManager.threshold) {
                Bot.logger.info(
                        "Hit threshold for message ${msg.id} clearing and re-adding reactions")
                reactionManager.removeAllReactions(msg) {
                    PollManager.addOptionReactions(category)
                }
            }
        }
    }
}