package com.mrkirby153.tgabot.logger

import com.mrkirby153.tgabot.Bot
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.exceptions.RateLimitedException
import java.text.SimpleDateFormat
import java.util.LinkedList

class LogPump(private val targetChannel: TextChannel, private val sleepDuration: Long = 5000) :
        Runnable {

    private val queued = LinkedList<String>()

    var running = true

    private val thread: Thread = Thread(this)

    private var quietPeriod = -1L

    init {
        thread.name = "LogPump/#${targetChannel.name}"
        thread.priority = Thread.MAX_PRIORITY
        thread.isDaemon = true
    }

    fun start() {
        this.thread.start()
    }

    override fun run() {
        Bot.logger.debug(
                "Starting log pump to #${targetChannel.name}")
        while (running) {
            val msg = buildString {
                while (queued.isNotEmpty()) {
                    if (queued.peek().length + length > 1990)
                        return@buildString
                    val msg = queued.pop()
                    appendln(msg)
                }
            }
            if (msg.isEmpty() || msg.isBlank())
                continue
            Bot.logger.debug("Logging message")
            try {
                targetChannel.sendMessage(msg).complete(false)
            } catch (e: RateLimitedException) {
                Bot.logger.debug("Got ratelimited entering quiet period")
                // We got ratelimited, batch messages for the next 60 seconds
                quietPeriod = System.currentTimeMillis() + 60000
            }
            if (quietPeriod != -1L) {
                if (quietPeriod < System.currentTimeMillis())
                    quietPeriod = -1L
                Thread.sleep(this.sleepDuration)
            } else {
                Thread.yield()
            }
        }
    }

    fun log(msg: String) {
        val sdf = SimpleDateFormat("HH:mm:ss")
        val msgAndTimestamp = "`[${sdf.format(System.currentTimeMillis())}]` $msg"
        if (msgAndTimestamp.length > 1999) {
            Bot.logger.warn("dropping message \"$msg\" as it's too long")
            return
        }
        queued.add(msgAndTimestamp)
    }

}