package com.mrkirby153.tgabot.db.models

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.findMessageById

@Table("categories")
class PollCategory : Model() {

    var id = 0L

    var name = ""

    @Column("message_id")
    var messageId: String? = null

    var guild = ""

    @Column("channel_id")
    var channel = ""

    val options
        get() = Model.where(PollOption::class.java, "category", this.id).get()

    override fun delete() {
        try {
            if (messageId != null) {
                val guild = Bot.jda.getGuildById(this.guild)
                guild.findMessageById(messageId!!)?.delete()?.queue()
            }
        } finally {
            super.delete()
        }
    }
}