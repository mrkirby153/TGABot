package com.mrkirby153.tgabot.commands

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.botcore.command.Command
import com.mrkirby153.botcore.command.Context
import com.mrkirby153.botcore.command.args.CommandContext
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.GREEN_CHECK
import com.mrkirby153.tgabot.RED_CROSS
import com.mrkirby153.tgabot.ama.AmaManager
import com.mrkirby153.tgabot.db.models.AmaQuestion
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
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
        val m = context.channel.sendMessage(
                ":warning: This will delete all questions submitted. Are you sure you want to do this?").complete()
        m.addReaction(GREEN_CHECK).queue {
            m.addReaction(RED_CROSS).queue()
        }
        Bot.waiter.waitFor(MessageReactionAddEvent::class.java, Predicate {
            it.messageId == m.id && it.user.id == context.author.id && (it.reactionEmote.name == RED_CROSS || it.reactionEmote.name == GREEN_CHECK)
        }, Consumer {
            if (it.reactionEmote.name == RED_CROSS) {
                m.editMessage("Canceled!").queue {
                    m.delete().queueAfter(10, TimeUnit.SECONDS)
                }
            } else if (it.reactionEmote.name == GREEN_CHECK) {
                val questions = Model.query(AmaQuestion::class.java).get()
                val toDelete = questions.filter { q -> !q.denied }
                AmaManager.amaChannel.purgeMessagesById(toDelete.map { it.messageId })
                Model.query(AmaQuestion::class.java).delete()
                context.channel.sendMessage(
                        "Success. `${questions.size}` questions deleted")
                m.editMessage(GREEN_CHECK).queue()
            }
        }, 10, TimeUnit.SECONDS, Runnable {
            m.editMessage("Aborted!").queue()
        })
    }
}