package com.mrkirby153.tgabot.db

import com.mrkirby153.tgabot.Bot
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object SchemaManager {

    lateinit var dataSource: HikariDataSource

    fun connect(username: String, password: String, db: String, url: String) {
        val connUrl = "jdbc:mysql://$url/$db"
        Bot.logger.debug(
                "Connecting to $connUrl with username: $username and password: ${"*".repeat(5)}")
        val cfg = HikariConfig()
        cfg.jdbcUrl = connUrl
        cfg.username = username
        cfg.password = password

        cfg.addDataSourceProperty("cachePrepStmts", true)
        cfg.addDataSourceProperty("prepStmtCacheSize", 250)
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", 2048)
        cfg.addDataSourceProperty("useServerPrepStmts", true)
        cfg.addDataSourceProperty("useLocalSessionState", true)
        cfg.addDataSourceProperty("rewriteBatchStatements", true)
        cfg.addDataSourceProperty("cacheResultSetMetadata", true)
        cfg.addDataSourceProperty("cacheServerConfiguration", true)

        dataSource = HikariDataSource(cfg)
        Bot.logger.debug("Connected successfully!")
    }

    fun createTables() {
        Bot.logger.info("Creating tables")
        dataSource.connection.use { con ->
            con.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `categories` (id INT NOT NULL AUTO_INCREMENT, name VARCHAR(255) NOT NULL, guild VARCHAR(255) NOT NULL, channel_id VARCHAR(255) NOT NULL, message_id VARCHAR(255), created_at TIMESTAMP NULL DEFAULT NULL, updated_at TIMESTAMP NULL DEFAULT NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
            con.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `options` (id INT NOT NULL AUTO_INCREMENT, message_id VARCHAR(255), channel_id VARCHAR(255), category int NOT NULL, custom BOOLEAN DEFAULT FALSE, reaction VARCHAR(255) NOT NULL, name TEXT NOT NULL, created_at TIMESTAMP NULL DEFAULT NULL, updated_at TIMESTAMP NULL DEFAULT NULL, PRIMARY KEY (id), FOREIGN KEY (category) REFERENCES categories(id) ON DELETE CASCADE ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
            con.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `votes` (id INT AUTO_INCREMENT UNIQUE, user varchar(255) NOT NULL, `option` INT NOT NULL, category INT NOT NULL, created_at TIMESTAMP NULL DEFAULT NULL, updated_at TIMESTAMP NULL DEFAULT NULL, PRIMARY KEY (id), FOREIGN KEY (`option`) REFERENCES options(id) ON DELETE CASCADE , FOREIGN KEY (category) REFERENCES categories(id) ON DELETE CASCADE ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
        }
        Bot.logger.debug("Tables created successfully")
    }
}