package com.mrkirby153.tgabot.polls

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import com.mrkirby153.botcore.command.CommandException
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.db.models.PollCategory
import com.mrkirby153.tgabot.db.models.PollOption
import com.mrkirby153.tgabot.db.models.PollVote
import com.mrkirby153.tgabot.findEmoteById
import com.mrkirby153.tgabot.listener.PollListener
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.util.Random

object PollManager {


    fun onStartup() {
        // Loop through all the polls and ensure the reactions are in place as well as tally any offline votes
        Bot.logger.debug("Verifying polls")
        val msgs = mutableMapOf<String, Message?>()
        val categories = Model.get(PollCategory::class.java)
        for (i in 0 until categories.size) {
            val category = categories[i]
            category.options.forEach { option ->
                val msg = msgs.computeIfAbsent(option.messageId) {
                    for (j in 0 until Bot.jda.guilds.size) {
                        val guild = Bot.jda.guilds[j]
                        val tc = guild.getTextChannelById(option.channelId)
                        val r = tc?.getMessageById(option.messageId)?.complete()
                        if (r != null)
                            return@computeIfAbsent r
                    }
                    null
                } ?: return@forEach

                val r = msg.reactions.firstOrNull {
                    if (option.custom && it.reactionEmote.isEmote) {
                        it.reactionEmote.emote.id == option.reaction
                    } else {
                        it.reactionEmote.name == option.reaction
                    }
                }
                r?.users?.stream()?.filter { it.id != msg.guild.selfMember.user.id }?.forEach {
                    registerVote(it, category, option)
                    r.removeReaction(it).queue()
                }
                if (option.custom)
                    msg.addReaction(findEmoteById(option.reaction)).queue()
                else
                    msg.addReaction(option.reaction).queue()
            }
        }
    }

    fun registerVote(user: User, category: PollCategory, option: PollOption): PollVote? {
        Bot.logger.debug("Registering vote for $user in category ${category.id}")

        val existingVote = Model.where(PollVote::class.java, "category", category.id).where("user",
                user.id).first()
        if (existingVote != null) {
            Bot.logger.debug(
                    "$user has voted before (${existingVote.option}) changing to ${option.id}")
            existingVote.option = option.id
            existingVote.save()
            return existingVote
        }

        val vote = PollVote()
        vote.category = category.id
        vote.option = option.id
        vote.user = user.id
        vote.save()
        return vote
    }

    fun tallyVotes(category: PollCategory): List<VoteResult> {
        val rows = DB.getResults(
                "SELECT options.id as id, COUNT(*) as `count`, `option` FROM votes LEFT JOIN  options ON options.id = `option` WHERE votes.category = ? GROUP BY `option` ORDER BY count DESC",
                category.id)
        val results = mutableListOf<VoteResult>()

        val options = mutableMapOf<Int, PollOption>()
        rows.forEach { row ->
            results.add(VoteResult(row.getInt("id"), row.getLong("count"),
                    options.computeIfAbsent(row.getInt("option")) {
                        Model.where(PollOption::class.java, "id", it).first()
                    }))
        }
        return results
    }

    fun addOption(category: PollCategory, channel: TextChannel, messageId: String,
                  emote: String): PollOption {
        val emoteRegex = Regex("<a?:.*:([0-9]*)>")
        val option = PollOption()
        option.category = category.id
        option.channelId = channel.id
        option.messageId = messageId
        if (emoteRegex.matches(emote)) {
            // Custom emote
            val emoteId = emoteRegex.find(emote)?.groups?.get(1)?.value ?: throw CommandException(
                    "Regex match failed")
            option.reaction = emoteId
            option.custom = true

            if (findEmoteById(emoteId) == null)
                throw CommandException("Cannot use `$emote` for polls")

            channel.getMessageById(messageId).complete()?.addReaction(
                    findEmoteById(emoteId))?.queue()
        } else {
            option.reaction = emote
            option.custom = false
            channel.getMessageById(messageId).complete()?.addReaction(emote)?.queue()
        }
        option.save()
        PollListener.optionCache.refresh(messageId)
        PollListener.msgCache.invalidate(messageId)
        return option
    }

    fun removeOption(option: PollOption) {
        val msg = option.message
        msg?.reactions?.filter { if (option.custom) it.reactionEmote.id == option.reaction else it.reactionEmote.name == option.reaction }?.forEach { r ->
            r.users.forEach {
                r.removeReaction(it).queue()
            }
        }
        option.delete()
    }

    fun removeOption(id: Int) = removeOption(Model.where(PollOption::class.java, "id", id).first())

    fun globalWinner(): String {
        val rows = DB.getFirstColumnValues<String>("SELECT DISTINCT user FROM votes")
        val random = Random()

        return rows[random.nextInt(rows.size)]
    }

    data class VoteResult(val id: Int, val count: Long, val option: PollOption)
}