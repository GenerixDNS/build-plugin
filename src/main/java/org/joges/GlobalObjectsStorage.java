package org.joges;

import com.google.common.collect.Lists;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.ScrollingGui;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
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

    private final LinkedList<WorldObject> worlds;
    private final LinkedHashMap<UUID, Boolean> settings;

    @StructureComponent(ignore = true)
    private final ScrollingGui worldInventory;
    @StructureComponent(ignore = true)
    private final File storageFile = new File(Plugin.getInstance().getDataFolder().getAbsolutePath() + "/build_storage.json");
    @StructureComponent(ignore = true)
    private final ArrayList<UUID> pending = new ArrayList<>();

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

        this.getWorldInventory().setItem(40, ItemBuilder.skull()
                .owner(Bukkit.getOfflinePlayer("MHF_ArrowUp"))
                .name(Component.text("add one"))
                .asGuiItem(event -> {
                    final var player = (Player) event.getWhoClicked();
                    player.closeInventory();
                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 20, 20);
                    //noinspection deprecation
                    player.sendTitle("", "§8» §7schreibe§8: §7{§6name§7} {§6normal§7/§6flat§7}", 50, 50, 50);
                    this.getPending().add(event.getWhoClicked().getUniqueId());
                })
        );
        this.getWorldInventory().setItem(Lists.newArrayList(44, 43, 42, 41, 39, 38, 37, 36), ItemBuilder.from(Material.LIGHT_GRAY_STAINED_GLASS_PANE).asGuiItem());

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
