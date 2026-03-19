# Java Report

## Tech stack

- **TCP**: Chat messages need ordered, reliable delivery thanks to Qos (quality of services); TCP delivers that without custom retransmission logic. The overhead of connection management is acceptable given the limited number of peers. Moreover, Tcp is easier to management several connections at the same time. 


- **UDP**: Peer discovery benefits from broadcast and low overhead on a local network. UDP removes any central server but requires the app to tolerate loss and duplicates.


- **SQLite**: An embedded database stores contacts and message history locally without requiring a database server. SQlite is easy to deploy and portable, at the cost of limited concurrency.


- **Swing**: A desktop GUI is required and Swing ships with the JDK, so there is no extra UI runtime to install. The toolkit is stable even if the look is dated. Nevertheless, We manage to improve the global look of our UI.


- **Log4j**: Networked apps need good diagnostics, and Log4j offers configurable levels and appenders. It adds dependency surface compared to `java.util.logging`, but the flexibility is worth it. Unfortunately, we dropped it during the development to focus on the features instead of learning the api. 


- **org.json**: JSON is the simplest cross-component format for network messages, and org.json keeps parsing lightweight trading type safety for speed of use. It was very powerful to store and delivered our NetworkMsg subclasses which contain many attributes used in higher level of code (Discovery, Messenger).


- **JUnit**: It keeps tests easy to run from the command line. Tests focus on realistic scenarios (network and database interactions) rather than isolated pure unit tests. It also provides several assert function to tests the Object and primitives and also throwing of Exceptions.

## Testing policy

Testing was approached at multiple levels. Core logic and critical components were validated through automated unit tests using JUnit. Logging with Log4j was extensively used to trace execution flows and diagnose issues during development and debugging. Development was primarily done on macOS, with systematic testing on virtual machines to validate network behavior, and final verification on INSA lab computers to ensure compilation and execution in the target environment.

## Highlights
- code structure -> `NetworkDiscoverer` l.72 : (`NetworkMsgFactory` & `NetworkMsgRegistry` classes) + (NetworkMsg subclasses `handle()` function : Command Design Pattern)
- facade structure -> `GuiAction` and `GuiUpdate` for plug backend with UI.
- code structure -> "Context" classes especially `DisvoryContext` (The class itself is not impressive; it is these uses that are relevant - cf.NetworkMsg subclasses `handle()` function for instance)