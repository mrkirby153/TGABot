package com.mrkirby153.tgabot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.mrkirby153.bfs.ConnectionFactory
import com.mrkirby153.bfs.sql.QueryBuilder
import com.mrkirby153.botcore.command.CommandExecutor
import com.mrkirby153.botcore.event.EventWaiter
import com.mrkirby153.botcore.shard.ShardManager
import com.mrkirby153.tgabot.ama.AmaManager
import com.mrkirby153.tgabot.commands.AdminCommands
import com.mrkirby153.tgabot.commands.AmaCommands
import com.mrkirby153.tgabot.commands.PollCommands
import com.mrkirby153.tgabot.db.SchemaManager
import com.mrkirby153.tgabot.listener.CommandListener
import com.mrkirby153.tgabot.listener.PollListener
import com.mrkirby153.tgabot.logger.LogPump
import com.mrkirby153.tgabot.polls.PollDisplayManager
import com.mrkirby153.tgabot.polls.PollManager
import com.mrkirby153.tgabot.polls.PollResultHandler
import com.mrkirby153.tgabot.redis.RedisConnection
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Properties
import java.util.concurrent.Executors

object Bot {

    lateinit var bot: ShardManager
    lateinit var commands: CommandExecutor
    lateinit var waiter: EventWaiter
    lateinit var adminLog: LogPump
    lateinit var redis: RedisConnection

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

    val tgaGuildId = properties.getProperty("guild")
    val tgaGuild: Guild
        get() = bot.getGuild(tgaGuildId)!!

    val executor = Executors.newCachedThreadPool()

    @JvmStatic
    fun main(args: Array<String>) {
        (logger as? Logger)?.let {
            it.level = logLevel
        }
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as? Logger)?.level = logLevel

        redis = RedisConnection(properties.getProperty("redis-host"),
                properties.getProperty("redis-port").toInt(),
                properties.getProperty("redis-password"),
                properties.getProperty("redis-db").toInt())
        waiter = EventWaiter()

        logger.info("Starting up")
        bot = ShardManager(properties.getProperty("token"), 1)
        bot.startAllShards(false)
        bot.addListener(CommandListener())
        bot.addListener(PollListener())
        bot.addListener(waiter)
        bot.addListener(PollResultHandler)
        bot.addListener(AmaManager.Listener())

        adminLog = LogPump(tgaGuild.getTextChannelById(properties.getProperty("log-channel")))

        commands = CommandExecutor(prefix = "!", shardManager = bot)
        commands.alertNoClearance = false
        commands.alertUnknownCommand = false
        commands.clearanceResolver = { member ->
            val roleIds = admins.asSequence().filter { it.startsWith("r:") }.map {
                it.substring(2)
            }.toList()
            val memberIds = admins.asSequence().filter { it.startsWith("u:") }.map {
                it.substring(2)
            }
            when {
                member.user.id in memberIds -> 100
                member.roles.asSequence().map { it.id }.filter { it in roleIds }.toList().isNotEmpty() -> 100
                else -> 0
            }
        }
        registerCommands()

        adminLog.start()

        SchemaManager.connect(properties.getProperty("db-username"),
                properties.getProperty("db-password"), properties.getProperty("db-database"),
                properties.getProperty("db-host"))
        SchemaManager.createTables()

        QueryBuilder.connectionFactory = ConnectionFactory { SchemaManager.dataSource.connection }
        QueryBuilder.logQueries = true
        PollResultHandler.verifyConfiguration()
        PollManager.onStartup()
        logger.info("Startup complete!")
        adminLog.log("Started up and ready!")
    }

    fun shutdown() {
        logger.info("Shutting down")
        PollDisplayManager.shutdown()
        this.bot.shutdownAll()
        logger.info("Shutting down logger")
        this.adminLog.shutdown(true)
        logger.info("Shutting down reaction listener")
        PollListener.reactionManager.shutdown(true)
        redis.shutdown()
        SchemaManager.shutdown()
        System.exit(0)
    }


    private fun registerCommands() {
        commands.register(AdminCommands())
        commands.register(PollCommands())
        commands.register(AmaCommands())
    }
}