# hashmain-message
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
