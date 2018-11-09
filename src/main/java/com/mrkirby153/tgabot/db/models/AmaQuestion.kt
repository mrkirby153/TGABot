package com.mrkirby153.tgabot.db.models

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.tgabot.Bot
import net.dv8tion.jda.core.entities.User

@Table("ama_questions")
class AmaQuestion : Model() {

    var id: Long= 0

    @Column("submitter")
    var submitterId: String = ""

    var submitter: User?
        get() = Bot.jda.getUserById(this.submitterId)
        set(value) {
            submitterId = value?.id ?: ""
        }

    var question: String = ""

    @Column("message_id")
    var messageId: String = ""

    var approved: Boolean = false
    var denied: Boolean = false


}