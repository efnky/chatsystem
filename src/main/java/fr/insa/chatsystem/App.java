package fr.insa.chatsystem;

import fr.insa.chatsystem.db.api.DBFacade;
import fr.insa.chatsystem.net.discovery.core.NetworkDiscoverer;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.messaging.api.Messenger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public final class App {

    private static final Logger LOG = LogManager.getLogger(App.class);
    private static final ContactList contactList = ContactList.getInstance();

    public static void main(String[] args) throws Exception {
        //DiscoveryApp.start(args);
        //AppMessaging.run(args);
        //GuiTestApp.run(args);

        //GuiApp app = new GuiApp(args[0]);
        //app.run(args[0]);

        /**
         * Host User creation
         */
        InetAddress localIp = findLocalIpv4()
                .orElseThrow(() -> new IllegalStateException(
                        "No suitable IPv4 address found (non-loopback)."));
        User hostUser = new User(loadOrCreateUUID(args[0]), "NotYetDefined-Not", User.Type.HOST, localIp, NetworkDiscoverer.DEFAULT_DISCOVERY_PORT);

        /**
         * App context Setting up
         */
        NetworkDiscoverer networker = new NetworkDiscoverer(hostUser, LOG);
        Messenger messenger = new Messenger(contactList);
        DBFacade db = new DBFacade(messenger, args[0]);

        AppContext appContext = new AppContext(networker, messenger, db, contactList, new HashMap<>());
        System.out.println("MAIN AppContext@" + System.identityHashCode(appContext));

        /**
         * Shutdown Hook
         */
        setShutdownHook(appContext);

        /**
         * Apps setting up
         */
        ConsoleApp consoleApp = new ConsoleApp(appContext);
        Thread consoleThread = new Thread(() -> {
            try {
                consoleApp.run();
            } catch (Exception e) {
                LOG.warn("Console thread stopped with error", e);
                appContext.requestShutdown();
            }
        }, "cli-main");
        consoleThread.setDaemon(true);
        consoleThread.start();

        Gui guiApp = new Gui(appContext);
        guiApp.run();

        appContext.awaitShutdown();
    }

    private static void setShutdownHook(AppContext appContext) {
        // disconnection
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    appContext.requestShutdown();
                })
        );
    }

    public static void safeClose(NetworkDiscoverer networker, Messenger messenger, DBFacade db){
        try {
            messenger.close();
            boolean success = networker.stop();
            int cFail = 0;
            while(!success && cFail < 10){
                success = networker.stop();
                cFail++;
            }
            if(cFail == 10)
                System.out.println("[APP] Network Discoverer not correctly close");
        } catch (Exception e) {
            LOG.warn("Error during shutdown disconnect", e);
        }
    }

    private static UUID loadOrCreateUUID(String session) {
        Path uuidPath = Paths.get("uuid"+ session +".txt");
        try {
            if (Files.exists(uuidPath)) {
                return UUID.fromString(Files.readString(uuidPath).trim());
            }

            UUID uuid = UUID.randomUUID();
            Files.writeString(uuidPath, uuid.toString());
            return uuid;

        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to load or create client UUID", e);
        }
    }

    private static Optional<InetAddress> findLocalIpv4() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces == null) return Optional.empty();

            List<InetAddress> candidates = new ArrayList<>();

            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();

                // Filter out interfaces that should not be used for peer discovery.
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

                // Gather IPv4 addresses.
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        candidates.add(addr);
                    }
                }
            }

            // Prefer private LAN addresses first (more likely to be reachable by peers).
            return candidates.stream()
                    .filter(InetAddress::isSiteLocalAddress)
                    .findFirst()
                    .or(() -> candidates.stream().findFirst());

        } catch (SocketException e) {
            LOG.error("Failed to enumerate network interfaces", e);
            return Optional.empty();
        }

    }
}