package org.joges;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Objects;

@Getter
public final class Plugin extends JavaPlugin {

    @Getter
    private static Plugin instance;

    private final StructureMapper<Byte[]> objectMapper = StructureMapper.createNewByteMapper();
    private GlobalObjectsStorage globalObjectsStorage;

    private final Component prefixComponent = Component.text("Build ▹", TextColor.color(Color.AQUA.asRGB()));
    private final ItemBuilder compass = ItemBuilder
            .from(Material.COMPASS)
            .pdc(persistentDataContainer -> persistentDataContainer.set(Objects.requireNonNull(NamespacedKey.fromString("compass")), PersistentDataType.BOOLEAN, true))
            .name(prefixComponent.asComponent().append(Component.text("Worlds", TextColor.color(Color.GRAY.asRGB()))));

    @Override public void onEnable() {

        instance = this;
        this.globalObjectsStorage = new GlobalObjectsStorage();

        onConnectListener();
        onInteractListener();

    }

    @Override public void onDisable() {

        final var con = Base64.getEncoder().encodeToString( ArrayUtils.toPrimitive(this.getObjectMapper().to(this.getGlobalObjectsStorage())));
        try {
            Files.writeString(this.getGlobalObjectsStorage().getStorageFile().toPath(), con, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /* Player join Listener */
    @SuppressWarnings("deprecation")
    public void onConnectListener() {
        Bukkit.getPluginManager().registerEvent(PlayerJoinEvent.class, new Listener() {
        }, EventPriority.HIGH, (listener, rawEvent) -> {
            final var event = (PlayerJoinEvent) rawEvent;
            event.setJoinMessage(null);
            event.getPlayer().playSound(event.getPlayer(), Sound.ENTITY_PLAYER_LEVELUP, (float) 20, (float) 20);
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> this.particle(event.getPlayer(),1.5, 60), 15);
            if (!this.getGlobalObjectsStorage().contains(event.getPlayer().getUniqueId())) {
                event.getPlayer().sendMessage(this.prefixComponent.asComponent().append(Component.text("Willkommen auf diesem Bauserver!", TextColor.color(org.bukkit.Color.GRAY.asRGB()))));
                this.getGlobalObjectsStorage().getSettings().put(event.getPlayer().getUniqueId(), false);
            }
            final var item = event.getPlayer().getInventory().getItem(0);
            if (item != null)
                event.getPlayer().getInventory().addItem(item);
            event.getPlayer().getInventory().setItem(0, this.getCompass().build());
        }, this);
    }

    /* Player interact item Listener  */
    public void onInteractListener() {
        Bukkit.getPluginManager().registerEvent(PlayerInteractEvent.class, new Listener() {
        }, EventPriority.HIGH, (listener, rawEvent) -> {
            final var event = (PlayerInteractEvent) rawEvent;
            if (event.getItem() != null) {
                if (event.getItem().getItemMeta() != null) {
                    if (event.getMaterial() != Material.AIR) {
                        if (event.getItem().getItemMeta().getPersistentDataContainer().has(Objects.requireNonNull(NamespacedKey.fromString("compass")))) {
                            event.getPlayer().playSound(event.getPlayer(), Sound.BLOCK_CHEST_OPEN, 20, 20);
                            this.getGlobalObjectsStorage().getWorldInventory().open(event.getPlayer());
                        }
                    }
                }
            }
        }, this);
    }

    public void particle(Player player, double radius, int particleCount) {
        Location playerLocation = player.getLocation().add(0, 0.5, 0);
        final var dustOptions = new Particle.DustOptions(Color.YELLOW, 1.0f);
        double centerX = playerLocation.getX();
        double centerY = playerLocation.getY();
        double centerZ = playerLocation.getZ();

        double angleIncrement = Math.PI * 2 / particleCount;

        for (int i = 0; i < particleCount; i++) {
            double angle = i * angleIncrement;
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);

            Location particleLocation = new Location(playerLocation.getWorld(), centerX + xOffset, centerY, centerZ + zOffset);
            playerLocation.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 1, dustOptions);
        }
    }


}
