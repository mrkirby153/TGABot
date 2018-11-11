package com.mrkirby153.tgabot.commands

import com.mrkirby153.botcore.command.Command
import com.mrkirby153.botcore.command.Context
import com.mrkirby153.botcore.command.args.CommandContext
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.listener.ReactionManager
import com.mrkirby153.tgabot.polls.PollManager

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

    @Command(name = "spam-alert", clearance = 100, arguments = ["<count:int>",  "<period:int>"])
    fun spamAlert(context: Context, cmdContext: CommandContext) {
        val count = cmdContext.getNotNull<Int>("count")
        val period = cmdContext.getNotNull<Int>("period")
        PollManager.pollBucket.count = count
        PollManager.pollBucket.period = period
        Bot.adminLog.log(
                "${context.author.asMention} updated the alert threshold to $count in $period. This may trigger some false positives")
        context.channel.sendMessage("Spam alert threshold updated to $count/$period").queue()
    }
}