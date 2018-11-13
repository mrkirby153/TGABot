package com.mrkirby153.tgabot.commands

import com.mrkirby153.botcore.command.Command
import com.mrkirby153.botcore.command.Context
import com.mrkirby153.botcore.command.args.CommandContext
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.listener.PollListener
import com.mrkirby153.tgabot.listener.ReactionManager
import com.mrkirby153.tgabot.polls.PollManager
import me.mrkirby153.kcutils.Time

class AdminCommands {

    @Command(name = "shutdown", clearance = 100)
    fun shutdown(context: Context, cmdContext: CommandContext) {
        context.channel.sendMessage("Shutting down...").queue {
            Bot.shutdown()
        }
    }

    @Command(name = "reaction-threshold", clearance = 100, arguments = ["[num:int]"])
    fun reactionThreshold(context: Context, cmdContext: CommandContext) {
        if (cmdContext.has("num")) {
            ReactionManager.threshold = cmdContext.getNotNull("num")
            Bot.adminLog.log("Reaction clear threshold set to ${ReactionManager.threshold}")
            context.channel.sendMessage("Updated threshold to ${ReactionManager.threshold}").queue()
        } else {
            context.channel.sendMessage("Current threshold: ${ReactionManager.threshold}").queue()
        }
    }

    @Command(name = "spam-alert", clearance = 100, arguments = ["<count:int>", "<period:int>"])
    fun spamAlert(context: Context, cmdContext: CommandContext) {
        val count = cmdContext.getNotNull<Int>("count")
        val period = cmdContext.getNotNull<Int>("period")
        PollManager.pollBucket.count = count
        PollManager.pollBucket.period = period
        Bot.adminLog.log(
                "${context.author.asMention} updated the alert threshold to $count in $period. This may trigger some false positives")
        context.channel.sendMessage("Spam alert threshold updated to $count/$period").queue()
    }

    @Command(name = "ping", parent = "pollbot", clearance = 100)
    fun pingCommand(context: Context, cmdContext: CommandContext) {
        val t = System.currentTimeMillis()
        context.channel.sendTyping().queue {
            context.channel.sendMessage(
                    ":ping_pong: Pong! ${Time.format(1, System.currentTimeMillis() - t)}").queue()
        }
    }

    @Command(name = "stats", parent = "pollbot", clearance = 100)
    fun reactionManagerStats(context: Context, cmdContext: CommandContext) {
        val v = "Queue: `${PollListener.reactionManager.getQueue(true)}`"
        context.channel.sendMessage(
                "There are `${PollListener.reactionManager.queueSize()}` pending reaction removals").queue()
        if(v.length < 2000)
            context.channel.sendMessage(v).complete()
    }
}