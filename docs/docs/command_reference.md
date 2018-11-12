# Command Reference

## Category Comamnds

Comamnds that have to do with manipulation categories.

| Command Name | Description | Example Usage |
| ------------ | ----------- | ------------- |
| `!poll categories` | Displays a list of all the categories | `!poll categories` |
| `!poll category create <channel> <name>` | Creates a category | `!poll category create #polls Test Category` |
| `!poll category remove <id>` | Removes a category and all its votes. This action is irreversable. | `!poll category remove 5` |
| `!poll category rename <id> <new name>` | Renames a category | `!poll category rename 3 new category name` |

## Option Commands

Commands that have to do with manipulating options.

| Command Name | Description | Example Usage |
| ------------ | ----------- | ------------- |
| `!poll option add <category> <emoji> <name>` | Adds an option to a poll | `!poll option add 3 👀 Eyes`
| `!poll option rename <id> <new name>` | Renames an option | `!poll option rename 4 A new name` |
| `!poll option remove <id>` | Removes an option from a poll | `!poll option remove 34` |
| `!poll options <id>` | Displays all options for the poll | `!poll options 3` |

## Miscelaneous Commands

Commands that don't fit into any of the above categories.

| Command Name | Description | Example Usage |
| ------------ | ----------- | ------------- |
| `!poll import` | Imports poll data from an attached json file | `!poll import` |
| `!poll verify` | Verifies the displaying of all categories registered | `!poll verify` |
| `!poll trigger-refresh <category>` | Tallies all the backlogged votes, clears them, then re-adds the reactions | `!poll trigger-refresh 3`
| `!poll verify-voted` | Ensures the #vote-confirmation channel is up-to-date | `!poll verify-voted` |
| `!poll gwinner` | Rolls a random winner out of everyone who has voted | `!poll gwinner`|
| `!poll tally <id>` | Tallies the poll | `!poll tally 4` |
| `!poll tally-all` | Tallies all the polls | `!poll tally-all` |
| `!poll reset <id>` | Resets a poll (Deletes all responses) | `!poll reset 4` |

## Administrative Commands

Commands for administring the bot or changing its internal settings.

| Command Name | Description | Example Usage |
| ------------ | ----------- | ------------- |
| `!shutdown` | **DANGER** Shuts down the bot. **DANGER** | `!shutdown` |
| `!spam-alert <count> <period>` | Updates the spam alert settings to `count` messages in `period` seconds | `!spam-alert 3 5`
| `!reaction-threshold [num]` | Gets or sets the threshold at which all reactions will be cleared and re-added (Defaults to 10) | `!reaction-threshold 5` |