package com.mrkirby153.tgabot.listener

import net.dv8tion.jda.core.entities.Message
import java.util.concurrent.ScheduledFuture

class ReactionManager {

    companion object {
        var threshold = 10
    }

    private val queuedReactions = mutableMapOf<String, MutableList<ScheduledFuture<*>>>()

    fun enqueue(message: Message, future: ScheduledFuture<*>) {
        val l = queuedReactions.computeIfAbsent(message.id) { mutableListOf() }
        // Remove tasks that are done
        l.removeIf { it.isDone }
        l.add(future)
    }

    fun getToClear(): List<String> {
        return queuedReactions.filter { it.value.size >= threshold }.keys.toList()
    }

    fun cancelAll(message: Message) {
        queuedReactions[message.id]?.forEach {
            it.cancel(true)
        }
    }
}