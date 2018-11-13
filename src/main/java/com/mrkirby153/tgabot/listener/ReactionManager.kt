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
                var callback: (() -> Unit)? = null
                synchronized(queue) {
                    if (queue.peek() != null) {
                        val task = queue.peek() ?: return@synchronized
                        callback = task.callback
                        task.ra.complete()
                        if (task.type == "all") {
                            val start = queue.size
                            Bot.logger.info("Removing all reactions on ${task.msg}")
                            // Remove all pending reaction removals
                            queue.removeIf { it.msg == task.msg }
                            val end = queue.size
                            Bot.logger.info("\tRemoved ${start - end} pending tasks")
                        }
                        queue.remove(task)
                    }
                }
                callback?.invoke()
            } catch (e: Exception) {
                Bot.logger.error("Encountered an error", e)
            }
            Thread.sleep(1)
        }
    }

    fun removeReaction(msg: Message, user: User, reaction: String, callback: (() -> Unit)? = null) {
        synchronized(queue) {
            val ra = msg.reactions.firstOrNull {
                !it.reactionEmote.isEmote && it.reactionEmote.name == reaction
            }?.removeReaction(user) ?: return
            queue.add(RemoveReactionTask("msg", ra, msg.id, callback))
        }
    }

    fun removeReaction(msg: Message, user: User, reaction: Emote, callback: (() -> Unit)? = null) {
        synchronized(queue) {
            val ra = msg.reactions.firstOrNull {
                it.reactionEmote.isEmote && it.reactionEmote.emote == reaction
            }?.removeReaction(user) ?: return
            queue.add(RemoveReactionTask("msg", ra, msg.id, callback))
        }
    }

    fun removeAllReactions(msg: Message, callback: (() -> Unit)? = null) {
        // Queue up the all removal task next
        synchronized(queue) {
            queue.addFirst(RemoveReactionTask("all", msg.clearReactions(), msg.id, callback))
        }
    }

    fun pendingReactions(msg: Message): Int {
        synchronized(queue) {
            return this.queue.count { it.msg == msg.id }
        }
    }

    fun queueSize(): Int {
        synchronized(queue) { return this.queue.size }
    }

    fun shutdown(waitFor: Boolean = false) {
        this.running = false
        if (waitFor)
            try {
                this.thread.join(500)
            } catch (e: Exception) {
                // Ignore
            }
    }

    fun getQueue(print: Boolean = false): String {
        val queue = this.queue.joinToString(", ") { it.type + ":" + it.msg }
        if (print) {
            Bot.logger.info("QUEUE: $queue")
        }
        return queue
    }

    data class RemoveReactionTask(val type: String, val ra: RestAction<*>, val msg: String,
                                  val callback: (() -> Unit)?)
}