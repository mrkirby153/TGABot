# Getting Started

## TL;DR:

1. Create a category with `!poll category create <channel> <name>`
2. Add options with `!poll option add <category id> <emoji> <name>`
3. Tally results with `!poll tally <category id>`

## Creating a Category

In order to start receiving votes, a category must be created. A category can be equated to a question in the poll, such as "What is your favourite color?"

To create a category, type the command `!poll category create <channel> <name>`. The channel parameter can either be a channel mention or an ID of the channel. The bot will, assuming it has permission, send the poll message in the channel. The bot will also respond with the category's ID, which is used later on.

If at any point you forget the category's id, you can type the command `!poll categories` for a list of all the categories and their ids.

## Adding an option

A category can have up to 12 options (one option for each reaction to the message). To add an option to the category, type the command `!poll option add <category id> <emoji> <name>`. The bot will automatically react to the message with the new option.

## Tallying Results

After votes have been collected, the category can be tallied by running the command `!poll tally <category id>`. The bot will tally up the responses and display them.

**Do this in a private channel to prevent leaking results!!**

For a more comprehensive list of comamnds see [the command list](command_reference.md)