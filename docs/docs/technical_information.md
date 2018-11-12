# Technical Notes

## Technologies Used

TGABot uses the following technologies and libraries:

* [MySQL](https://www.mysql.com)/[MariaDB](https://www.mariadb.com) for vote storing and tallying
* [Redis](https://redis.io) for caching and spam-detection
* [Bug Free Spork](https://github.com/mrkirby153/bug-free-spork) an ORM for interfacing with the MySQL database.
* [Bot Core](https://github.com/mrkirby153/bot-core) as a basic framework for the bot.

## Installation and Running

_The below instructions are provided for informational purposes only, and we're not responsible for any harm or damage to your system that may result in following these instructions_

### Requirements

* Java 8 or above
* Apache Maven
* Redis
* MySQL/MariadDB
* A Discord bot token.

### Installation

1. Clone the [source](https://github.com/mrkirby153/tgabot)
2. Build the bot: `mvn package`

### Running

#### Example Configuration

```properties
token=<BOT TOKEN GOES HERE>
db-username=root
db-password=root
db-host=localhost:3306
db-database=TGABot
log-level=INFO
guild=<TGA GUILD>
ama-channel=<AMA CHANNEL ID>
log-channel=<LOG CHANNEL ID>

# If AMA questions should get pushed to Trello once accepted
trello-enabled=false
trello-token=
trello-key=
trello-board=
trello-list=

redis-host=localhost
redis-port=6379
redis-db=1
```

1. Copy the example `config.properties` into the same directory as the jarfile built above
2. Create a file called `admins` and add role/user ids in the following format:
    * Roles: `r:<id>`
    * Users: `u:<id>`
3. Create a `votemessage` file and populate it with the text that will be shown to the user after they've voted.
4. Run the bot: `java -jar TGABot.jar`