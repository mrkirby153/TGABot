package com.mrkirby153.tgabot.listener

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

    init {
        thread.name = "ReactionManager"
        thread.isDaemon = true
        thread.start()
    }

    private val queue = LinkedList<RemoveReactionTask>()

    override fun run() {
        while (true) {
            if (queue.peek() != null) {
                val task = queue.peek() ?: continue
                task.ra.complete()
                queue.remove(task)
                task.callback?.invoke()
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
        queue.removeIf { it.msg == msg.id && it.type != "all" } // Remove any pending reactions
        queue.addFirst(RemoveReactionTask("all", msg.clearReactions(), msg.id, callback))
    }

    fun pendingReactions(msg: Message): Int {
        return this.queue.count { it.msg == msg.id }
    }

    data class RemoveReactionTask(val type: String, val ra: RestAction<*>, val msg: String,
                                  val callback: (() -> Unit)?)
}