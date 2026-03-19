package fr.insa.chatsystem;

import fr.insa.chatsystem.net.discovery.core.NetworkDiscoverer;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.messaging.api.Messenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

public class ConsoleApp {

    private final AppContext appContext;

    private final NetworkDiscoverer networker;
    private final Messenger messenger;
    private final ContactList contactList;
    private BufferedReader input;

    public ConsoleApp(AppContext appContext) {
        this.appContext = appContext;
        this.networker = appContext.networker();
        this.messenger = appContext.messenger();
        this.contactList = appContext.contactList();
        this.appContext.addShutdownListener(this::closeInput);
    }

    public void run() throws Exception {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            this.input = in;

            /**
             * Messaging & networking
             */
            while (appContext.isRunning()) {
                System.out.print("> ");
                String line = null;
                try {
                    line = in.readLine();
                } catch (IOException e) {
                    if (!appContext.isRunning()) {
                        return;
                    }
                    throw e;
                }
                if (line == null) {
                    appContext.requestShutdown();
                    return;
                }

                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equals("/quit")) {
                    appContext.requestShutdown();
                    return;
                }

                if (line.equals("/help")) {
                    System.out.println("""
                            Commands:
                              /who                     list connected users (pseudo + uuid)
                              /send <uuid> <text...>    send a text message
                              /pseudo <newPseudo>       request pseudo change
                              /state                    print discovery state
                              /quit                     exit
                            """);
                    continue;
                }

                if (line.equals("/state")) {
                    System.out.println("Discovery state: " + networker.getState());
                    continue;
                }

                if (!networker.isConnected()) {
                    System.out.println("You are not connected to the network !");
                    continue;
                }

                if (line.equals("/who")) {
                    contactList.print();
                    continue;
                }

                if (line.startsWith("/pseudo ")) {
                    String newPseudo = line.substring("/pseudo ".length()).trim();
                    if (newPseudo.isEmpty()) {
                        System.out.println("Usage: /pseudo <newPseudo>");
                        continue;
                    }
                    boolean ok = networker.changePseudo(newPseudo);
                    System.out.println(ok ? "Pseudo accepted: " + newPseudo : "Pseudo rejected.");
                    continue;
                }

                if (line.startsWith("/send ")) {
                    String rest = line.substring("/send ".length()).trim();
                    int space = rest.indexOf(' ');
                    if (space <= 0) {
                        System.out.println("Usage: /send <uuid> <text...>");
                        continue;
                    }

                    String uuidStr = rest.substring(0, space).trim();
                    String text = rest.substring(space + 1).trim();
                    if (text.isEmpty()) {
                        System.out.println("Message is empty.");
                        continue;
                    }

                    UUID target;
                    try {
                        target = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid UUID: " + uuidStr);
                        continue;
                    }

                    boolean ok = messenger.sendMessage(target, text);
                    System.out.println(ok ? "Sent." : "Failed (unknown peer in ContactList or TCP error).");
                    continue;
                }

                System.out.println("Unknown command. Type /help");
            }
        } finally {
            this.input = null;
        }
    }

    private void closeInput() {
        BufferedReader in = this.input;
        try {
            System.in.close();
        } catch (IOException ignored) {
        }
        this.input = null;
    }
}
