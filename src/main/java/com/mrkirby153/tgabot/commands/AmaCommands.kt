package com.mrkirby153.tgabot.commands

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.botcore.command.Command
import com.mrkirby153.botcore.command.Context
import com.mrkirby153.botcore.command.args.CommandContext
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.ama.AmaManager
import com.mrkirby153.tgabot.db.models.AmaQuestion
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate

class AmaCommands {

    @Command(name = "ama", arguments = ["<question:string...>"], clearance = 0)
    fun submitAmaQuestion(context: Context, cmdContext: CommandContext) {
        val question = cmdContext.get<String>("question")

        if (question == null) {
            context.channel.sendMessage(
                    "${context.author.asMention} Please provide a question to ask!").queue {
                it.delete().queueAfter(10, TimeUnit.SECONDS) {
                    context.delete().queue()
                }
            }
            return
        }

        val response = AmaManager.submitQuestion(context.author, question)
        if (response.response == AmaManager.AmaSubmitResponse.Responses.THROTTLED) {
            context.channel.sendMessage(
                    "${context.author.asMention} You're doing that too fast! You can only submit 1 question every 5 minutes.").queue {
                it.delete().queueAfter(10, TimeUnit.SECONDS) {
                    context.delete().queue()
                }
            }
            return
        }
        if (response.response == AmaManager.AmaSubmitResponse.Responses.UNKNOWN_ERROR) {
            context.channel.sendMessage(
                    "${context.author.asMention} An unknown error occurred. Please contact the mods for assistance.").queue {
                it.delete().queueAfter(10, TimeUnit.SECONDS) {
                    context.delete().queue()
                }
            }
        }

        context.channel.sendMessage(
                "${context.author.asMention} Your question has been submitted!").queue {
            it.delete().queueAfter(10, TimeUnit.SECONDS) {
                context.delete().queue()
            }
        }
    }

    @Command(name = "reset", parent = "ama", clearance = 100)
    fun resetAma(context: Context, cmdContext: CommandContext) {
        context.channel.sendMessage(
                ":warning: This will delete all questions submitted. Are you sure you want to do this?").queue()
        Bot.waiter.waitFor(MessageReceivedEvent::class.java,
                Predicate { it.channel.id == context.channel.id && it.author == context.author },
                Consumer {
                    when (it.message.contentRaw) {
                        "yes" -> {
                            val questions = Model.get(AmaQuestion::class.java)
                            val toDelete = questions.filter { q -> !q.denied }
                            AmaManager.amaChannel.purgeMessagesById(toDelete.map { it.messageId })
                            Model.query(AmaQuestion::class.java).delete()
                            context.channel.sendMessage(
                                    "Success. `${questions.size}` questions deleted")
                        }
                        else -> {
                            context.channel.sendMessage("`yes` not received. Aborting").queue()
                        }
                    }
                }, onTimeout = Runnable {
            context.channel.sendMessage("Timed out. Aborting").queue()
        })
    }
}