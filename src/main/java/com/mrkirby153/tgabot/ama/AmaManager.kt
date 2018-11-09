package com.mrkirby153.tgabot.ama

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.db.models.AmaQuestion
import com.mrkirby153.tgabot.findEmoteById
import me.mrkirby153.kcutils.timing.Throttler
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

object AmaManager {

    const val GREEN_CHECK_ID = "414875062001205249"
    const val RED_X_ID = "414875062336880640"

    private val chanId = Bot.properties.getProperty("ama-channel")

    private val amaThrottler = Throttler<User>(null)

    private val greenTickEmote by lazy {
        findEmoteById(GREEN_CHECK_ID) ?: throw IllegalStateException(
                "Could not find green check emote")
    }

    private val redXEmote by lazy {
        findEmoteById(RED_X_ID) ?: throw IllegalArgumentException("Could not find red X emote")
    }

    val amaChannel by lazy {
        Bot.tgaGuild.getTextChannelById(this.chanId) ?: throw IllegalStateException(
                "Could not get the AMA channel")
    }

    fun submitQuestion(submitter: User, question: String): AmaSubmitResponse {
        try {
            if (amaThrottler.throttled(submitter))
                return AmaSubmitResponse(AmaSubmitResponse.Responses.THROTTLED, null)
            amaThrottler.trigger(submitter, 5, TimeUnit.MINUTES)

            val amaQuestion = AmaQuestion()
            amaQuestion.submitter = submitter
            amaQuestion.question = question
            amaQuestion.save()

            amaChannel.sendMessage(buildString {
                appendln("----------------------------")
                appendln("**ID:** ${amaQuestion.id}")
                appendln(
                        "**Submitted By:** ${submitter.name}#${submitter.discriminator} (`${submitter.id}`)")
                appendln()
                appendln(question)
            }).queue { m ->
                amaQuestion.messageId = m.id
                amaQuestion.save()
                m.addReaction(greenTickEmote).queue()
                m.addReaction(redXEmote).queue()
            }
            return AmaSubmitResponse(AmaSubmitResponse.Responses.SUCCESS, amaQuestion)
        } catch (e: Exception) {
            e.printStackTrace()
            return AmaSubmitResponse(AmaSubmitResponse.Responses.UNKNOWN_ERROR, null)
        }
    }

    fun approveQuestion(amaQuestion: AmaQuestion) {
        Bot.logger.debug("Approving question ${amaQuestion.id}")
        amaQuestion.approved = true
        amaQuestion.save()
        // TODO 11/8/18 Put it somewhere??
    }

    fun denyQuestion(amaQuestion: AmaQuestion) {
        Bot.logger.debug("Denying question ${amaQuestion.id}")
        amaQuestion.denied = true
        amaQuestion.save()
        amaChannel.deleteMessageById(amaQuestion.messageId).queueAfter(250, TimeUnit.MILLISECONDS)
    }

    class AmaSubmitResponse(val response: Responses, val question: AmaQuestion?) {

        enum class Responses {
            THROTTLED,
            UNKNOWN_ERROR,
            SUCCESS
        }
    }

    class Listener : ListenerAdapter() {

        override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
            if (event.member == event.guild.selfMember)
                return

            if (event.channel.id != chanId)
                return
            val amaQuestion = Model.query(AmaQuestion::class.java).where("message_id",
                    event.messageId).first() ?: return

            when (event.reactionEmote.emote) {
                redXEmote -> denyQuestion(amaQuestion)
                greenTickEmote -> approveQuestion(amaQuestion)
                else -> event.reaction.removeReaction(event.user).queue()
            }
        }
    }
}