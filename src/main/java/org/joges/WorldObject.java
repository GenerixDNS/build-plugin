package org.joges;

import org.bukkit.entity.Player;

import java.io.Serializable;
import java.time.LocalDate;

public record WorldObject(String name, String creator, LocalDate created, boolean permission, WorldType worldType) implements Serializable {

    public static enum WorldType {
        NORMAL,
        FLAT,
        IMPORTED,
    }

    public static WorldObject get(String name) {
        for (final var world : Plugin.getInstance().getGlobalObjectsStorage().getWorlds()) {
            if (world.name.equals(name))
                return world;
        }
        return null;
    }

    public static WorldObject create(String name, Player player, WorldType worldType) {
        return new WorldObject(name, player.getName(), LocalDate.now(), player.isOp(), worldType);
    }

}
