# Chatsystem

This project implements a distributed chat system between multiple users on the same local network.  
The goal is to implement a Java application that allows:
- automatic discovery of contacts on the network with UDP,
- message exchange between users with TCP,
- an architecture without any central component.

The project is carried out as part of the **Advanced Programming in Java – INSA 4IR** module.

## Prerequisites

- **Java** 21
- **Maven** 3.x
- Access to a local network allowing communication between several machines.

## Compilation

From the root of the repository:

```bash
mvn compile
mvn dependency:copy-dependencies
```

## Execution
The entry point of the application is the `fr.insa.chatsystem.App` class.
The program takes one command-line parameter: a session identifier (`sessionID`) (string or integer) used to distinguish different sessions on the network.
For instance : 
session="1" will create uuid1.txt and chat1.db which stores the save of the session
session="toto" will create uuidtoto.txt and chattoto.db stores the save of the session

### Windows:
```bash
java -cp "target/classes;target/dependency/*" fr.insa.chatsystem.App <sessionID>
```
### OSX/Linux:
```bash
java -cp "target/classes:target/dependency/*" fr.insa.chatsystem.App <sessionID>
```

## Commands in the command line program 

- Help `/help`
- Quit the network `/quit`
- Change pseudo `/pseudo <newPseudo>`
- Print ContactList `/who`
- Send a text message `/send <uuid> <text...>`
- Print discovery state `/state`

## Testing

  ```bash
  mvn test
  ```

## Main features
When launching the application, the welcome screen prompts you to enter a username. Press **Enter** or click the **Connexion** button to join the network.

The main window displays the contact list on the left: connected users are marked with a green dot, disconnected users with a red dot. The search bar in the top-left corner allows you to filter users by name.

Click a username in the left panel to open a conversation, which appears on the right side of the window. The full message history with that user is displayed.

To send a message, type in the input field at the bottom and press **Enter** or click the send arrow. Sent messages appear in blue, received messages in grey, with timestamps shown below.

Right-clicking a message opens an emoji reaction menu (emoji support is limited on Linux Mint). Images can be sent using the **+** button in the bottom-left corner (not stored in database).

Additional options are available via the three-dot menu in the top-right corner:
- **Change pseudo**: update your username (must be unique among connected users)
- **Disconnect**: properly close the connection and leave the network

## Authors
  - Marguier Yohann
  - Inkaya Efe
  - Morra-Fischer Mathis

## Potential errors

```
Exception in thread "main" java.lang.NoClassDefFoundError: org/apache/logging/log4j/LogManager
at fr.insa.chatsystem.App.<clinit>(App.java:28)
Caused by: java.lang.ClassNotFoundException: org.apache.logging.log4j.LogManager
at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:580)
at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:490)
... 1 more
```

**In this case run :**
```bash
mvn clean package
mvn compile
mvn dependency:copy-dependencies
```