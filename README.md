# hashmain-message
## Status
| Build | Deploy on dev |
|:---:|:---:|
|![Build - moebius message](https://github.com/team-moebius/hashmain-message/workflows/Build%20-%20moebius%20message/badge.svg)|![Deploy on dev - moebius message](https://github.com/team-moebius/hashmain-message/workflows/Deploy%20on%20dev%20-%20moebius%20message/badge.svg)

## Goal
* Provide a common interface for each message send method
    * Currently, supported methods are Slack and Email
* Provide a message deduplication method as consumer needed
 
## Module structure
* message
    * Core logic for hashmain-message
    * Handle message deduplication
    * Handle message sending method such as Slack, Email
* Consumer
    * Receive message sending request from Queue such as Kakfa

## Sequence Diagram
* Please refer diagram located root directory of this repository
