package fr.insa.chatsystem;

import fr.insa.chatsystem.db.api.DBFacade;
import fr.insa.chatsystem.db.models.Chatroom;
import fr.insa.chatsystem.gui.api.GuiFacade;
import fr.insa.chatsystem.gui.impl.SwingGuiFacade;
import fr.insa.chatsystem.net.discovery.core.NetworkDiscoverer;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.messaging.api.Messenger;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppContext{

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean shutdownStarted = new AtomicBoolean(false);
    private final List<Runnable> shutdownListeners = new CopyOnWriteArrayList<>();
    private final Object shutdownLock = new Object();

    private final NetworkDiscoverer networker;
    private final DBFacade db;
    private final Messenger messenger;
    private Chatroom selectedChatroom;
    private final Map<String, String> myReactions;
    private final ContactList contactList;
    private GuiFacade gui;

    public AppContext(NetworkDiscoverer networker,
               Messenger messenger,
               DBFacade db,
               ContactList contactList,
               Map<String, String> myReactions){
        this.networker = networker;
        this.db = db;
        this.messenger = messenger;
        this.contactList = contactList;
        this.myReactions = myReactions;
    }

    public AppContext(NetworkDiscoverer networker,
                      Messenger messenger,
                      DBFacade db,
                      ContactList contactList,
                      Map<String, String> myReactions,
                      GuiFacade gui){
        this.networker = networker;
        this.db = db;
        this.messenger = messenger;
        this.contactList = contactList;
        this.myReactions = myReactions;
        this.gui = gui;
    }

    public void setSelectedChatroom(Chatroom selectedChatroom){
        this.selectedChatroom = selectedChatroom;
    }

    public void setGui(GuiFacade gui){
        this.gui = gui;
    }

    public GuiFacade gui(){
        return gui;
    }

    public NetworkDiscoverer networker() {
        return networker;
    }

    public DBFacade db() {
        return db;
    }

    public Messenger messenger() {
        return messenger;
    }

    public Chatroom selectedChatroom() {
        return selectedChatroom;
    }

    public Map<String, String> myReactions() {
        return myReactions;
    }

    public ContactList contactList() {
        return contactList;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean running) {
        this.running.set(running);
    }

    public void addShutdownListener(Runnable listener) {
        shutdownListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void requestShutdown() {
        System.out.println("REQUEST SHUTDOWN CALLED AppContext@" + System.identityHashCode(this));
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            dumpNonDaemonThreads("watchdog +1s");
        }, "shutdown-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();

        if (!shutdownStarted.compareAndSet(false, true)) {
            return;
        }
        System.out.println("[SHUTDOWN] step=compareAndSet done");
        System.out.println("[SHUTDOWN] step=running.set start");
        running.set(false);
        synchronized (shutdownLock) {
            shutdownLock.notifyAll();
        }
        System.out.println("[SHUTDOWN] step=running.set done");
        int listenerIndex = 0;
        for (Runnable listener : shutdownListeners) {
            System.out.println("[SHUTDOWN] step=shutdownListener-" + listenerIndex + " start");
            try {
                listener.run();
            } catch (Exception ignored) {
                // Best-effort shutdown.
            }
            System.out.println("[SHUTDOWN] step=shutdownListener-" + listenerIndex + " done");
            listenerIndex++;
        }
        System.out.println("[SHUTDOWN] step=safeClose start");
        App.safeClose(networker, messenger, db);
        System.out.println("[SHUTDOWN] step=safeClose done");
        if (gui != null) {
            System.out.println("[SHUTDOWN] step=gui.stop start");
            gui.stop();
            System.out.println("[SHUTDOWN] step=gui.stop done");
        }
        System.out.println("[SHUTDOWN] step=dumpNonDaemonThreads start");
        dumpNonDaemonThreads("after shutdown");
        System.out.println("[SHUTDOWN] step=dumpNonDaemonThreads done");
        Thread delayedDump = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            dumpNonDaemonThreads("after shutdown +500ms");
        }, "shutdown-dump-delayed");
        delayedDump.setDaemon(true);
        delayedDump.start();
    }

    private void dumpNonDaemonThreads(String reason) {
        System.out.println("===== Non-daemon thread dump (" + reason + ") =====");
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (Entry<Thread, StackTraceElement[]> entry : traces.entrySet()) {
            Thread t = entry.getKey();
            if (!t.isAlive() || t.isDaemon()) {
                continue;
            }
            System.out.println(
                    "Thread: name=" + t.getName()
                            + " daemon=" + t.isDaemon()
                            + " state=" + t.getState()
            );
            for (StackTraceElement ste : entry.getValue()) {
                System.out.println("  at " + ste);
            }
        }
        System.out.println("===== End non-daemon thread dump =====");
    }

    public void awaitShutdown() throws InterruptedException {
        synchronized (shutdownLock) {
            while (running.get()) {
                shutdownLock.wait();
            }
        }
    }
}
