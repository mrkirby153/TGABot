package com.mrkirby153.tgabot.ama

import com.julienvey.trello.Trello
import com.julienvey.trello.domain.Board
import com.julienvey.trello.domain.Card
import com.julienvey.trello.domain.TList
import com.julienvey.trello.impl.TrelloImpl
import com.julienvey.trello.impl.http.ApacheHttpClient
import com.mrkirby153.tgabot.Bot
import com.mrkirby153.tgabot.db.models.AmaQuestion

object TrelloUtils {

    val trelloAPI: Trello by lazy {
        TrelloImpl(Bot.properties.getProperty("trello-key"),
                Bot.properties.getProperty("trello-token"),
                ApacheHttpClient())
    }

    val board: Board by lazy {
        trelloAPI.getBoard(Bot.properties.getProperty("trello-board"))
    }

    val list: TList by lazy {
        board.fetchLists().first { it.id == Bot.properties.getProperty("trello-list") }
    }

    fun addQuestionToBoard(amaQuestion: AmaQuestion) {
        val cardTitle = amaQuestion.question.substring(
                0..Math.min(32, amaQuestion.question.length - 1))
        val body = buildString {
            appendln("**ID:** ${amaQuestion.id}")
            val submitter = amaQuestion.submitter
            if (submitter != null) {
                appendln(
                        "**Submitted By:** ${submitter.name}#${submitter.discriminator} (`${submitter.id}`)")
            } else {
                appendln("**Submitted by:** `${amaQuestion.submitterId}`")
            }
            appendln()
            appendln(amaQuestion.question)
        }

        val card = Card()
        card.desc = body
        card.name = cardTitle
        list.createCard(card)
    }
}