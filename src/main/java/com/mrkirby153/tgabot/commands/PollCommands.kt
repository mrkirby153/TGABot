package com.mrkirby153.tgabot.commands

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.botcore.command.Command
import com.mrkirby153.botcore.command.CommandException
import com.mrkirby153.botcore.command.Context
import com.mrkirby153.botcore.command.args.CommandContext
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.GREEN_CHECK
import com.mrkirby153.tgabot.RED_CROSS
import com.mrkirby153.tgabot.db.models.PollCategory
import com.mrkirby153.tgabot.db.models.PollOption
import com.mrkirby153.tgabot.findEmoteById
import com.mrkirby153.tgabot.polls.PollManager
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate


class PollCommands {

    @Command(name = "category create", parent = "poll", arguments = ["<name:string...>"],
            clearance = 100)
    fun addCategory(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.getNotNull<String>("name").toLowerCase()

        if (Model.where(PollCategory::class.java, "name", name).first() != null) {
            throw CommandException("A category already exists with that name!")
        }

        val category = PollCategory()
        category.name = name
        category.save()
        context.channel.sendMessage("Created category `$name` with id **${category.id}**").queue()
    }

    @Command(name = "category delete", parent = "poll", arguments = ["<id:int>"], clearance = 100)
    fun deleteCategory(context: Context, cmdContext: CommandContext) {
        val cat = Model.where(PollCategory::class.java, "id", cmdContext.getNotNull("id")).first()
                ?: throw CommandException("Category not found!")
        if (cat.options.firstOrNull { it.votes.size > 0 } != null) {
            val m = context.channel.sendMessage(
                    ":warning: This category has options with responses. Are you sure you want to delete it? It can't be undone").complete()
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
                    cat.delete()
                    m.editMessage(GREEN_CHECK).queue()
                }
            }, 10, TimeUnit.SECONDS, Runnable {
                m.editMessage("Aborted!").queue()
            })
            return
        }
        context.channel.sendMessage("Deleted category `${cat.name}`").queue()
        cat.delete()
    }

    @Command(name = "verify", parent = "poll", clearance = 100)
    fun verify(context: Context, cmdContext: CommandContext) {
        val msg = context.channel.sendMessage(":timer: Verifying polls").complete()
        PollManager.onStartup()
        msg.editMessage(":ballot_box_with_check: Polls verified successfully").queue()
    }

    @Command(name = "gwinner", parent = "poll", clearance = 100)
    fun globalWinner(context: Context, cmdContext: CommandContext){
        val winnerId = PollManager.globalWinner()
        context.channel.sendMessage(":tada: The winner is <@$winnerId> (`$winnerId`) :tada:").queue()
    }

    @Command(name = "option remove", parent = "poll",
            arguments = ["<option:int>"], clearance = 100)
    fun deletePollOption(context: Context, cmdContext: CommandContext) {
        val option = Model.query(PollOption::class.java).where("id",
                cmdContext.getNotNull("option")).first() ?: throw CommandException(
                "Not a valid option")

        if (option.votes.size > 0) {
            val m = context.channel.sendMessage(
                    ":warning: This option has **${option.votes.size}** votes are you sure you want to delete it? This can't be undone").complete()
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
                    PollManager.removeOption(option)
                    m.editMessage(GREEN_CHECK).queue()
                }
            }, 10, TimeUnit.SECONDS, Runnable {
                m.editMessage("Aborted!").queue()
            })
            return
        }
        PollManager.removeOption(option)
        context.channel.sendMessage("Deleted option **${option.id}**").queue()
    }

    @Command(name = "tally", parent = "poll", arguments = ["<category:int>"], clearance = 100)
    fun pollResults(context: Context, cmdContext: CommandContext) {
        val category = Model.where(PollCategory::class.java, "id",
                cmdContext.getNotNull("category")).first() ?: throw CommandException(
                "Invalid category!")
        val results = PollManager.tallyVotes(category)

        context.channel.sendMessage(buildString {
            appendln("**Results for `${category.name}`**")
            appendln()
            results.forEach {
                if (it.option.custom) {
                    val e = findEmoteById(it.option.reaction)
                    if (e != null) {
                        val a = if (e.isAnimated) "a" else ""
                        appendln(" - <$a:${e.name}:${e.id}> — **${it.count} votes**")
                    } else {
                        appendln(" - `${it.option.reaction}` — **${it.count} votes**")
                    }
                } else {
                    appendln(" - ${it.option.reaction} – **${it.count} votes**")
                }
            }
        }).queue()
    }

    @Command(name = "option add", parent = "poll",
            arguments = ["<category:int>", "<channelid:string>", "<mid:string>", "<emoji:string>"],
            clearance = 100)
    fun addPollOption(context: Context, cmdContext: CommandContext) {
        val category = Model.where(PollCategory::class.java, "id",
                cmdContext.getNotNull("category")).first() ?: throw CommandException(
                "Invalid category!")
        val emoji = cmdContext.getNotNull<String>("emoji")
        val msgId = cmdContext.getNotNull<String>("mid")
        val option = PollManager.addOption(category,
                context.guild.getTextChannelById(cmdContext.getNotNull<String>("channelid")), msgId,
                emoji)

        context.channel.sendMessage(
                "Added $emoji as an option for category `${category.id}` with id **${option.id}**").queue()
    }

    @Command(name = "options import", parent = "poll", clearance = 100,
            arguments = ["<category:int>", "<cid:string>", "<mid:string>"])
    fun importReactions(context: Context, cmdContext: CommandContext) {
        val category = Model.where(PollCategory::class.java, "id",
                cmdContext.getNotNull("category")).first() ?: throw CommandException(
                "Invalid category")

        val msg = context.guild.getTextChannelById(
                cmdContext.getNotNull<String>("cid")).getMessageById(
                cmdContext.getNotNull<String>("mid")).complete()

        val emotes = mutableListOf<String>()

        msg.reactions.forEach {
            if (it.reactionEmote.isEmote) {
                emotes.add(it.reactionEmote.emote.asMention)
            } else {
                emotes.add(it.reactionEmote.name)
            }
        }
        msg.clearReactions().queue {
            emotes.forEach { emote ->
                PollManager.addOption(category, msg.channel as TextChannel, msg.id, emote)
            }
        }

        context.channel.sendMessage(buildString {
            appendln(
                    "Imported ${emotes.size} emotes into category **${category.id}**: ${emotes.joinToString(
                            ", ")}")
        }).queue()
    }
}