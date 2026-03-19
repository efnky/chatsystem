package fr.insa.chatsystem.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class DeterministicUUID {

    private DeterministicUUID() {}

    public static UUID from(UUID u, LocalDate d, LocalTime t) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Encodage binaire stable (pas de string parsing, pas de locale)
            ByteBuffer buf = ByteBuffer.allocate(16 + 4 + 8); // UUID(16) + date(4) + time(8)
            buf.putLong(u.getMostSignificantBits());
            buf.putLong(u.getLeastSignificantBits());
            buf.putInt(d.toEpochDay() > Integer.MAX_VALUE ? (int)(d.toEpochDay() % Integer.MAX_VALUE) : (int)d.toEpochDay());
            buf.putLong(t.toNanoOfDay());

            byte[] hash = md.digest(buf.array());

            // Prendre 16 octets -> UUID
            ByteBuffer out = ByteBuffer.wrap(hash, 0, 16);
            long msb = out.getLong();
            long lsb = out.getLong();

            // Marquage "UUID type v5-like" (version=5, variant=IETF)
            msb = (msb & 0xffffffffffff0ffFL) | (5L << 12);
            lsb = (lsb & 0x3fffffffffffffffL) | 0x8000000000000000L;

            return new UUID(msb, lsb);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate deterministic UUID", e);
        }
    }
}

