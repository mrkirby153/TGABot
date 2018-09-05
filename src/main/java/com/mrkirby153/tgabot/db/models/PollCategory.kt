package com.mrkirby153.tgabot.db.models

import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model

@Table("categories")
class PollCategory : Model() {

    var id = 0L

    var name = ""


    val options
        get() = Model.where(PollOption::class.java, "category", this.id).get()
}