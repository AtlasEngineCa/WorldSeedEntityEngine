import commands.PlayerEmoteCommand;
import commands.SpawnCommand;
import events.CombatEvent;
import events.PackEvent;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.monitoring.TickMonitor;
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.world.DimensionType;
import net.worldseed.multipart.events.EntityDismountEvent;
import net.worldseed.multipart.events.EntityControlEvent;
import net.worldseed.multipart.events.EntityInteractEvent;
import net.worldseed.multipart.ModelEngine;
import net.worldseed.resourcepack.PackBuilder;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    private static final Path BASE_PATH = Path.of("src/test/resources");
    private static final Path ZIP_PATH = BASE_PATH.resolve("resourcepack.zip");
    private static final Path MODEL_PATH = BASE_PATH.resolve("models");

    public static void main(String[] args) throws Exception {
        MinecraftServer minecraftServer = MinecraftServer.init();

        try {
            FileUtils.deleteDirectory(BASE_PATH.resolve("resourcepack").toFile());
        } catch (IllegalArgumentException ignored) { }

        FileUtils.copyDirectory(BASE_PATH.resolve("resourcepack_template").toFile(), BASE_PATH.resolve("resourcepack").toFile());
        var config = PackBuilder.Generate(BASE_PATH.resolve("bbmodel"), BASE_PATH.resolve("resourcepack"), MODEL_PATH);
        FileUtils.writeStringToFile(BASE_PATH.resolve("model_mappings.json").toFile(), config.modelMappings(), Charset.defaultCharset());

        Reader mappingsData = new InputStreamReader(new FileInputStream(BASE_PATH.resolve("model_mappings.json").toFile()));
        ModelEngine.loadMappings(mappingsData, MODEL_PATH);

        ZipUtil.pack(BASE_PATH.resolve("resourcepack").toFile(), ZIP_PATH.toFile());
        File zipFile = ZIP_PATH.toFile();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer lobby = instanceManager.createInstanceContainer(DimensionType.OVERWORLD);
        lobby.enableAutoChunkLoad(true);
        lobby.setGenerator(unit -> unit.modifier().fillHeight(0, 1, Block.STONE));
        lobby.setTimeRate(0);
        instanceManager.registerInstance(lobby);

        // Commands
        {
            CommandManager manager = MinecraftServer.getCommandManager();
            manager.setUnknownCommandCallback((sender, c) -> sender.sendMessage("Command not found."));
            manager.register(new SpawnCommand());
            manager.register(new PlayerEmoteCommand());
        }

        // Events
        {
            GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();

            CombatEvent.hook(handler);
            PackEvent.hook(handler, zipFile);

            // Login
            handler.addListener(PlayerLoginEvent.class, event -> {
                final Player player = event.getPlayer();
                player.setRespawnPoint(new Pos(0.5, 16, 0.5));
                event.setSpawningInstance(lobby);

                Audiences.all().sendMessage(Component.text(
                        player.getUsername() + " has joined",
                        NamedTextColor.GREEN
                ));

                player.sendMessage(Component.text("Run /spawn or /emote", NamedTextColor.YELLOW));
            });

            handler.addListener(PlayerPacketEvent.class, event -> {
                if (event.getPacket() instanceof ClientSteerVehiclePacket packet) {
                    Entity ridingEntity = event.getPlayer().getVehicle();

                    if (packet.flags() == 2 && ridingEntity != null) {
                        EntityDismountEvent entityRideEvent = new EntityDismountEvent(ridingEntity, event.getPlayer());
                        EventDispatcher.call(entityRideEvent);
                    }

                    if (packet.flags() == 1 && ridingEntity != null) {
                        EventDispatcher.call(new EntityControlEvent(ridingEntity, packet.forward(), packet.sideways(), true));
                    }

                    if (packet.flags() == 0 && ridingEntity != null) {
                        EventDispatcher.call(new EntityControlEvent(ridingEntity, packet.forward(), packet.sideways(), false));
                    }
                }
            });

            handler.addListener(PlayerEntityInteractEvent.class, event -> {
                if (event.getTarget().getTag(Tag.String("WSEE")) != null) {
                    EntityInteractEvent entityInteractEvent = new EntityInteractEvent(event.getTarget(), event.getPlayer());
                    EventDispatcher.call(entityInteractEvent);
                }
            });

            handler.addListener(PlayerSpawnEvent.class, event -> {
                if (!event.isFirstSpawn()) return;
                final Player player = event.getPlayer();
                player.setGameMode(GameMode.CREATIVE);
                player.setItemInMainHand(ItemStack.of(Material.DIAMOND_SWORD));
                player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f));
                player.setEnableRespawnScreen(false);
            });

            // Logout
            handler.addListener(PlayerDisconnectEvent.class, event -> Audiences.all().sendMessage(Component.text(
                    event.getPlayer().getUsername() + " has left",
                    NamedTextColor.RED
            )));

            // Chat
            handler.addListener(PlayerChatEvent.class, chatEvent -> {
                chatEvent.setChatFormat((event) -> Component.text(event.getEntity().getUsername())
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY)
                                .append(Component.text(event.getMessage(), NamedTextColor.WHITE))));
            });

            // Monitoring
            AtomicReference<TickMonitor> lastTick = new AtomicReference<>();
            handler.addListener(ServerTickMonitorEvent.class, event -> lastTick.set(event.getTickMonitor()));

            // Header/footer
            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                Collection<Player> players = MinecraftServer.getConnectionManager().getOnlinePlayers();
                if (players.isEmpty()) return;

                final Runtime runtime = Runtime.getRuntime();
                final TickMonitor tickMonitor = lastTick.get();
                final long ramUsage = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;

                final Component header = Component.newline()
                        .append(Component.text("RAM USAGE: " + ramUsage + " MB", NamedTextColor.GRAY).append(Component.newline())
                                .append(Component.text("TICK TIME: " + MathUtils.round(tickMonitor.getTickTime(), 2) + "ms", NamedTextColor.GRAY))).append(Component.newline());

                final Component footer = Component.newline()
                        .append(Component.text("          WorldSeed Entity Engine          ")
                                .color(TextColor.color(57, 200, 73))
                        .append(Component.newline()));

                Audiences.players().sendPlayerListHeaderAndFooter(header, footer);
            }, TaskSchedule.tick(10), TaskSchedule.tick(10));
        }

        OpenToLAN.open();
        MojangAuth.init();

        minecraftServer.start("0.0.0.0", 25565);
        System.out.println("Server startup done!");
    }
}
