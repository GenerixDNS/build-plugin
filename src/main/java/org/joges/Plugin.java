package org.joges;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Objects;

@Getter
public final class Plugin extends JavaPlugin {

    @Getter
    private static Plugin instance;

    private final StructureMapper<Byte[]> objectMapper = StructureMapper.createNewByteMapper();
    private GlobalObjectsStorage globalObjectsStorage;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
        onChatListener();

        final var removed = new LinkedList<WorldObject>();
        final int[] i = {0, 0};
        this.getGlobalObjectsStorage().getWorlds().forEach(item -> {
            if (new File(item.name()).exists()) {
                final var wc = new WorldCreator(item.name());
                Bukkit.createWorld(wc);
                this.globalObjectsStorage.getWorldInventory().setItem(i[0], this.item(item));
                i[0]++;
            } else removed.add(item);
        });
        removed.forEach(item -> this.globalObjectsStorage.getWorlds().remove(item));

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

    /* Player chat Listener  */
    @SuppressWarnings("deprecation")
    public void onChatListener() {
        Bukkit.getPluginManager().registerEvent(AsyncPlayerChatEvent.class, new Listener() {
        }, EventPriority.HIGH, (listener, rawEvent) -> {
            final var event = (AsyncPlayerChatEvent) rawEvent;
            if (this.getGlobalObjectsStorage().getPending().contains(event.getPlayer().getUniqueId())) {
                if (event.getMessage().split(" ").length == 2) {
                    if (this.getGlobalObjectsStorage().contains(event.getPlayer().getUniqueId())) {
                        final var name = event.getMessage().split(" ")[0];
                        if (!this.getGlobalObjectsStorage().getWorlds().contains(WorldObject.get(name))) {
                            final var file = new File(name);
                            final var wc = new WorldCreator(name);
                            if (file.exists()) {
                                if (file.isDirectory()) {
                                    Bukkit.getScheduler().runTask(this, () -> {
                                        final var imported = Bukkit.createWorld(wc);
                                        event.getPlayer().teleportAsync(Objects.requireNonNull(imported).getSpawnLocation());
                                        final var obj = WorldObject.create(name, event.getPlayer(), WorldObject.WorldType.IMPORTED);
                                        this.getGlobalObjectsStorage().getWorlds().add(obj);
                                        this.getGlobalObjectsStorage().getWorldInventory().addItem(this.item(obj));
                                    });
                                }
                            } else {
                                Bukkit.getScheduler().runTask(this, () -> {
                                    final var type = event.getMessage().split(" ")[1].equalsIgnoreCase("normal") ? WorldObject.WorldType.NORMAL : WorldObject.WorldType.FLAT;
                                    if (type == WorldObject.WorldType.FLAT)
                                        wc.type(WorldType.FLAT);
                                    final var imported = Bukkit.createWorld(wc);
                                    event.getPlayer().teleportAsync(Objects.requireNonNull(imported).getSpawnLocation());
                                    final var obj = WorldObject.create(name, event.getPlayer(), type);
                                    this.getGlobalObjectsStorage().getWorlds().add(obj);
                                    this.getGlobalObjectsStorage().getWorldInventory().addItem(this.item(obj));
                                });
                            }
                            this.getGlobalObjectsStorage().getPending().remove(event.getPlayer().getUniqueId());
                            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                                event.getPlayer().playSound(event.getPlayer(), Sound.ENTITY_ENDERMAN_TELEPORT, 20, 20);
                                new BukkitRunnable() {
                                    double angle = 0;
                                    @Override
                                    public void run() {
                                        if (angle >= Math.PI * 2 * 1) {
                                            cancel();
                                            return;
                                        }
                                        particle(event.getPlayer(), 1.5, 10);
                                        angle += Math.PI / 16;
                                    }
                                }.runTaskTimer(this, 0L, 1L);
                            }, 15);
                        } else {
                            event.getPlayer().playSound(event.getPlayer(), Sound.BLOCK_LAVA_POP, 20, 20);
                        }
                    }
                }
            }
        }, this);
    }

    public GuiItem item(@NotNull WorldObject worldObject) {
        return ItemBuilder.from(Material.FILLED_MAP)
                .name(Component.text("§8» §6" + worldObject.name()))
                .lore(
                        Component.text("§8§m--------------------"),
                        Component.text("§7date§8: §7" + worldObject.created().format(this.formatter)),
                        Component.text("§7creator§8: §7" + worldObject.creator()),
                        Component.text("§7need perms§8: §7" + (worldObject.permission() ?  "§ayes" : "§7false")),
                        Component.text("§7type§8: §7" + worldObject.worldType().name())
                ).asGuiItem(event -> {
                    event.getWhoClicked().teleportAsync(Objects.requireNonNull(Bukkit.getWorld(worldObject.name())).getSpawnLocation());
                    Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                        ((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 20, 20);
                        new BukkitRunnable() {
                            double angle = 0;
                            @Override
                            public void run() {
                                if (angle >= Math.PI * 2 * 1) {
                                    cancel();
                                    return;
                                }
                                particle((Player) event.getWhoClicked(), 1.5, 10);
                                angle += Math.PI / 16;
                            }
                        }.runTaskTimer(this, 0L, 1L);
                    }, 15);
                });
    }

    public void particle(@NotNull Player player, double radius, int particleCount) {
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
