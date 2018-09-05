package com.mrkirby153.tgabot.db.models

import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model

@Table("votes")
class PollVote : Model() {

    var id = 0L

    var user = ""

    var option = 0L

    var category = 0L
}