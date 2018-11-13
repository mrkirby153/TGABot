package com.mrkirby153.tgabot.polls

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.QueryBuilder
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.db.models.PollCategory
import com.mrkirby153.tgabot.logName
import me.mrkirby153.kcutils.createFileIfNotExist
import me.mrkirby153.kcutils.timing.Throttler
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.exceptions.ErrorResponseException
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.requests.ErrorResponse
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object PollResultHandler : ListenerAdapter() {

    val channelName = "vote-confirmation"
    val roleName = "Voted"

    val channelTextFile = File("votemessage").createFileIfNotExist()

    var channelMessage = channelTextFile.readLines().joinToString("\n")

    var tgaMid = ""
    var tgaChanId = ""
    var tgaRoleId = ""

    private const val MAILBOX = "\uD83D\uDCEB"

    private val dmThrottler = Throttler<User>(Consumer { u ->
        if (u == null) {
            return@Consumer
        }
        val channel = u.openPrivateChannel().complete()
        try {
            val results = buildVoteResults(u)
            var m = ""
            results.lines().forEach {
                if (m.length + it.length > 2000) {
                    channel.sendMessage(m).complete()
                    m = ""
                } else {
                    m += "$it\n"
                }
            }
            channel.sendMessage(m).complete()

            Bot.tgaGuild.getTextChannelById(tgaChanId).sendMessage(
                    "${u.asMention} I've sent a copy of your votes to your DMs!").queue {
                it.delete().queueAfter(30, TimeUnit.SECONDS)
            }
            Bot.adminLog.log(":mailbox_with_mail: Sent ${u.logName} their votes")
        } catch (e: Exception) {
            if (e is ErrorResponseException) {
                if (e.errorCode == ErrorResponse.CANNOT_SEND_TO_USER.code) {
                    Bot.adminLog.log(":warning: ${u.logName} has their DMs closed, cannot send DM")
                    Bot.tgaGuild.getTextChannelById(tgaChanId).sendMessage(
                            "${u.asMention} Please open your DMs!\n\nYou can open your DMs by right clicking on the server icon, clicking `Privacy Settings` and ensuring `Allow DMs from server members` is checked.").queue {
                        it.delete().queueAfter(15, TimeUnit.SECONDS)
                    }
                    return@Consumer
                }
            }
            Bot.adminLog.log(
                    ":rotating_light: An error occurred when sending DMs to ${u.logName}: ${e.javaClass.name} ${e.message}")
            Bot.tgaGuild.getTextChannelById(tgaChanId).sendMessage(
                    "${u.asMention} An unknown error occurred. Please report this to the moderators").queue {
                it.delete().queueAfter(30, TimeUnit.SECONDS)
            }
        }
    })

    fun updateMessage() {
        this.channelMessage = channelTextFile.readLines().joinToString("\n")
    }

    fun verifyConfiguration() {
        Bot.adminLog.log("Verifying configuration...")
        var channel = Bot.tgaGuild.getTextChannelsByName(channelName, true).firstOrNull()
        if (channel == null) {
            if (Bot.tgaGuild.selfMember.hasPermission(Permission.MANAGE_CHANNEL)) {
                Bot.logger.debug("Creating vote-confirmation channel")
                channel = Bot.tgaGuild.controller.createTextChannel(
                        channelName).complete() as TextChannel
            }
        }
        var role = Bot.tgaGuild.getRolesByName(roleName, false).firstOrNull()
        if (role == null) {
            if (Bot.tgaGuild.selfMember.hasPermission(Permission.MANAGE_ROLES)) {
                Bot.logger.debug("Creating voted role")
                role = Bot.tgaGuild.controller.createRole().setName(roleName).setPermissions(
                        0).complete()
            }
        }

        if (channel == null || role == null)
            return
        tgaChanId = channel.id
        tgaRoleId = role.id

        // Verify channel overrides exist

        val tgaBotOverride = channel.getPermissionOverride(channel.guild.selfMember)
                ?: channel.createPermissionOverride(channel.guild.selfMember).setAllow(
                        Permission.MESSAGE_READ, Permission.MESSAGE_WRITE,
                        Permission.MANAGE_PERMISSIONS).complete()
        if (Permission.MESSAGE_READ !in tgaBotOverride.allowed || Permission.MESSAGE_WRITE !in tgaBotOverride.allowed || Permission.MANAGE_PERMISSIONS !in tgaBotOverride.allowed) {
            tgaBotOverride.manager.grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE,
                    Permission.MANAGE_PERMISSIONS).queue()
        }

        val votedOverride = channel.getPermissionOverride(role)
                ?: channel.createPermissionOverride(role).setAllow(
                        Permission.MESSAGE_READ).complete()
        if (Permission.MESSAGE_READ !in votedOverride.allowed)
            votedOverride.manager.grant(Permission.MESSAGE_READ).queue()

        val everyoneOverride = channel.getPermissionOverride(channel.guild.publicRole)
                ?: channel.createPermissionOverride(channel.guild.publicRole).setDeny(
                        Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).complete()

        if (Permission.MESSAGE_READ !in everyoneOverride.denied || Permission.MESSAGE_WRITE !in everyoneOverride.denied)
            everyoneOverride.manager.deny(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).queue()

        // Retrieve the entire history of the channel
        val history = mutableListOf<Message>()

        channel.iterableHistory.forEach { message ->
            history.add(message)
        }

        val descriptionMsg = history.firstOrNull { it.contentRaw == channelMessage }
        if (descriptionMsg != null) {
            if (!descriptionMsg.isPinned)
                descriptionMsg.pin().queue()
            val toDelete = history.filter { it.id != descriptionMsg.id }
            descriptionMsg.addReaction(MAILBOX).queue()
            channel.purgeMessages(toDelete)
            tgaMid = descriptionMsg.id
        } else {
            channel.purgeMessages(history)
            channel.sendMessage(channelMessage).queue {
                it.pin().queue()
                it.addReaction(MAILBOX).queue()
                tgaMid = it.id
            }
        }
        Bot.adminLog.log("Configuration verified")
    }

    fun buildVoteResults(user: User): String {
        return buildString {
            appendln("Hello, ${user.name} here are your responses for the polls")
            appendln()
            val categories = Model.where(PollCategory::class.java, "guild", Bot.tgaGuildId).get()

            val responses = QueryBuilder().table("votes").where("user", user.id).whereIn(
                    "votes.category",
                    categories.map { it.id }.toTypedArray()).innerJoin("options", "votes.option",
                    "=", "options.id").select("votes.*", "options.name").query()

            categories.forEach { category ->
                val resp = responses.firstOrNull { it.getInt("category").toLong() == category.id }
                append("**${category.name}**: ")
                if (resp == null)
                    appendln("No vote yet!")
                else
                    appendln(resp.getString("name"))
            }

            appendln()
            appendln(
                    "If at any point you would like to change your vote, just click on the reactions again!")
        }
    }

    fun afterVote(user: User) {
        val role = Bot.tgaGuild.getRoleById(this.tgaRoleId)
        val member = Bot.tgaGuild.getMember(user)
        if (member == null || role == null)
            return
        if (role !in member.roles) {
            Bot.tgaGuild.controller.addRolesToMember(member, role).queue()
        }
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.member == event.guild.selfMember)
            return // Ignore messages from ourself
        if (event.messageId == tgaMid && event.reactionEmote.name == "\uD83D\uDCEB") {
            event.reaction.removeReaction(event.user).queueAfter(500, TimeUnit.MILLISECONDS)
            dmThrottler.trigger(event.user, 1, TimeUnit.MINUTES)
        }
    }
}