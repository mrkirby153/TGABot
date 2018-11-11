package com.mrkirby153.tgabot.redis

class LeakyBucket(private val keyPattern: String, private val connection: RedisConnection,
                  var count: Int, var period: Int) {

    fun incr(key: String, amount: Int = 1): Boolean {
        connection.connection.use {
            val redisKey = keyPattern.format(key)
            val count = it.incrBy(redisKey, amount.toLong())
            if (count == 1L) {
                it.expire(redisKey, this.period)
            }
            return count >= this.count
        }
    }

    fun check(key: String): Boolean {
        connection.connection.use {
            return (it.get(keyPattern.format(key)) ?: "0").toInt() > count
        }
    }

    fun empty(key: String) {
        connection.connection.use {
            it.del(keyPattern.format(key))
        }
    }
}