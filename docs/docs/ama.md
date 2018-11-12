# AMA

TGABot includes AMA features. In any channel, a user can type `!ama <question>` to send their question to the mod approval queue (a channel specified in the configuration). From there, mods can either react with a green check or a red x to approve or deny the submission.

To reduce spam, each discord user can only submit 1 question per 5 minutes.

## Trello Integration

The bot supports pushing approved AMA questions to a column in Trello. If enabled, approved questions will automatically get added to the trello board for further organization.

**Note:** The bot has a rate limit of 100 requests per 10 second interval. This shouldn't be an issue, but don't approve stuff too fast, otherwise the bot will error out.

## Commands

Only one admin command exists: `!ama reset`, which resets the AMA module: Deletes all questions submitted previously and clears out the AMA channel.

This command asks for a confirmation to prevent accidental deletions of questions.

**Note:** Questions that have been pushed to Trello are not automatically deleted.