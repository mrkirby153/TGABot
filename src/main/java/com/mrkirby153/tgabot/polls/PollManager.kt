package com.mrkirby153.tgabot.polls

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import com.mrkirby153.botcore.command.CommandException
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.db.models.PollCategory
import com.mrkirby153.tgabot.db.models.PollOption
import com.mrkirby153.tgabot.db.models.PollVote
import com.mrkirby153.tgabot.findEmoteById
import com.mrkirby153.tgabot.findMessageById
import com.mrkirby153.tgabot.listener.PollListener
import com.mrkirby153.tgabot.logName
import com.mrkirby153.tgabot.redis.LeakyBucket
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.InputStream
import java.util.Random

object PollManager {

    val pollBucket = LeakyBucket("vote:%s", Bot.redis, 5, 10)

    fun onStartup() {
        // Loop through all the polls and ensure the reactions are in place as well as tally any offline votes
        Bot.logger.info("Verifying polls, this may take some time")
        Bot.adminLog.log("Verifying polls, this may take some time")
        val msgs = mutableMapOf<String, Message?>()
        val categories = Model.query(PollCategory::class.java).get()
        for (i in 0 until categories.size) {
            val category = categories[i]
            var shouldClear = false
            category.options.forEach { option ->
                val msg = msgs.computeIfAbsent(option.messageId) {
                    Bot.tgaGuild.findMessageById(it)
                } ?: return@forEach

                val r = msg.reactions.firstOrNull {
                    if (option.custom && it.reactionEmote.isEmote) {
                        it.reactionEmote.emote.id == option.reaction
                    } else {
                        it.reactionEmote.name == option.reaction
                    }
                }
                r?.users?.stream()?.filter { it.id != msg.guild.selfMember.user.id }?.forEach {
                    registerVote(it, category, option, false)
                    shouldClear = true
                }
            }
            if(shouldClear) {
                if (category.messageId != null) {
                    Bot.tgaGuild.findMessageById(category.messageId!!)?.clearReactions()?.queue {
                        PollManager.addOptionReactions(category)
                    }
                }
            } else {
                PollManager.addOptionReactions(category)
            }
            PollDisplayManager.update(category)
        }
        Bot.logger.debug("Poll verification complete!")
        Bot.adminLog.log("Polls verified")
    }

    fun registerVote(user: User, category: PollCategory, option: PollOption, log: Boolean = true): PollVote? {
        Bot.logger.debug("Registering vote for $user in category ${category.id}")

        Bot.redis.connection.use {
            val key = "${user.id}:${category.id}"
            if (pollBucket.incr(key) && (it.get("lv_$key")
                            ?: "0").toLong() + (10 * 1000) < System.currentTimeMillis()) {
                it.setex("lv_$key", 60, System.currentTimeMillis().toString())
                Bot.adminLog.log(
                        ":warning: ${user.logName} has voted on the category **${category.name}** too quickly")
            }
        }


        val existingVote = Model.where(PollVote::class.java, "category", category.id).where("user",
                user.id).first()
        if (existingVote != null) {
            if(log) {
                Bot.adminLog.log("${user.logName} changed vote on **${category.name}**")
            }
            Bot.logger.debug(
                    "$user has voted before (${existingVote.option}) changing to ${option.id}")
            existingVote.option = option.id
            existingVote.save()
            PollDisplayManager.updateDebounced(category)
            return existingVote
        }

        val vote = PollVote()
        vote.category = category.id
        vote.option = option.id
        vote.user = user.id
        vote.save()
        PollDisplayManager.updateDebounced(category)
        PollResultHandler.afterVote(user)
        if(log) {
            Bot.adminLog.log(
                    "${user.logName} has voted on **${category.name}**")
        }
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
        category.options.filter { it.id.toInt() !in results.map { it.id } }.forEach { o ->
            results.add(VoteResult(o.id.toInt(), 0, o))
        }
        return results
    }

    fun addOption(category: PollCategory, channel: TextChannel, messageId: String,
                  emote: String, name: String): PollOption {
        val emoteRegex = Regex("<a?:.*:([0-9]*)>")
        val option = PollOption()
        option.category = category.id
        option.channelId = channel.id
        option.messageId = messageId
        option.name = name
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

    fun import(stream: InputStream, guild: Guild) {
        val rootObj = JSONObject(JSONTokener(stream))
        val categories = (rootObj.get("categories") as JSONArray).map { it as JSONObject }
        val options = rootObj.get("options") as JSONObject


        val pollCategories = mutableListOf<PollCategory>()
        categories.forEach { c ->
            Bot.logger.info("Importing ${c.getInt("id")}")
            val cat = PollCategory()
            cat.name = c.getString("name")
            cat.guild = guild.id
            cat.channel = c.getString("channel")
            cat.save()
            pollCategories.add(cat)
            PollDisplayManager.update(cat)
            val msg = guild.findMessageById(cat.messageId!!)!!
            options.getJSONArray(c.getInt("id").toString()).map { it as JSONObject }.forEach {
                addOption(cat, msg.channel as TextChannel, msg.id, it.getString("emoji"),
                        it.getString("name"))
            }
        }
        pollCategories.forEach { PollDisplayManager.update(it) }
    }

    fun addOptionReactions(category: PollCategory) {
        val chan = Bot.jda.getGuildById(category.guild).getTextChannelById(category.channel)
        chan.getMessageById(category.messageId).queue { msg ->
            category.options.forEach { opt ->
                if (opt.custom) {
                    msg.addReaction(findEmoteById(opt.reaction)).queue()
                } else {
                    msg.addReaction(opt.reaction).queue()
                }
            }
        }
    }

    data class VoteResult(val id: Int, val count: Long, val option: PollOption)
}