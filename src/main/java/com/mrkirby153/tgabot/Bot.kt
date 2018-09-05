package com.mrkirby153.tgabot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.mrkirby153.bfs.ConnectionFactory
import com.mrkirby153.bfs.sql.QueryBuilder
import com.mrkirby153.botcore.command.CommandExecutor
import com.mrkirby153.botcore.event.EventWaiter
import com.mrkirby153.botcore.shard.ShardManager
import com.mrkirby153.tgabot.commands.AdminCommands
import com.mrkirby153.tgabot.commands.PollCommands
import com.mrkirby153.tgabot.db.SchemaManager
import com.mrkirby153.tgabot.listener.CommandListener
import com.mrkirby153.tgabot.listener.PollListener
import com.mrkirby153.tgabot.polls.PollManager
import net.dv8tion.jda.core.JDA
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Properties

object Bot {

    lateinit var bot: ShardManager
    lateinit var commands: CommandExecutor
    lateinit var waiter: EventWaiter

    @JvmStatic
    val logger = LoggerFactory.getLogger("TGABot")

    val properties = Properties().apply {
        val `is` = File("config.properties").inputStream()
        load(`is`)
        `is`.close()
    }
    val admins = File("admins").readLines()
    private val logLevel = Level.toLevel(properties.getProperty("log-level"), Level.INFO)

    val jda: JDA
        get() = bot.getShard(0)

    @JvmStatic
    fun main(args: Array<String>) {
        (logger as? Logger)?.let {
            it.level = logLevel
        }
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as? Logger)?.level = logLevel
        waiter = EventWaiter()

        logger.info("Starting up")
        bot = ShardManager(properties.getProperty("token"), 1)
        bot.startAllShards(false)
        bot.addListener(CommandListener())
        bot.addListener(PollListener())
        bot.addListener(waiter)

        commands = CommandExecutor(prefix = "!", shardManager = bot)
        commands.alertNoClearance = false
        commands.alertUnknownCommand = false
        commands.clearanceResolver = {
            if (it.user.id in admins)
                100
            else
                0
        }
        registerCommands()

        SchemaManager.connect(properties.getProperty("db-username"),
                properties.getProperty("db-password"), properties.getProperty("db-database"),
                properties.getProperty("db-host"))
        SchemaManager.createTables()

        QueryBuilder.connectionFactory = ConnectionFactory { SchemaManager.dataSource.connection }
        PollManager.onStartup()
        logger.info("Startup complete!")
    }


    private fun registerCommands() {
        commands.register(AdminCommands())
        commands.register(PollCommands())
    }
}