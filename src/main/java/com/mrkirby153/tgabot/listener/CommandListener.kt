package com.mrkirby153.tgabot.listener

import com.mrkirby153.tgabot.Bot
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class CommandListener : ListenerAdapter() {

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        Bot.executor.submit {
            Bot.commands.execute(event.message)
        }
    }
}