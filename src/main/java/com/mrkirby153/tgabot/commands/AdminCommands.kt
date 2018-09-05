package com.mrkirby153.tgabot.commands

import com.mrkirby153.botcore.command.Command
import com.mrkirby153.botcore.command.Context
import com.mrkirby153.botcore.command.args.CommandContext
import com.mrkirby153.tgabot.Bot

class AdminCommands {

    @Command(name = "shutdown", clearance = 100)
    fun shutdown(context: Context, cmdContext: CommandContext){
        context.channel.sendMessage("Shutting down...").queue {
            Bot.bot.shutdownAll()
        }
    }
}