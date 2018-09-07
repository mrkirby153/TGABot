package com.mrkirby153.tgabot.db.models

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.findEmoteById
import net.dv8tion.jda.core.entities.Message

@Table("options")
class PollOption : Model() {
    @PrimaryKey
    var id = 0L

    @Column("message_id")
    var messageId = ""

    @Column("channel_id")
    var channelId = ""

    var custom = false

    var category = -1L

    var reaction = ""

    val votes
        get() = Model.where(PollVote::class.java, "option", this.id).get()

    val message: Message?
        get() {
            val chan = Bot.jda.guilds.flatMap { it.textChannels }.firstOrNull { it.id == channelId }
                    ?: return null

            return try {
                chan.getMessageById(messageId).complete()
            } catch (e: Exception) {
                null
            }
        }
    val asMention: String
        get() {
            return if(custom){
                val emote = findEmoteById(reaction)
                emote?.asMention ?: "$\$EMOTE NOT FOUND$$"
            } else {
                reaction
            }
        }
}