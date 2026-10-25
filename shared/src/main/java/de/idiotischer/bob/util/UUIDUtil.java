package de.idiotischer.bob.util;

import java.util.Set;
import java.util.UUID;

public class UUIDUtil {

    public static UUID getUnused(Set<UUID> used) {
        UUID uuid;
        do {
            uuid = UUID.randomUUID();
        } while (used.contains(uuid));

        return uuid;
    }
}
