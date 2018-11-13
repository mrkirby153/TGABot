package com.mrkirby153.tgabot.listener

import com.mrkirby153.tgabot.Bot
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.requests.RestAction
import java.util.LinkedList

class ReactionManager : Runnable {

    companion object {
        var threshold = 10
    }

    private val thread = Thread(this)

    private var running = true

    init {
        thread.name = "ReactionManager"
        thread.isDaemon = true
        thread.start()
    }

    private val queue = LinkedList<RemoveReactionTask>()

    override fun run() {
        while (running) {
            try {
                if (queue.peek() != null) {
                    val task = queue.peek() ?: continue
                    if (task.type == "all") {
                        Bot.logger.debug("Removing all reactions on ${task.msg}")
                        // Remove all pending reaction removals
                        queue.removeIf { it.msg == task.msg && it.type != "all" }
                    }
                    task.ra.complete()
                    queue.remove(task)
                    task.callback?.invoke()
                }
            } catch (e: Exception) {
                Bot.logger.error("Encountered an error", e)
            }
            Thread.sleep(1)
        }
    }

    fun removeReaction(msg: Message, user: User, reaction: String, callback: (() -> Unit)? = null) {
        val ra = msg.reactions.firstOrNull {
            !it.reactionEmote.isEmote && it.reactionEmote.name == reaction
        }?.removeReaction(user) ?: return
        queue.add(RemoveReactionTask("msg_${msg.id}", ra, msg.id, callback))
    }

    fun removeReaction(msg: Message, user: User, reaction: Emote, callback: (() -> Unit)? = null) {
        val ra = msg.reactions.firstOrNull {
            it.reactionEmote.isEmote && it.reactionEmote.emote == reaction
        }?.removeReaction(user) ?: return
        queue.add(RemoveReactionTask("msg_${msg.id}", ra, msg.id, callback))
    }

    fun removeAllReactions(msg: Message, callback: (() -> Unit)? = null) {
        // Queue up the all removal task next
        queue.addFirst(RemoveReactionTask("all", msg.clearReactions(), msg.id, callback))
    }

    fun pendingReactions(msg: Message): Int {
        return this.queue.count { it.msg == msg.id }
    }

    fun queueSize(): Int = this.queue.size

    fun shutdown(waitFor: Boolean = false) {
        this.running = false
        if (waitFor)
            try {
                this.thread.join(500)
            } catch (e: Exception) {
                // Ignore
            }
    }

    data class RemoveReactionTask(val type: String, val ra: RestAction<*>, val msg: String,
                                  val callback: (() -> Unit)?)
}