package org.joges;

import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.ScrollingGui;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.joges.annotations.Structure;
import org.joges.annotations.StructureComponent;

import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Getter
public final class GlobalObjectsStorage implements Structure {

    private final LinkedList<String> worlds;
    private final LinkedHashMap<UUID, Boolean> settings;

    @StructureComponent(ignore = true)
    private final ScrollingGui worldInventory;
    @StructureComponent(ignore = true)
    private final File storageFile = new File(Plugin.getInstance().getDataFolder().getAbsolutePath() + "/build_storage.json");

    @SneakyThrows
    public GlobalObjectsStorage() {
        this.worldInventory = Gui.scrolling(ScrollType.VERTICAL)
                .title(Component.text("Compass").color(TextColor.color(Color.CYAN.getRGB())))
                .rows(5)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        if (!this.storageFile.exists()) {
            final var _x =this.storageFile.getParentFile().mkdirs();
            final var _y = this.storageFile.createNewFile();
            this.worlds = new LinkedList<>();
            this.settings = new LinkedHashMap<>();
        } else {
            final var raw = Files.readString(this.storageFile.toPath(), StandardCharsets.UTF_8);
            final GlobalObjectsStorage storage = Plugin.getInstance()
                    .getObjectMapper()
                    .from(ArrayUtils.toObject(Base64.getDecoder().decode(raw)));
            this.worlds = storage.getWorlds();
            this.settings = storage.getSettings();
        }

    }

    public boolean contains(@NotNull UUID uniqueID) {
        return this.getSettings().containsKey(uniqueID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GlobalObjectsStorage) obj;
        return Objects.equals(this.worlds, that.worlds) &&
                Objects.equals(this.settings, that.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worlds, settings);
    }

    @Override
    public String toString() {
        return "GlobalObjects[" +
                "worlds=" + worlds + ", " +
                "settings=" + settings + ']';
    }

}
