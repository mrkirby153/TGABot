package com.mrkirby153.tgabot.commands

import com.mrkirby153.botcore.command.Command
import com.mrkirby153.botcore.command.Context
import com.mrkirby153.botcore.command.args.CommandContext
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.listener.ReactionManager

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
            context.channel.sendMessage("Updated threshold to ${ReactionManager.threshold}").queue()
        } else {
            context.channel.sendMessage("Current threshold: ${ReactionManager.threshold}").queue()
        }
    }
}