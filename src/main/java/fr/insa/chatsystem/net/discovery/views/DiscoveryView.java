package fr.insa.chatsystem.net.discovery.views;

import fr.insa.chatsystem.net.discovery.core.DiscoveryContext;
import fr.insa.chatsystem.net.discovery.events.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * "View" côté discovery : ne fait rien d'autre que logger les évènements émis par l'API discovery.
 *
 * Important :
 * - doit rester "rapide" (pas de blocage, pas d'I/O lourde).
 * - n'appelle pas le transport, ne modifie pas le modèle : uniquement des logs.
 */
public final class DiscoveryView implements NetworkListener {

    private final DiscoveryContext cxt;
    private final Logger log;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private DiscoveryView(DiscoveryContext cxt) {
        this.cxt = Objects.requireNonNull(cxt, "cxt");
        this.log = cxt.getLogger();
    }

    /**
     * Installe la view et l'abonne aux events discovery.
     * (Sans ça, tes callbacks ne seront jamais appelés.)
     */
    public static void initialize(DiscoveryContext cxt) {
        DiscoveryView view = new DiscoveryView(cxt);
        cxt.getEventManager().subscribe(view);
        view.log.log(Level.DEBUG, "[DISCOVERY] DiscoveryView subscribed to NetworkEventManager");
    }

    private void printlnRed(String string){
        System.out.println(ANSI_RED + string + ANSI_RESET);
    }

    @Override
    public void onConnectionRequest(ConnectionInitMsg msg) {
        log.log(Level.INFO, "[DISCOVERY] Incoming connection request: {}", summarize(msg));
        printlnRed("[DISCOVERY] Incoming connection request: "+ summarize(msg));
    }

    @Override
    public void onNewUserAccepted(ConnectionInitMsg msg) {
        printlnRed("[DISCOVERY] A user has joined the network :" +summarize(msg));
    }

    @Override
    public void onNewUserDenied(ConnectionInitMsg msg) {
        printlnRed("[DISCOVERY] A User has been denied access to the network"+ summarize(msg));
    }

    @Override
    public void onDisconnection(DisconnectionMsg msg) {
        log.log(Level.INFO, "[DISCOVERY] Peer disconnected: {}", summarize(msg));
        printlnRed("[DISCOVERY] Peer disconnected: "+ summarize(msg));
    }

    @Override
    public void onPseudoChangeRequest(PseudoRequestMsg msg) {
        log.log(Level.INFO, "[DISCOVERY] Incoming pseudo change request: {}", summarize(msg));
        printlnRed("[DISCOVERY] Incoming pseudo change request: "+ summarize(msg));
    }

    @Override
    public void onConnectionAcceptance(AcceptanceMsg msg) {
        log.log(Level.INFO, "[DISCOVERY] Connection accepted: {}", summarize(msg));
        printlnRed("[DISCOVERY] Connection accepted: "+ summarize(msg));
    }

    @Override
    public void onConnectionRejection(RejectionMsg msg) {
        log.log(Level.WARN, "[DISCOVERY] Connection rejected: {}", summarize(msg));
        printlnRed("[DISCOVERY] Connection rejected: "+ summarize(msg));
    }

    @Override
    public void onValidPseudo(ValidPseudoMsg msg) {
        log.log(Level.INFO, "[DISCOVERY] Pseudo accepted: {}", summarize(msg));
        printlnRed("[DISCOVERY] Pseudo accepted: "+ summarize(msg));
    }

    @Override
    public void onInvalidPseudo(InvalidPseudoMsg msg) {
        log.log(Level.WARN, "[DISCOVERY] Pseudo rejected: {}", summarize(msg));
        printlnRed("[DISCOVERY] Pseudo rejected: "+ summarize(msg));
    }

    /**
     * Résumé robuste (compile même si tes messages n'ont pas exactement les mêmes getters),
     * et tente d’extraire des infos utiles via réflexion.
     */
    private String summarize(Object msg) {
        if (msg == null) return "null";

        String type = msg.getClass().getSimpleName();

        // champs fréquents dans tes messages (selon ton code précédent)
        Optional<UUID> owner = invokeUUID(msg, "getOwner", "getID", "getOwnerId");
        Optional<String> pseudo = invokeString(msg, "getRequestedPseudo", "getPseudo", "getNewPseudo");
        Optional<InetAddress> srcAddr = invokeInetAddress(msg, "getSrcAddress", "getOwnerAddress", "getHostAddress", "getAddress");
        Optional<Integer> srcPort = invokeInt(msg, "getSrcPort", "getOwnerPort", "getPort");
        Optional<InetAddress> tgtAddr = invokeInetAddress(msg, "getTargetAddress");
        Optional<Integer> tgtPort = invokeInt(msg, "getTargetPort");

        StringBuilder sb = new StringBuilder();
        sb.append(type);

        owner.ifPresent(v -> sb.append(" owner=").append(v));
        pseudo.ifPresent(v -> sb.append(" pseudo=").append(v));

        if (srcAddr.isPresent() || srcPort.isPresent()) {
            sb.append(" src=").append(srcAddr.map(InetAddress::getHostAddress).orElse("?"))
                    .append(":").append(srcPort.map(String::valueOf).orElse("?"));
        }

        if (tgtAddr.isPresent() || tgtPort.isPresent()) {
            sb.append(" tgt=").append(tgtAddr.map(InetAddress::getHostAddress).orElse("?"))
                    .append(":").append(tgtPort.map(String::valueOf).orElse("?"));
        }

        // fallback: toString() si ça donne déjà des détails
        String raw = msg.toString();
        if (raw != null && !raw.startsWith(type + "@")) {
            sb.append(" raw=").append(raw);
        }

        return sb.toString();
    }

    private static Optional<String> invokeString(Object o, String... names) {
        for (String n : names) {
            try {
                Method m = o.getClass().getMethod(n);
                Object r = m.invoke(o);
                if (r instanceof String s && !s.isBlank()) return Optional.of(s);
            } catch (Exception ignored) { }
        }
        return Optional.empty();
    }

    private static Optional<Integer> invokeInt(Object o, String... names) {
        for (String n : names) {
            try {
                Method m = o.getClass().getMethod(n);
                Object r = m.invoke(o);
                if (r instanceof Integer i) return Optional.of(i);
                if (r instanceof Number num) return Optional.of(num.intValue());
            } catch (Exception ignored) { }
        }
        return Optional.empty();
    }

    private static Optional<InetAddress> invokeInetAddress(Object o, String... names) {
        for (String n : names) {
            try {
                Method m = o.getClass().getMethod(n);
                Object r = m.invoke(o);
                if (r instanceof InetAddress a) return Optional.of(a);
            } catch (Exception ignored) { }
        }
        return Optional.empty();
    }

    private static Optional<UUID> invokeUUID(Object o, String... names) {
        for (String n : names) {
            try {
                Method m = o.getClass().getMethod(n);
                Object r = m.invoke(o);
                if (r instanceof UUID u) return Optional.of(u);
                if (r instanceof String s) {
                    try { return Optional.of(UUID.fromString(s)); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) { }
        }
        return Optional.empty();
    }
}